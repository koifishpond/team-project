package interface_adapter.Profile.ResetPassword;

import use_case.Profile.ResetPassword.ResetPasswordInputBoundary;
import use_case.Profile.ResetPassword.ResetPasswordInputData;
import use_case.Profile.ResetPassword.ResetPasswordVerificationInputData;

public class ResetPasswordController {
    private final ResetPasswordInputBoundary inputBoundary;

    public ResetPasswordController(ResetPasswordInputBoundary inputBoundary) {
        this.inputBoundary = inputBoundary;
    }

    public void verifyAccount(String username, String email) {
        inputBoundary.verifyAccount(new ResetPasswordVerificationInputData(username, email));
    }

    public void resetPassword(String username, String email, String newPassword) {
        inputBoundary.resetPassword(new ResetPasswordInputData(username, email, newPassword));
    }
}