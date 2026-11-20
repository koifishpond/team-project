package data_access.discovery;

import entity.matchmaking.*;
import use_case.discovery.*;
import java.time.*;
import java.util.*;
import static use_case.discovery.ProfileCommitResult.Status.*;

/** Local fixtures only. One monitor protects profiles, photo ownership, pair facts and feed revision. */
public final class InMemoryDiscoveryDataAccess implements ViewerSessionDataAccessInterface, DiscoveryDataAccessInterface,
        FeedbackDataAccessInterface {
    public enum Operation { SAVE, PUBLISH, PAUSE, RESUME }
    private final Map<ProfileId, DiscoveryProfile> profiles = new LinkedHashMap<>();
    private final Map<UUID, ResolvedViewer> sessions = new HashMap<>();
    private final Map<UUID, ProfileId> photoOwners = new HashMap<>();
    private final Map<List<Object>, PairState> pairs = new HashMap<>();
    private final Map<ProfileId, FeedbackHistory> feedback = new HashMap<>();
    private final Map<UUID, Map<ProfileId, UUID>> confirmedMatches = new HashMap<>();
    private final Clock clock;
    private long feedRevision = 1;
    private boolean available = true;

    public InMemoryDiscoveryDataAccess(Clock clock) {
        this.clock = Objects.requireNonNull(clock).withZone(ZoneId.of("America/Toronto"));
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
        pairs.put(pairKey(viewer, candidate), Objects.requireNonNull(pair));
        feedRevision = Math.incrementExact(feedRevision);
    }
    private List<Object> pairKey(ResolvedViewer viewer, ProfileId candidate) {
        return List.of(viewer.getViewerId(), candidate, viewer.getDiscoverySessionId());
    }
    public synchronized long getFeedRevision() { return feedRevision; }

    /** Trusted demo/test driver records what it actually showed as a separate step from selection. */
    public synchronized boolean captureExposure(UUID token, UUID exposureId, SelectedCandidateData displayed, Set<String> shown) {
        ResolvedViewer viewer = sessions.get(token);
        if (!available || viewer == null) return false;
        LocalDate date = LocalDate.now(clock);
        if (displayed.getFeedRevision() != feedRevision || !displayed.getEffectiveDate().equals(date)) return false;
        Optional<DiscoverySnapshot> read = load(viewer, date).getSnapshot();
        if (read.isEmpty()) return false;
        MatchingProfile viewerProfile = read.get().getViewer();
        EligibilityPolicy eligibility = new EligibilityPolicy();
        for (CandidateSnapshot candidate : read.get().getCandidates()) {
            MatchingProfile profile = candidate.getProfile();
            if (!profile.getProfileId().equals(displayed.getProfileId())) continue;
            if (profile.getProfileRevision() != displayed.getProfileRevision()) return false;
            OptionalInt distance = eligibility.distance(viewerProfile, profile);
            if (!eligibility.eligible(viewerProfile, profile, candidate.getPairState(), distance)) return false;
            FeedbackExposure exposure = new FeedbackExposure(exposureId, profile.getProfileId(),
                    DiscoveryFeatures.candidate(profile, distance), shown);
            return feedback.computeIfAbsent(viewer.getViewerId(), id -> new FeedbackHistory()).capture(viewer, exposure);
        }
        return false;
    }

    /** The trusted fixture backend supplies a match-to-like link independently of the feedback input. */
    public synchronized boolean registerConfirmedMatch(UUID matchId, ProfileId viewer, UUID likeId) {
        Objects.requireNonNull(matchId);
        Objects.requireNonNull(viewer);
        Objects.requireNonNull(likeId);
        if (!profiles.containsKey(viewer)) return false;
        UUID existing = confirmedMatches.computeIfAbsent(matchId, id -> new HashMap<>()).putIfAbsent(viewer, likeId);
        return existing == null || existing.equals(likeId);
    }

    @Override public synchronized FeedbackResult applyFeedback(UUID token, FeedbackEvent event) {
        if (!available) return FeedbackResult.RETRYABLE_FAILURE;
        ResolvedViewer viewer = sessions.get(token);
        if (viewer == null) return FeedbackResult.SESSION_EXPIRED;
        if (event.isConfirmedMatch() && !event.likeId().equals(
                confirmedMatches.getOrDefault(event.matchId(), Map.of()).get(viewer.getViewerId()))) {
            return FeedbackResult.REJECTED;
        }
        FeedbackHistory history = feedback.get(viewer.getViewerId());
        if (history == null) return FeedbackResult.REJECTED;
        FeedbackResult result = history.apply(viewer, event);
        if (result == FeedbackResult.APPLIED) feedRevision = Math.incrementExact(feedRevision);
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
                    && !pairs.containsKey(pairKey(viewer, profile.getProfileId())))
                return DiscoveryReadResult.failure(DiscoveryReadResult.Status.UNAVAILABLE);
        }
        return DiscoveryReadResult.validated(() -> snapshot(viewer, effectiveDate, storedViewer));
    }
    private DiscoverySnapshot snapshot(ResolvedViewer viewer, LocalDate effectiveDate, DiscoveryProfile storedViewer) {
        List<CandidateSnapshot> candidates = new ArrayList<>();
        for (DiscoveryProfile profile : profiles.values()) {
            if (profile.getProfileId().equals(viewer.getViewerId()) || !profile.getFields().publicationErrors(effectiveDate).isEmpty()) continue;
            PairState pair = pairs.get(pairKey(viewer, profile.getProfileId()));
            candidates.add(new CandidateSnapshot(profile.matchingOn(effectiveDate), profile.publicOn(effectiveDate), pair));
        }
        MatchingProfile matchingViewer = storedViewer.matchingOn(effectiveDate);
        FeedbackHistory history = feedback.get(viewer.getViewerId());
        ViewerPreferenceVector preferences = DiscoveryFeatures.preferences(matchingViewer.getPreferences(),
                history == null ? Map.of() : history.affinities());
        return new DiscoverySnapshot(feedRevision, 1, effectiveDate, matchingViewer, candidates, preferences);
    }
}
