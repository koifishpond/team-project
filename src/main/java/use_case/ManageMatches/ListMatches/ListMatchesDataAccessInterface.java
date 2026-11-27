package use_case.ManageMatches.ListMatches;

import java.util.List;
import entity.Match;

public interface ListMatchesDataAccessInterface {
    /** Returns every match (active or ended) that involves the given user. */
    List<Match> getMatchesFor(int userId);

    /** Returns true if blockerId has blocked blockedId. */
    boolean hasBlocked(int blockerId, int blockedId);

    /** Returns the name to show for a user, or null if the user does not exist. */
    String getDisplayName(int userId);

}
