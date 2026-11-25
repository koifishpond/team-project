package use_case.Profile.ResetPassword;

public interface ResetPasswordOutputBoundary {
    void prepareNewPasswordView();

    void prepareSuccessView();

    void prepareFailView(String errorMessage);
}