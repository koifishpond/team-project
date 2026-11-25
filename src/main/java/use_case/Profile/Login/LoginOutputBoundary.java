package use_case.Profile.Login;

public interface LoginOutputBoundary {
    void prepareSuccessView(LoginOutputData outputData);

    void prepareFailView(String errorMessage);
}