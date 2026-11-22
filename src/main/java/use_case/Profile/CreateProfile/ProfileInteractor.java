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
                    profileInputData.getPassword(),
                    profileInputData.getEmail(),
                    profileInputData.getPhoneNumber()
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
