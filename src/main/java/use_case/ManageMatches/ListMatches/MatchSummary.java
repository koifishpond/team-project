package use_case.ManageMatches.ListMatches;

import java.time.Instant;

public class MatchSummary {
    private final int otherUserId;
    private final String displayName;
    private final Instant matchedAt;

    public MatchSummary(int otherUserId, String displayName, Instant matchedAt) {
        this.otherUserId = otherUserId;
        this.displayName = displayName;
        this.matchedAt = matchedAt;
    }

    public int getOtherUserId() {
        return otherUserId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Instant getMatchedAt() {
        return matchedAt;
    }
}
