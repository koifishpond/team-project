package use_case.Profile.ResetPassword;

public interface ResetPasswordInputBoundary {
    void verifyAccount(ResetPasswordVerificationInputData inputData);

    void resetPassword(ResetPasswordInputData inputData);
}