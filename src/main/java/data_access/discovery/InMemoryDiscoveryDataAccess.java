package data_access.discovery;

import entity.matchmaking.*;
import use_case.discovery.*;
import java.time.*;
import java.util.*;
import static use_case.discovery.ProfileCommitResult.Status.*;

/** Local fixtures only. One monitor protects profiles, photo ownership, pair facts and feed revision. */
public final class InMemoryDiscoveryDataAccess implements ViewerSessionDataAccessInterface, DiscoveryDataAccessInterface,
        FeedbackDataAccessInterface, OpenProfileDataAccessInterface, MatchConfirmationDataAccessInterface,
        AcceptedLikeDataAccessInterface {
    public enum Operation { SAVE, PUBLISH, PAUSE, RESUME }
    private final Map<ProfileId, DiscoveryProfile> profiles = new LinkedHashMap<>();
    private final Map<UUID, ResolvedViewer> sessions = new HashMap<>();
    private final Map<UUID, ProfileId> photoOwners = new HashMap<>();
    private final FixturePairStore pairs = new FixturePairStore();
    private final Map<ProfileId, FeedbackHistory> feedback = new HashMap<>();
    private final Map<List<Object>, BackendMatchConfirmation> pendingMatches = new HashMap<>();
    private final Map<List<Object>, BackendMatchConfirmation> confirmedMatches = new HashMap<>();
    private final Map<UUID, List<ProfileId>> matchPairs = new HashMap<>();
    private final Map<List<ProfileId>, UUID> pairMatches = new HashMap<>();
    private final EligibilityPolicy eligibility = new EligibilityPolicy();
    private final Clock clock;
    private final int learningWindow;
    private long feedRevision = 1;
    private boolean available = true;

    public InMemoryDiscoveryDataAccess(Clock clock) {
        this(clock, FeedbackHistory.DEFAULT_WINDOW);
    }

    public InMemoryDiscoveryDataAccess(Clock clock, int learningWindow) {
        if (learningWindow < 1) throw new IllegalArgumentException("Learning window must be positive");
        this.clock = Objects.requireNonNull(clock).withZone(ZoneId.of("America/Toronto"));
        this.learningWindow = learningWindow;
    }
    public synchronized void addSession(UUID token, ResolvedViewer viewer) {
        if (sessions.putIfAbsent(Objects.requireNonNull(token), Objects.requireNonNull(viewer)) != null)
            throw new IllegalArgumentException("Duplicate session token");
    }
    public synchronized void expireSession(UUID token) { sessions.remove(token); }
    public synchronized void setAvailable(boolean value) { available = value; }
    public synchronized void registerPhoto(UUID id, ProfileId owner) {
        if (photoOwners.putIfAbsent(Objects.requireNonNull(id), Objects.requireNonNull(owner)) != null)
            throw new IllegalArgumentException("Duplicate photo ID");
    }
    public synchronized void setPairState(ResolvedViewer viewer, ProfileId candidate, PairState pair) {
        pairs.seed(viewer, candidate, Objects.requireNonNull(pair));
        feedRevision = Math.incrementExact(feedRevision);
    }
    public synchronized long getFeedRevision() { return feedRevision; }

    public synchronized EngagementMetrics engagementMetrics(ProfileId viewer) {
        FeedbackHistory ledger = feedback.get(viewer);
        return (ledger == null ? new FeedbackHistory(learningWindow) : ledger).metrics(clock.instant());
    }

    /** Trusted fixture bridge from selection to an offer. Preparation alone is not an impression. */
    public synchronized boolean preparePreview(UUID token, UUID exposureId, SelectedCandidateData selected) {
        ResolvedViewer viewer = sessions.get(token);
        if (!available || viewer == null) return false;
        LocalDate date = LocalDate.now(clock);
        DiscoveryProfile candidate = profiles.get(selected.getProfileId());
        DiscoveryProfile owner = profiles.get(viewer.getViewerId());
        if (selected.getFeedRevision() != feedRevision || !selected.getEffectiveDate().equals(date) || owner == null
                || candidate == null || candidate.getProfileRevision() != selected.getProfileRevision()) return false;
        Optional<PairState> pair = pairs.read(viewer, candidate.getProfileId());
        MatchingProfile profile = candidate.matchingOn(date), viewerProfile = owner.matchingOn(date);
        OptionalInt distance = eligibility.distance(viewerProfile, profile);
        if (pair.isEmpty() || !eligibility.eligible(viewerProfile, profile, pair.get(), distance)) return false;
        SelectedCandidateData canonical = new SelectedCandidateData(candidate.publicOn(date), feedRevision, date, distance);
        FeedbackExposure offer = new FeedbackExposure(exposureId, profile.getProfileId(), owner.getProfileRevision(),
                profile.getProfileRevision(), date, FeedbackExposure.presentKeys(DiscoveryFeatures.candidate(profile, distance),
                canonical.getExposureKeys()));
        FeedbackHistory ledger = history(viewer.getViewerId());
        ledger.activeOffer(viewer).filter(active -> !current(viewer, active, date))
                .ifPresent(active -> ledger.invalidateOffer(viewer, active.id()));
        return ledger.capture(viewer, offer);
    }

    private FeedbackHistory history(ProfileId viewer) {
        return feedback.computeIfAbsent(viewer, id -> new FeedbackHistory(learningWindow));
    }

    @Override public synchronized OpenProfileResult authorizeDetail(UUID token, UUID exposureId) {
        if (!available) return OpenProfileResult.unavailable(OpenProfileResult.Status.RETRYABLE_FAILURE);
        ResolvedViewer viewer = sessions.get(token);
        if (viewer == null) return OpenProfileResult.unavailable(OpenProfileResult.Status.SESSION_EXPIRED);
        FeedbackHistory ledger = feedback.get(viewer.getViewerId());
        Optional<FeedbackExposure> exposure = ledger == null ? Optional.empty() : ledger.exposure(viewer, exposureId);
        LocalDate date = LocalDate.now(clock);
        if (exposure.isEmpty() || !current(viewer, exposure.get(), date)) {
            return OpenProfileResult.unavailable(OpenProfileResult.Status.PROFILE_UNAVAILABLE);
        }
        DiscoveryProfile candidate = profiles.get(exposure.get().candidate());
        MatchingProfile matching = candidate.matchingOn(date);
        OptionalInt distance = eligibility.distance(profiles.get(viewer.getViewerId()).matchingOn(date), matching);
        Set<String> keys = FeedbackExposure.presentKeys(DiscoveryFeatures.candidate(matching, distance),
                DiscoveryFeatures.detailKeys(matching, distance));
        if (!ledger.authorizeDetail(viewer, exposureId, keys)) {
            return OpenProfileResult.unavailable(OpenProfileResult.Status.PROFILE_UNAVAILABLE);
        }
        return OpenProfileResult.ready(new PublicProfileDetailData(exposureId, matching, candidate.getFields(), distance, keys));
    }

    private boolean current(ResolvedViewer viewer, FeedbackExposure exposure, LocalDate date) {
        DiscoveryProfile actor = profiles.get(viewer.getViewerId()), candidate = profiles.get(exposure.candidate());
        if (actor == null || candidate == null || !exposure.date().equals(date)
                || actor.getProfileRevision() != exposure.viewerRevision()
                || candidate.getProfileRevision() != exposure.candidateRevision()) return false;
        return pairs.read(viewer, exposure.candidate()).map(pair -> eligibility.eligible(actor.matchingOn(date),
                candidate.matchingOn(date), pair)).orElse(false);
    }

    @Override public synchronized FeedbackResult applyFeedback(UUID token, FeedbackEvent event) {
        if (!available) return FeedbackResult.RETRYABLE_FAILURE;
        ResolvedViewer viewer = sessions.get(token);
        if (viewer == null) return FeedbackResult.SESSION_EXPIRED;
        FeedbackHistory history = feedback.get(viewer.getViewerId());
        if (history == null) return FeedbackResult.REJECTED;
        Optional<FeedbackResult> replay = history.replay(viewer, event);
        if (replay.isPresent()) return replay.get();
        FeedbackExposure exposure = history.exposure(viewer, event.exposureId()).orElseThrow();
        Instant now = clock.instant();
        if (event.type() != FeedbackEvent.Type.CLOSE && !current(viewer, exposure, LocalDate.ofInstant(now, clock.getZone()))) {
            return FeedbackResult.PREVIEW_UNAVAILABLE;
        }
        if (event.type() == FeedbackEvent.Type.LIKE && !pendingLinkMatches(viewer.getViewerId(), exposure.candidate(), event)) {
            return FeedbackResult.REJECTED;
        }
        long nextRevision = Math.incrementExact(feedRevision);
        FeedbackResult result = history.apply(viewer, event, now);
        if (result == FeedbackResult.APPLIED) {
            if (history.wasShown(event.exposureId())) pairs.apply(viewer, exposure.candidate(), event.type());
            if (event.type() == FeedbackEvent.Type.LIKE) {
                reconcileLike(viewer.getViewerId(), event, history);
            }
            if (event.closesExposure() && event.type() != FeedbackEvent.Type.LIKE) {
                discardPending(viewer.getViewerId(), event.exposureId());
            }
            feedRevision = nextRevision;
        }
        return result;
    }

    @Override public synchronized FeedbackResult replayAcceptedLike(BackendAcceptedLike accepted) {
        if (!available) return FeedbackResult.RETRYABLE_FAILURE;
        ProfileId viewer = accepted.viewer();
        FeedbackHistory history = feedback.get(viewer);
        FeedbackEvent event = accepted.event();
        BackendMatchConfirmation pending = pendingMatches.get(List.of(viewer, event.id()));
        if (history == null || !profiles.containsKey(viewer) || !profiles.containsKey(accepted.candidate())
                || accepted.acceptedAt().isAfter(clock.instant()) || !pendingLinkMatches(viewer, accepted.candidate(), event)
                || (pending != null && pending.confirmedAt().isBefore(accepted.acceptedAt()))) {
            return FeedbackResult.REJECTED;
        }
        long nextRevision = Math.incrementExact(feedRevision);
        FeedbackResult result = history.replayAcceptedLike(accepted);
        if (result == FeedbackResult.APPLIED) {
            pairs.like(viewer, accepted.candidate());
            reconcileLike(viewer, event, history);
            feedRevision = nextRevision;
        }
        return result;
    }

    private boolean pendingLinkMatches(ProfileId viewer, ProfileId candidate, FeedbackEvent event) {
        for (BackendMatchConfirmation pending : pendingMatches.values()) {
            if (!pending.viewer().equals(viewer)) continue;
            boolean sameLike = pending.likeId().equals(event.id());
            boolean sameExposure = pending.exposureId().equals(event.exposureId());
            boolean sameCandidate = pending.candidate().equals(candidate);
            if ((sameLike || sameExposure || sameCandidate) && !(sameLike && sameExposure && sameCandidate)) return false;
        }
        return true;
    }

    private void reconcileLike(ProfileId viewer, FeedbackEvent event, FeedbackHistory history) {
        BackendMatchConfirmation pending = pendingMatches.get(List.of(viewer, event.id()));
        if (pending != null) applyMatch(pending, history);
        discardPending(viewer, event.exposureId());
    }

    private void discardPending(ProfileId viewer, UUID exposureId) {
        pendingMatches.values().removeIf(pending -> pending.viewer().equals(viewer) && pending.exposureId().equals(exposureId));
    }

    @Override public synchronized FeedbackResult recordMatch(BackendMatchConfirmation confirmation) {
        if (!available) return FeedbackResult.RETRYABLE_FAILURE;
        if (!profiles.containsKey(confirmation.viewer()) || !profiles.containsKey(confirmation.candidate())
                || confirmation.confirmedAt().isAfter(clock.instant())) return FeedbackResult.REJECTED;
        List<Object> receipt = List.of(confirmation.matchId(), confirmation.viewer());
        BackendMatchConfirmation prior = confirmedMatches.get(receipt);
        if (prior != null) return prior.sameDelivery(confirmation) ? FeedbackResult.DUPLICATE : FeedbackResult.REJECTED;
        List<Object> link = List.of(confirmation.viewer(), confirmation.likeId());
        BackendMatchConfirmation pending = pendingMatches.get(link);
        if (pending != null && !pending.sameDelivery(confirmation)) return FeedbackResult.REJECTED;
        List<ProfileId> pair = FixturePairStore.unordered(confirmation.viewer(), confirmation.candidate());
        for (BackendMatchConfirmation other : pendingMatches.values()) {
            if (other == pending) continue;
            boolean sameMatch = other.matchId().equals(confirmation.matchId());
            boolean samePair = FixturePairStore.unordered(other.viewer(), other.candidate()).equals(pair);
            if (sameMatch != samePair || (other.viewer().equals(confirmation.viewer())
                    && (sameMatch || other.exposureId().equals(confirmation.exposureId())))) return FeedbackResult.REJECTED;
        }
        FeedbackHistory history = feedback.get(confirmation.viewer());
        if (history == null) return FeedbackResult.REJECTED;
        long nextRevision = Math.incrementExact(feedRevision);
        FeedbackResult result = applyMatch(confirmation, history);
        if (result == FeedbackResult.APPLIED) feedRevision = nextRevision;
        if (result == FeedbackResult.PENDING) pendingMatches.put(link, confirmation);
        else pendingMatches.remove(link);
        return result;
    }

    private FeedbackResult applyMatch(BackendMatchConfirmation confirmation, FeedbackHistory history) {
        List<ProfileId> pair = FixturePairStore.unordered(confirmation.viewer(), confirmation.candidate());
        if (!matchPairs.getOrDefault(confirmation.matchId(), pair).equals(pair)
                || !pairMatches.getOrDefault(pair, confirmation.matchId()).equals(confirmation.matchId())
                || confirmedMatches.containsKey(List.of(confirmation.matchId(), confirmation.viewer()))) {
            return FeedbackResult.REJECTED;
        }
        FeedbackResult result = history.confirm(confirmation);
        if (result == FeedbackResult.APPLIED) {
            matchPairs.put(confirmation.matchId(), pair);
            pairMatches.put(pair, confirmation.matchId());
            confirmedMatches.put(List.of(confirmation.matchId(), confirmation.viewer()), confirmation);
            pairs.match(confirmation.viewer(), confirmation.candidate());
        }
        return result;
    }

    @Override public synchronized SessionReadResult resolve(UUID token) {
        Objects.requireNonNull(token);
        if (!available) return SessionReadResult.failure(SessionReadResult.Status.UNAVAILABLE);
        ResolvedViewer viewer = sessions.get(token);
        return viewer == null ? SessionReadResult.failure(SessionReadResult.Status.SESSION_EXPIRED) : SessionReadResult.success(viewer);
    }

    public synchronized Optional<DiscoveryProfile> getOwnedProfile(UUID token, ProfileId id) {
        ResolvedViewer viewer = sessions.get(token);
        return available && viewer != null && viewer.getViewerId().equals(id) ? Optional.ofNullable(profiles.get(id)) : Optional.empty();
    }
    public synchronized ProfileCommitResult createDraft(UUID token) {
        if (!available) return ProfileCommitResult.failure(RETRYABLE_FAILURE);
        ResolvedViewer viewer = sessions.get(token);
        if (viewer == null) return ProfileCommitResult.failure(SESSION_EXPIRED);
        ProfileId id = viewer.getViewerId();
        if (profiles.containsKey(id)) return ProfileCommitResult.failure(INVALID_TRANSITION);
        DiscoveryProfile profile = new DiscoveryProfile(id, 1, DiscoveryStatus.DRAFT, ProfileFields.from(new ProfileFields.Input(), LocalDate.now(clock)));
        install(profile);
        return ProfileCommitResult.success(profile);
    }

    /** Expected revision, validation and replacement occur under the same monitor as discovery reads. */
    public synchronized ProfileCommitResult commit(UUID token, ProfileId id, long expectedRevision, Operation operation, ProfileFields.Input input) {
        Objects.requireNonNull(id); Objects.requireNonNull(operation);
        if (!available) return ProfileCommitResult.failure(RETRYABLE_FAILURE);
        ResolvedViewer viewer = sessions.get(token);
        if (viewer == null) return ProfileCommitResult.failure(SESSION_EXPIRED);
        DiscoveryProfile old = profiles.get(id);
        if (!viewer.getViewerId().equals(id) || old == null) return ProfileCommitResult.failure(PROFILE_UNAVAILABLE);
        if (old.getProfileRevision() != expectedRevision) return ProfileCommitResult.failure(STALE_REVISION);
        DiscoveryStatus status = old.getDiscoveryStatus();
        if ((operation == Operation.PUBLISH && status != DiscoveryStatus.DRAFT)
                || (operation == Operation.PAUSE && status != DiscoveryStatus.ACTIVE)
                || (operation == Operation.RESUME && status != DiscoveryStatus.PAUSED)) return ProfileCommitResult.failure(INVALID_TRANSITION);
        DiscoveryStatus nextStatus = operation == Operation.PAUSE ? DiscoveryStatus.PAUSED
                : operation == Operation.PUBLISH || operation == Operation.RESUME ? DiscoveryStatus.ACTIVE : status;
        LocalDate date = LocalDate.now(clock);
        Map<String, FieldError> errors = new LinkedHashMap<>();
        ProfileFields fields = old.getFields();
        if (operation == Operation.SAVE) {
            if (input == null) errors.put("profile", FieldError.REQUIRED);
            else {
                try { fields = ProfileFields.from(input, date, nextStatus == DiscoveryStatus.ACTIVE); }
                catch (ProfileValidationException e) { errors.putAll(e.getErrors()); }
                if (input.photoIds != null) {
                    for (UUID photo : new ArrayList<>(input.photoIds)) {
                        if (photo == null || !photoOwners.containsKey(photo)) errors.put("photoIds", FieldError.INVALID_VALUE);
                        else if (!id.equals(photoOwners.get(photo))) errors.putIfAbsent("photoIds", FieldError.NOT_OWNED);
                    }
                }
            }
        } else if (nextStatus == DiscoveryStatus.ACTIVE) errors.putAll(fields.publicationErrors(date));
        if (!errors.isEmpty()) return ProfileCommitResult.invalid(errors);
        if (status == nextStatus && fields.equals(old.getFields())) return ProfileCommitResult.success(old);
        DiscoveryProfile next = new DiscoveryProfile(id, Math.incrementExact(old.getProfileRevision()), nextStatus, fields);
        install(next);
        if (!old.getFields().getPreferences().equals(fields.getPreferences()) && feedback.containsKey(id)) {
            feedback.get(id).resetLearning();
        }
        return ProfileCommitResult.success(next);
    }
    private void install(DiscoveryProfile profile) {
        long nextRevision = Math.incrementExact(feedRevision);
        profiles.put(profile.getProfileId(), profile);
        feedRevision = nextRevision;
    }

    @Override public synchronized DiscoveryReadResult load(ResolvedViewer viewer, LocalDate effectiveDate) {
        Objects.requireNonNull(viewer); Objects.requireNonNull(effectiveDate);
        if (!available) return DiscoveryReadResult.failure(DiscoveryReadResult.Status.UNAVAILABLE);
        DiscoveryProfile storedViewer = profiles.get(viewer.getViewerId());
        if (storedViewer == null || storedViewer.getDiscoveryStatus() != DiscoveryStatus.ACTIVE
                || !storedViewer.getFields().publicationErrors(effectiveDate).isEmpty())
            return DiscoveryReadResult.failure(DiscoveryReadResult.Status.VIEWER_UNAVAILABLE);
        for (DiscoveryProfile profile : profiles.values()) {
            if (!profile.getProfileId().equals(viewer.getViewerId()) && profile.getFields().publicationErrors(effectiveDate).isEmpty()
                    && pairs.read(viewer, profile.getProfileId()).isEmpty())
                return DiscoveryReadResult.failure(DiscoveryReadResult.Status.UNAVAILABLE);
        }
        return DiscoveryReadResult.validated(() -> snapshot(viewer, effectiveDate, storedViewer));
    }
    private DiscoverySnapshot snapshot(ResolvedViewer viewer, LocalDate effectiveDate, DiscoveryProfile storedViewer) {
        List<CandidateSnapshot> candidates = new ArrayList<>();
        for (DiscoveryProfile profile : profiles.values()) {
            if (profile.getProfileId().equals(viewer.getViewerId()) || !profile.getFields().publicationErrors(effectiveDate).isEmpty()) continue;
            PairState pair = pairs.read(viewer, profile.getProfileId()).orElseThrow();
            candidates.add(new CandidateSnapshot(profile.matchingOn(effectiveDate), profile.publicOn(effectiveDate), pair));
        }
        MatchingProfile matchingViewer = storedViewer.matchingOn(effectiveDate);
        FeedbackHistory history = feedback.get(viewer.getViewerId());
        ViewerPreferenceVector preferences = DiscoveryFeatures.preferences(matchingViewer.getPreferences(),
                history == null ? Map.of() : history.affinities());
        return new DiscoverySnapshot(feedRevision, 1, effectiveDate, matchingViewer, candidates, preferences,
                history == null ? List.of() : history.explorationEvidence(clock.instant()));
    }
}
