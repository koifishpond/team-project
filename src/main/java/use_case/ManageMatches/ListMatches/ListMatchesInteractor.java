package use_case.ManageMatches.ListMatches;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import entity.Match;

public class ListMatchesInteractor implements ListMatchesInputBoundary {
    private final ListMatchesDataAccessInterface dataAccess;
    private final ListMatchesOutputBoundary presenter;

    public ListMatchesInteractor(ListMatchesDataAccessInterface dataAccess, ListMatchesOutputBoundary presenter) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(ListMatchesInputData inputData) {
        int userId = inputData.getUserId();
        if (dataAccess.getDisplayName(userId) == null) {
            presenter.prepareFailView("User not found.");
            return;
        }

        List<MatchSummary> summaries = new ArrayList<>();
        for (Match match : dataAccess.getMatchesFor(userId)) {
            if (!match.isActive()) {
                continue;
            }
            int otherUserId = match.getOtherUserId(userId);
            if (dataAccess.hasBlocked(userId, otherUserId)
                    || dataAccess.hasBlocked(otherUserId, userId)) {
                continue;
            }
            String name = dataAccess.getDisplayName(otherUserId);
            if (name == null) {
                continue;
            }
            summaries.add(new MatchSummary(otherUserId, name, match.getMatchedAt()));
        }

        summaries.sort(Comparator.comparing(MatchSummary::getMatchedAt).reversed());
        presenter.prepareSuccessView(new ListMatchesOutputData(summaries));
    }
}
