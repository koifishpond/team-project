package use_case.Profile.ResetPassword;

import entity.Profile;
import use_case.Profile.ProfileDataAccessInterface;

public class ResetPasswordInteractor implements ResetPasswordInputBoundary {
    private static final String INVALID_ACCOUNT_MESSAGE = "Could not verify username and email.";

    private final ProfileDataAccessInterface profileDataAccess;
    private final ResetPasswordOutputBoundary outputBoundary;

    public ResetPasswordInteractor(ProfileDataAccessInterface profileDataAccess,
                                   ResetPasswordOutputBoundary outputBoundary) {
        this.profileDataAccess = profileDataAccess;
        this.outputBoundary = outputBoundary;
    }

    @Override
    public void verifyAccount(ResetPasswordVerificationInputData inputData) {
        if (findVerifiedProfile(inputData.getUsername(), inputData.getEmail()) == null) {
            outputBoundary.prepareFailView(INVALID_ACCOUNT_MESSAGE);
            return;
        }
        outputBoundary.prepareNewPasswordView();
    }

    @Override
    public void resetPassword(ResetPasswordInputData inputData) {
        String newPassword = inputData.getNewPassword();
        if (newPassword == null || newPassword.trim().isEmpty()) {
            outputBoundary.prepareFailView("Password cannot be empty.");
            return;
        }

        Profile profile = findVerifiedProfile(inputData.getUsername(), inputData.getEmail());
        if (profile == null) {
            outputBoundary.prepareFailView(INVALID_ACCOUNT_MESSAGE);
            return;
        }

        profile.setNewPassword(newPassword);
        profileDataAccess.saveProfile(profile);
        outputBoundary.prepareSuccessView();
    }

    private Profile findVerifiedProfile(String username, String email) {
        if (username == null || username.trim().isEmpty() || email == null || email.trim().isEmpty()) {
            return null;
        }

        Profile profile = profileDataAccess.getProfile(username);
        if (profile == null || profile.getEmail() == null) {
            return null;
        }
        return profile.getEmail().trim().equalsIgnoreCase(email.trim()) ? profile : null;
    }
}