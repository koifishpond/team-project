package use_case.Profile.CreateProfile;

public interface ProfileOutputBoundary {
    void prepareSuccessView(ProfileOutputData profileOutputData);

    void prepareFailView(String errorMessage);
}
