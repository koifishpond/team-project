package use_case.discovery;

import entity.matchmaking.ProfileId;

import java.time.Instant;
import java.util.Objects;

/** An already-accepted like from the trusted backend, with the original interaction payload. */
public final class BackendAcceptedLike {
    private final ProfileId viewer;
    private final ProfileId candidate;
    private final FeedbackEvent event;
    private final Instant acceptedAt;

    public BackendAcceptedLike(ProfileId viewer, ProfileId candidate, FeedbackEvent event, Instant acceptedAt) {
        this.viewer = Objects.requireNonNull(viewer);
        this.candidate = Objects.requireNonNull(candidate);
        this.event = Objects.requireNonNull(event);
        this.acceptedAt = Objects.requireNonNull(acceptedAt);
        if (viewer.equals(candidate) || event.type() != FeedbackEvent.Type.LIKE) {
            throw new IllegalArgumentException("Invalid accepted like");
        }
    }

    public ProfileId viewer() { return viewer; }
    public ProfileId candidate() { return candidate; }
    public FeedbackEvent event() { return event; }
    public Instant acceptedAt() { return acceptedAt; }
}
