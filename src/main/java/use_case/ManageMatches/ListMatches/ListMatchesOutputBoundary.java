package use_case.ManageMatches.ListMatches;

public interface ListMatchesOutputBoundary {
    void prepareSuccessView(ListMatchesOutputData outputData);

    void prepareFailView(String errorMessage);
}
