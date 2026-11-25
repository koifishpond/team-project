package use_case.Profile.CreateProfile;

import entity.InvalidUserIDException;
import entity.Profile;
import use_case.Profile.ProfileDataAccessInterface;

public class ProfileInteractor implements ProfileInputBoundary {
    private final ProfileDataAccessInterface profileDataAccess;
    private final ProfileOutputBoundary profileOutputBoundary;

    public ProfileInteractor(ProfileDataAccessInterface profileDataAccess,
                             ProfileOutputBoundary profileOutputBoundary) {
        this.profileDataAccess = profileDataAccess;
        this.profileOutputBoundary = profileOutputBoundary;
    }

    @Override
    public void execute(ProfileInputData profileInputData) {
        String username = profileInputData.getUsername();
        if (username == null || username.trim().isEmpty()) {
            profileOutputBoundary.prepareFailView("Username cannot be empty.");
            return;
        }
        String password = profileInputData.getPassword();
        if (password == null || password.trim().isEmpty()) {
            profileOutputBoundary.prepareFailView("Password cannot be empty.");
            return;
        }
        String email = profileInputData.getEmail();
        if (email == null || !email.contains("@")) {
            profileOutputBoundary.prepareFailView("Email must contain an @ symbol.");
            return;
        }
        String phoneNumber = profileInputData.getPhoneNumber();
        if (phoneNumber == null || !phoneNumber.matches("[0-9]{10}")) {
            profileOutputBoundary.prepareFailView("Phone number must contain exactly 10 digits.");
            return;
        }
        if (profileDataAccess.existsByUsername(username)) {
            profileOutputBoundary.prepareFailView("Username already exists.");
            return;
        }
        if (profileDataAccess.existsByEmail(profileInputData.getEmail())) {
            profileOutputBoundary.prepareFailView("Email already exists.");
            return;
        }
        if (profileDataAccess.existsByPhoneNumber(profileInputData.getPhoneNumber())) {
            profileOutputBoundary.prepareFailView("Phone number already exists.");
            return;
        }

        try {
                Profile profile = new Profile(
                    username,
                    password,
                    email,
                    phoneNumber
            );

            profileDataAccess.saveProfile(profile);
            profileOutputBoundary.prepareSuccessView(
                new ProfileOutputData(true, "Profile created successfully.", profile.getUsername())
            );
        } catch (InvalidUserIDException | IllegalArgumentException exception) {
            profileOutputBoundary.prepareFailView(exception.getMessage());
        }
    }

}
