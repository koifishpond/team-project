package use_case.discovery;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Client interaction. Backend match confirmations use a separate authoritative channel. */
public final class FeedbackEvent {
    public enum Type { PREVIEW_SHOWN, OPEN, ACTIVE_VIEW, SWIPE, RECOMMEND_LESS, LIKE, DISLIKE, CLOSE }

    final UUID id;
    final UUID exposureId;
    final Type type;
    final long sequence;
    final long cumulativeMillis;
    final Set<String> shown;
    final String target;

    private FeedbackEvent(UUID id, UUID exposure, long sequence, Type type, long millis, Set<String> shown, String target) {
        this.id = Objects.requireNonNull(id);
        exposureId = Objects.requireNonNull(exposure);
        this.type = Objects.requireNonNull(type);
        if (sequence < 1 || millis < 0) throw new IllegalArgumentException("Invalid sequence or duration");
        this.sequence = sequence;
        cumulativeMillis = millis;
        this.shown = Set.copyOf(shown);
        this.target = target;
    }

    public static FeedbackEvent previewShown(UUID id, UUID exposure, long sequence, Set<String> shown) {
        return new FeedbackEvent(id, exposure, sequence, Type.PREVIEW_SHOWN, 0, shown, null);
    }

    public static FeedbackEvent profileOpened(UUID id, UUID exposure, long sequence, Set<String> shown) {
        return new FeedbackEvent(id, exposure, sequence, Type.OPEN, 0, shown, null);
    }

    public static FeedbackEvent reaction(UUID id, UUID exposure, long sequence, Type type) {
        return reaction(id, exposure, sequence, type, 0);
    }

    public static FeedbackEvent reaction(UUID id, UUID exposure, long sequence, Type type, long millis) {
        if (type == Type.ACTIVE_VIEW || type == Type.OPEN || type == Type.PREVIEW_SHOWN) {
            throw new IllegalArgumentException("Use the typed factory");
        }
        return new FeedbackEvent(id, exposure, sequence, type, millis, Set.of(), null);
    }

    public static FeedbackEvent activeView(UUID id, UUID exposure, long sequence, long cumulativeMillis) {
        return new FeedbackEvent(id, exposure, sequence, Type.ACTIVE_VIEW, cumulativeMillis, Set.of(), null);
    }

    public static FeedbackEvent targetedDislike(UUID id, UUID exposure, long sequence, String feature) {
        return targetedDislike(id, exposure, sequence, feature, 0);
    }

    public static FeedbackEvent targetedDislike(UUID id, UUID exposure, long sequence, String feature, long millis) {
        Objects.requireNonNull(feature);
        if (!(feature.startsWith("distance:") || feature.startsWith("smoking:") || feature.startsWith("drinking:"))
                || !DiscoveryFeatures.recognizes(feature)) throw new IllegalArgumentException("Unsupported dislike target");
        return new FeedbackEvent(id, exposure, sequence, Type.DISLIKE, millis, Set.of(), feature);
    }

    public UUID id() { return id; }
    public UUID exposureId() { return exposureId; }
    public Type type() { return type; }
    public boolean closesExposure() {
        return type == Type.SWIPE || type == Type.RECOMMEND_LESS || type == Type.LIKE || type == Type.DISLIKE || type == Type.CLOSE;
    }

    boolean sameDelivery(FeedbackEvent other) {
        return exposureId.equals(other.exposureId) && type == other.type && sequence == other.sequence
                && cumulativeMillis == other.cumulativeMillis && shown.equals(other.shown) && Objects.equals(target, other.target);
    }
}
