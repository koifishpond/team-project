package use_case.discovery;

import entity.matchmaking.ProfileId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Participant-specific link from trusted backend Data Access. Independent of login and feed sessions. */
public final class BackendMatchConfirmation {
    private final UUID matchId;
    private final ProfileId viewer;
    private final ProfileId candidate;
    private final UUID exposureId;
    private final UUID likeId;
    private final Instant confirmedAt;

    public BackendMatchConfirmation(UUID matchId, ProfileId viewer, ProfileId candidate, UUID exposureId, UUID likeId, Instant confirmedAt) {
        this.matchId = Objects.requireNonNull(matchId);
        this.viewer = Objects.requireNonNull(viewer);
        this.candidate = Objects.requireNonNull(candidate);
        if (viewer.equals(candidate)) throw new IllegalArgumentException("Self match");
        this.exposureId = Objects.requireNonNull(exposureId);
        this.likeId = Objects.requireNonNull(likeId);
        this.confirmedAt = Objects.requireNonNull(confirmedAt);
    }

    public UUID matchId() { return matchId; }
    public ProfileId viewer() { return viewer; }
    public ProfileId candidate() { return candidate; }
    public UUID exposureId() { return exposureId; }
    public UUID likeId() { return likeId; }
    public Instant confirmedAt() { return confirmedAt; }

    public boolean sameDelivery(BackendMatchConfirmation other) {
        return matchId.equals(other.matchId) && viewer.equals(other.viewer) && candidate.equals(other.candidate)
                && exposureId.equals(other.exposureId) && likeId.equals(other.likeId) && confirmedAt.equals(other.confirmedAt);
    }
}
