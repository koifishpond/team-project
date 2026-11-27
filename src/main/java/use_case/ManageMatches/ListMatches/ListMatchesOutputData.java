package use_case.ManageMatches.ListMatches;

import java.util.Collections;
import java.util.List;

public class ListMatchesOutputData {
    private final List<MatchSummary> matches;

    public ListMatchesOutputData(List<MatchSummary> matches) {
        this.matches = Collections.unmodifiableList(matches);
    }
    /** Newest match first. Empty if the user has no active matches. */
    public List<MatchSummary> getMatches() {
        return matches;
    }

    public boolean isEmpty() {
        return matches.isEmpty();
    }
}
