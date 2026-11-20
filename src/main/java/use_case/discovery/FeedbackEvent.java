package use_case.discovery;

import java.util.Objects;
import java.util.UUID;

/** Declared evidence for a trusted fixture exposure. Match identity is supplied by the fixture backend. */
public final class FeedbackEvent {
    public enum Type { OPEN, ACTIVE_VIEW, SWIPE, RECOMMEND_LESS, LIKE, DISLIKE, CONFIRMED_MATCH }

    final UUID id;
    final UUID exposureId;
    final Type type;
    final long cumulativeSeconds;
    final String target;
    final UUID likeId;
    final UUID matchId;

    private FeedbackEvent(UUID id, UUID exposure, Type type, long seconds, String target, UUID like, UUID match) {
        this.id = Objects.requireNonNull(id);
        exposureId = Objects.requireNonNull(exposure);
        this.type = Objects.requireNonNull(type);
        cumulativeSeconds = seconds;
        this.target = target;
        likeId = like;
        matchId = match;
    }

    public static FeedbackEvent reaction(UUID id, UUID exposure, Type type) {
        if (type == Type.ACTIVE_VIEW || type == Type.CONFIRMED_MATCH) throw new IllegalArgumentException("Use the typed factory");
        return new FeedbackEvent(id, exposure, type, 0, null, null, null);
    }

    public static FeedbackEvent activeView(UUID id, UUID exposure, long cumulativeSeconds) {
        if (cumulativeSeconds < 0) throw new IllegalArgumentException("Negative viewing time");
        return new FeedbackEvent(id, exposure, Type.ACTIVE_VIEW, cumulativeSeconds, null, null, null);
    }

    public static FeedbackEvent targetedDislike(UUID id, UUID exposure, String feature) {
        Objects.requireNonNull(feature);
        if (!(feature.startsWith("distance:") || feature.startsWith("smoking:") || feature.startsWith("drinking:"))
                || !DiscoveryFeatures.recognizes(feature)) throw new IllegalArgumentException("Unsupported dislike target");
        return new FeedbackEvent(id, exposure, Type.DISLIKE, 0, feature, null, null);
    }

    public static FeedbackEvent confirmedMatch(UUID id, UUID exposure, UUID likeId, UUID matchId) {
        return new FeedbackEvent(id, exposure, Type.CONFIRMED_MATCH, 0, null,
                Objects.requireNonNull(likeId), Objects.requireNonNull(matchId));
    }

    public boolean isConfirmedMatch() { return type == Type.CONFIRMED_MATCH; }
    public UUID matchId() { return matchId; }
    public UUID likeId() { return likeId; }

    boolean sameDelivery(FeedbackEvent other) {
        return exposureId.equals(other.exposureId) && type == other.type && cumulativeSeconds == other.cumulativeSeconds
                && Objects.equals(target, other.target) && Objects.equals(likeId, other.likeId) && Objects.equals(matchId, other.matchId);
    }
}
