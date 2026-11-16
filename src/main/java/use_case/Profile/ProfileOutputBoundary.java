package use_case.Profile;

public interface ProfileOutputBoundary {
    void prepareSuccessView(ProfileOutputData profileOutputData);

    void prepareFailView(String errorMessage);
}
