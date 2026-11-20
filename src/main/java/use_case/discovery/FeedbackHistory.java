package use_case.discovery;

import entity.matchmaking.ProfileId;
import java.util.*;

/** One viewer's fixture ledger. Data Access serializes reads and writes under its snapshot lock. */
public final class FeedbackHistory {
    private final Map<UUID, ExposureState> exposures = new HashMap<>();
    private final Map<UUID, FeedbackEvent> accepted = new HashMap<>();
    private final Map<UUID, FeedbackEvent> matches = new HashMap<>();
    private final Map<ProfileId, ExposureState> effective = new TreeMap<>();
    private long captureSequence;

    public boolean capture(ResolvedViewer viewer, FeedbackExposure exposure) {
        if (viewer.getViewerId().equals(exposure.candidate()) || exposures.containsKey(exposure.id())) return false;
        exposures.put(exposure.id(), new ExposureState(viewer, exposure, ++captureSequence));
        return true;
    }

    public FeedbackResult apply(ResolvedViewer viewer, FeedbackEvent event) {
        ExposureState state = exposures.get(event.exposureId);
        if (state == null || !state.belongsTo(viewer)) return FeedbackResult.REJECTED;
        FeedbackEvent prior = accepted.get(event.id);
        if (prior != null) return prior.sameDelivery(event) ? FeedbackResult.DUPLICATE : FeedbackResult.REJECTED;
        if (event.target != null && !state.exposure.evidence().contains(event.target)) return FeedbackResult.REJECTED;
        if (event.type == FeedbackEvent.Type.CONFIRMED_MATCH) {
            FeedbackEvent priorMatch = matches.get(event.matchId);
            if (priorMatch != null) return priorMatch.sameDelivery(event) ? FeedbackResult.DUPLICATE : FeedbackResult.REJECTED;
            if (state.reaction == null || state.reaction.type != FeedbackEvent.Type.LIKE
                    || !state.reaction.id.equals(event.likeId) || state.confirmed) return FeedbackResult.REJECTED;
            state.confirmed = true;
            matches.put(event.matchId, event);
        } else if (event.type == FeedbackEvent.Type.OPEN) {
            state.opened = true;
        } else if (event.type == FeedbackEvent.Type.ACTIVE_VIEW) {
            state.viewSeconds = Math.max(state.viewSeconds, event.cumulativeSeconds);
        } else {
            if (state.reaction != null) return FeedbackResult.REJECTED;
            state.reaction = event;
        }
        accepted.put(event.id, event);
        ProfileId candidate = state.exposure.candidate();
        ExposureState current = effective.get(candidate);
        if (state.takesPrecedenceOver(current)) {
            effective.put(candidate, state);
        }
        return FeedbackResult.APPLIED;
    }

    public Map<String, Double> affinities() {
        Map<String, Double> result = new HashMap<>();
        for (ExposureState state : effective.values()) {
            Set<String> features = state.reaction != null && state.reaction.target != null
                    ? Set.of(state.reaction.target) : state.exposure.evidence();
            if (features.isEmpty()) continue;
            double share = state.magnitude() / features.size();
            for (String feature : features) result.merge(feature, share, Double::sum);
        }
        return Map.copyOf(result);
    }

    private static final class ExposureState {
        private final ResolvedViewer owner;
        private final FeedbackExposure exposure;
        private final long sequence;
        private boolean opened;
        private long viewSeconds;
        private FeedbackEvent reaction;
        private boolean confirmed;

        private ExposureState(ResolvedViewer owner, FeedbackExposure exposure, long sequence) {
            this.owner = owner;
            this.exposure = exposure;
            this.sequence = sequence;
        }

        private boolean belongsTo(ResolvedViewer viewer) {
            return owner.getViewerId().equals(viewer.getViewerId()) && owner.getLoginSessionId().equals(viewer.getLoginSessionId())
                    && owner.getDiscoverySessionId().equals(viewer.getDiscoverySessionId());
        }

        private boolean takesPrecedenceOver(ExposureState current) {
            if (current == null) return true;
            if (reaction != null && current.reaction == null) return true;
            if (reaction == null && current.reaction != null) return false;
            return sequence > current.sequence;
        }

        private double magnitude() {
            if (reaction == null) return (opened ? 0.05 : 0) + 0.10 * Math.min(viewSeconds, 60) / 60;
            switch (reaction.type) {
                case SWIPE: return -0.20;
                case RECOMMEND_LESS: return -0.35;
                case LIKE: return confirmed ? 0.70 : 0.45;
                case DISLIKE: return -0.90;
                default: throw new IllegalStateException("Not a deliberate reaction");
            }
        }
    }
}
