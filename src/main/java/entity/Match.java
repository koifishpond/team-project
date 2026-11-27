package entity;

import java.time.Instant;
import java.util.Objects;

/**
 * A match between two users. A match is active until either user unmatches.
 */
public class Match {
    private final int firstUserId;
    private final int secondUserId;
    private final Instant matchedAt;
    private boolean active;

    public Match(int firstUserId, int secondUserId, Instant matchedAt) {
        if (firstUserId == secondUserId) {
            throw new IllegalArgumentException("A user cannot match with themselves.");
        }
        this.firstUserId = firstUserId;
        this.secondUserId = secondUserId;
        this.matchedAt = Objects.requireNonNull(matchedAt);
        this.active = true;
    }

    public int getFirstUserId() {
        return firstUserId;
    }

    public int getSecondUserId() {
        return secondUserId;
    }

    public Instant getMatchedAt() {
        return matchedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void end() {
        active = false;
    }

    public boolean involves(int userId) {
        if (firstUserId == userId) return true;
        else if (secondUserId == userId) return true;
        return false;
    }

    /**
     * Returns the ID of the other person in this match.
     */
    public int getOtherUserId(int userId) {
        if (userId == firstUserId) {
            return secondUserId;
        }
        if (userId == secondUserId) {
            return firstUserId;
        }
        throw new IllegalArgumentException("User" + userId + " is not part of this match.");
    }
}
