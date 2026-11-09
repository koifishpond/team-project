package use_case;

public interface ProfileOutputBoundary {
    void prepareSuccessView(ProfileOutputData profileOutputData);

    void prepareFailView(String errorMessage);
}
