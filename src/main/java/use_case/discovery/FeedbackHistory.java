package use_case.discovery;

import entity.matchmaking.ProfileId;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** One viewer's interaction ledger. The enclosing Data Access transaction owns synchronization. */
public final class FeedbackHistory {
    public static final int DEFAULT_WINDOW = 50;
    public static final Duration FOLLOW_UP = Duration.ofHours(24);
    private static final long LEARNING_DWELL_CAP = 60_000;
    private final int window;
    private final Map<UUID, ExposureState> exposures = new LinkedHashMap<>();
    private final Map<UUID, FeedbackEvent> accepted = new HashMap<>();
    private final Map<ProfileId, UUID> acceptedLikes = new HashMap<>();
    private final LinkedHashMap<ProfileId, ExposureState> effective = new LinkedHashMap<>();
    private final Map<ProfileId, ExposureState> latest = new HashMap<>();
    private ExposureState active;
    private long epoch;
    private long sequence;

    public FeedbackHistory() { this(DEFAULT_WINDOW); }

    public FeedbackHistory(int window) {
        if (window < 1) throw new IllegalArgumentException("Window must be positive");
        this.window = window;
    }

    public boolean capture(ResolvedViewer viewer, FeedbackExposure exposure) {
        if (viewer.getViewerId().equals(exposure.candidate()) || exposures.containsKey(exposure.id())
                || (active != null && !active.closed && active.belongsTo(viewer))) return false;
        if (active != null) active.closed = true;
        active = new ExposureState(viewer, exposure, ++sequence, epoch);
        exposures.put(exposure.id(), active);
        return true;
    }

    public Optional<FeedbackExposure> exposure(ResolvedViewer viewer, UUID id) {
        ExposureState state = exposures.get(id);
        return state != null && state.belongsTo(viewer) ? Optional.of(state.exposure) : Optional.empty();
    }

    public Optional<FeedbackExposure> activeOffer(ResolvedViewer viewer) {
        return active != null && !active.closed && active.belongsTo(viewer) ? Optional.of(active.exposure) : Optional.empty();
    }

    public void invalidateOffer(ResolvedViewer viewer, UUID id) {
        activeOffer(viewer).filter(offer -> offer.id().equals(id)).ifPresent(offer -> active.closed = true);
    }

    public boolean wasShown(UUID id) {
        ExposureState state = exposures.get(id);
        return state != null && state.shownAt != null;
    }

    public boolean authorizeDetail(ResolvedViewer viewer, UUID id, Set<String> detailKeys) {
        ExposureState state = exposures.get(id);
        if (state == null || !state.belongsTo(viewer) || state.closed || state.shownAt == null) return false;
        state.detailOffer = Set.copyOf(detailKeys);
        return true;
    }

    public Optional<FeedbackResult> replay(ResolvedViewer viewer, FeedbackEvent event) {
        ExposureState state = exposures.get(event.exposureId);
        if (state == null || !state.belongsTo(viewer)) return Optional.of(FeedbackResult.REJECTED);
        FeedbackEvent prior = accepted.get(event.id);
        return prior == null ? Optional.empty() : Optional.of(prior.sameDelivery(event) ? FeedbackResult.DUPLICATE : FeedbackResult.REJECTED);
    }

    public FeedbackResult apply(ResolvedViewer viewer, FeedbackEvent event, Instant now) {
        Optional<FeedbackResult> replay = replay(viewer, event);
        if (replay.isPresent()) return replay.get();
        ExposureState state = exposures.get(event.exposureId);
        if (state.closed || event.sequence <= state.lastSequence || (state.lastAt != null && now.isBefore(state.lastAt))) {
            return FeedbackResult.REJECTED;
        }
        if (event.type != FeedbackEvent.Type.PREVIEW_SHOWN && event.type != FeedbackEvent.Type.CLOSE && state.shownAt == null) {
            return FeedbackResult.REJECTED;
        }
        if (event.target != null && !state.evidence().contains(event.target)) return FeedbackResult.REJECTED;
        if (event.type == FeedbackEvent.Type.LIKE && acceptedLikes.containsKey(state.exposure.candidate())) {
            return FeedbackResult.REJECTED;
        }
        if (!validViewTime(state, event, now)) return FeedbackResult.REJECTED;

        boolean changedLearning = false;
        switch (event.type) {
            case PREVIEW_SHOWN:
                if (state.shownAt != null || !state.exposure.previewKeys().containsAll(event.shown)) return FeedbackResult.REJECTED;
                state.preview = event.shown;
                state.shownAt = now;
                break;
            case OPEN:
                if (state.detailOffer == null || !state.detailOffer.containsAll(event.shown)) return FeedbackResult.REJECTED;
                Set<String> newlyShown = new HashSet<>(event.shown);
                newlyShown.removeAll(state.detail);
                if (state.openedAt == null) {
                    state.openedAt = now;
                    changedLearning = true;
                }
                if (!newlyShown.isEmpty()) {
                    newlyShown.addAll(state.detail);
                    state.detail = Set.copyOf(newlyShown);
                    changedLearning = true;
                }
                state.detailOffer = null;
                break;
            case ACTIVE_VIEW:
                changedLearning = event.cumulativeMillis > state.viewMillis;
                break;
            case CLOSE:
                state.acceptedClose = true;
                state.closed = true;
                break;
            default:
                acceptReaction(state, event, now);
                changedLearning = true;
                break;
        }
        remember(state, event, now, changedLearning);
        return FeedbackResult.APPLIED;
    }

    public FeedbackResult replayAcceptedLike(BackendAcceptedLike like) {
        FeedbackEvent event = like.event();
        ExposureState state = exposures.get(event.exposureId);
        if (!matchesShownExposure(state, like.viewer(), like.candidate(), like.acceptedAt())) return FeedbackResult.REJECTED;
        FeedbackEvent prior = accepted.get(event.id);
        if (prior != null) {
            boolean sameTime = state.backendAcceptedAt == null || state.backendAcceptedAt.equals(like.acceptedAt());
            return prior.sameDelivery(event) && sameTime ? FeedbackResult.DUPLICATE : FeedbackResult.REJECTED;
        }
        // A retired offer still has its shown evidence. An accepted terminal action cannot be overwritten.
        Instant acceptedAt = like.acceptedAt();
        if (state.reaction != null || state.acceptedClose || event.sequence <= state.lastSequence
                || acceptedLikes.containsKey(like.candidate()) || acceptedAt.isBefore(state.lastAt)
                || !validViewTime(state, event, acceptedAt)) return FeedbackResult.REJECTED;
        state.backendAcceptedAt = acceptedAt;
        acceptReaction(state, event, acceptedAt);
        remember(state, event, acceptedAt, true);
        return FeedbackResult.APPLIED;
    }

    private static boolean validViewTime(ExposureState state, FeedbackEvent event, Instant at) {
        if (event.type != FeedbackEvent.Type.ACTIVE_VIEW && event.cumulativeMillis == 0) return true;
        return state.openedAt != null && !at.isBefore(state.openedAt)
                && Duration.ofMillis(event.cumulativeMillis).compareTo(Duration.between(state.openedAt, at)) <= 0;
    }

    private static void acceptReaction(ExposureState state, FeedbackEvent event, Instant at) {
        state.reaction = event;
        state.reactionAt = at;
        state.reactionEvidence = event.target == null ? state.evidence() : Set.of(event.target);
        state.closed = true;
    }

    private void remember(ExposureState state, FeedbackEvent event, Instant now, boolean changedLearning) {
        state.viewMillis = Math.max(state.viewMillis, event.cumulativeMillis);
        state.lastSequence = event.sequence;
        state.lastAt = now;
        accepted.put(event.id, event);
        if (event.type == FeedbackEvent.Type.LIKE) acceptedLikes.put(state.exposure.candidate(), event.id);
        if (changedLearning) updateEffective(state);
    }

    public FeedbackResult confirm(BackendMatchConfirmation confirmation) {
        ExposureState state = exposures.get(confirmation.exposureId());
        if (!matchesShownExposure(state, confirmation.viewer(), confirmation.candidate(), confirmation.confirmedAt())) {
            return FeedbackResult.REJECTED;
        }
        FeedbackEvent like = accepted.get(confirmation.likeId());
        if (like == null) {
            return state.reaction != null || state.acceptedClose || acceptedLikes.containsKey(confirmation.candidate())
                    ? FeedbackResult.REJECTED : FeedbackResult.PENDING;
        }
        if (like.type != FeedbackEvent.Type.LIKE || !like.exposureId.equals(confirmation.exposureId())
                || (state.backendAcceptedAt != null && confirmation.confirmedAt().isBefore(state.backendAcceptedAt))) {
            return FeedbackResult.REJECTED;
        }
        if (state.confirmation != null) return state.confirmation.sameDelivery(confirmation) ? FeedbackResult.DUPLICATE : FeedbackResult.REJECTED;
        state.confirmation = confirmation;
        // A delayed match upgrades the effective state by reference, without reactivating or refreshing its slot.
        return FeedbackResult.APPLIED;
    }

    private static boolean matchesShownExposure(ExposureState state, ProfileId viewer, ProfileId candidate, Instant at) {
        return state != null && state.owner.getViewerId().equals(viewer) && state.exposure.candidate().equals(candidate)
                && state.shownAt != null && !at.isBefore(state.shownAt);
    }

    public void resetLearning() {
        epoch++;
        latest.clear();
        effective.clear();
    }

    private void updateEffective(ExposureState state) {
        if (state.epoch != epoch) return;
        ProfileId candidate = state.exposure.candidate();
        ExposureState prior = latest.get(candidate);
        if (prior != null && prior != state && ((state.reaction == null && prior.reaction != null)
                || (state.sequence < prior.sequence && (state.reaction == null || prior.reaction != null)))) return;
        if (prior == state && !effective.containsKey(candidate)) return;
        latest.put(candidate, state);
        effective.remove(candidate);
        effective.put(candidate, state);
        if (effective.size() > window) effective.remove(effective.keySet().iterator().next());
    }

    public Map<String, Double> affinities() {
        Map<String, Double> result = new HashMap<>();
        for (ExposureState state : new TreeMap<>(effective).values()) {
            Set<String> features = state.reaction == null ? state.evidence() : state.reactionEvidence;
            if (features.isEmpty()) continue;
            double share = state.magnitude() / features.size();
            for (String feature : features) result.merge(feature, share, Double::sum);
        }
        return Map.copyOf(result);
    }

    public EngagementMetrics metrics(Instant now) {
        long impressions = 0, firstOpens = 0, activeMillis = 0, likes = 0, dislikes = 0, matches = 0;
        long mature = 0, matureMatches = 0;
        Set<UUID> sessions = new HashSet<>();
        for (ExposureState state : exposures.values()) {
            if (state.shownAt == null) continue;
            impressions++;
            sessions.add(state.owner.getDiscoverySessionId());
            if (state.openedAt != null) firstOpens++;
            activeMillis += state.viewMillis;
            if (state.reaction != null && state.reaction.type == FeedbackEvent.Type.LIKE) likes++;
            if (state.reaction != null && state.reaction.type == FeedbackEvent.Type.DISLIKE) dislikes++;
            if (state.confirmation != null) matches++;
            if (state.mature(now)) {
                mature++;
                if (state.matureOutcome().matched()) matureMatches++;
            }
        }
        return new EngagementMetrics(impressions, firstOpens, activeMillis, likes, dislikes, matches, mature, matureMatches, sessions.size());
    }

    public List<MatureImpression> explorationEvidence(Instant now) {
        List<MatureImpression> sample = new ArrayList<>();
        exposures.values().stream().filter(state -> state.epoch == epoch && state.mature(now))
                .sorted(Comparator.comparing(state -> state.shownAt))
                .forEach(state -> sample.add(state.matureOutcome()));
        return List.copyOf(sample);
    }

    private static final class ExposureState {
        private final ResolvedViewer owner;
        private final FeedbackExposure exposure;
        private final long sequence;
        private final long epoch;
        private Set<String> preview = Set.of(), detail = Set.of(), detailOffer, reactionEvidence = Set.of();
        private Instant shownAt, openedAt, lastAt, reactionAt, backendAcceptedAt;
        private long viewMillis, lastSequence;
        private FeedbackEvent reaction;
        private BackendMatchConfirmation confirmation;
        private boolean closed, acceptedClose;

        private ExposureState(ResolvedViewer owner, FeedbackExposure exposure, long sequence, long epoch) {
            this.owner = owner;
            this.exposure = exposure;
            this.sequence = sequence;
            this.epoch = epoch;
        }

        private boolean belongsTo(ResolvedViewer viewer) {
            return owner.getViewerId().equals(viewer.getViewerId()) && owner.getLoginSessionId().equals(viewer.getLoginSessionId())
                    && owner.getDiscoverySessionId().equals(viewer.getDiscoverySessionId());
        }

        private Set<String> evidence() {
            Set<String> keys = new TreeSet<>(preview);
            keys.addAll(detail);
            return Collections.unmodifiableSet(keys);
        }

        private double magnitude() {
            if (reaction == null) return (openedAt == null ? 0 : 0.05) + 0.10 * Math.min(viewMillis, LEARNING_DWELL_CAP) / LEARNING_DWELL_CAP;
            switch (reaction.type) {
                case SWIPE: return -0.20;
                case RECOMMEND_LESS: return -0.35;
                case LIKE: return confirmation == null ? 0.45 : 0.70;
                case DISLIKE: return -0.90;
                default: throw new IllegalStateException("Not a deliberate reaction");
            }
        }

        private boolean mature(Instant now) { return shownAt != null && !shownAt.plus(FOLLOW_UP).isAfter(now); }

        private MatureImpression matureOutcome() {
            Instant cutoff = shownAt.plus(FOLLOW_UP);
            return new MatureImpression(openedAt != null && !openedAt.isAfter(cutoff),
                    confirmation != null && !confirmation.confirmedAt().isAfter(cutoff),
                    reaction != null && reaction.type == FeedbackEvent.Type.DISLIKE && !reactionAt.isAfter(cutoff));
        }
    }
}
