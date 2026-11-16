package use_case.Profile;

import data_access.Profile.ProfileDataAccessInterface;
import entity.InvalidUserIDException;
import entity.Preference;
import entity.Profile;

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

        Profile profile = createProfile(
                    username,
                    profileInputData.getPassword(),
                    profileInputData.getEmail(),
                    profileInputData.getPhoneNumber()
            );
        

        profileDataAccess.saveProfile(profile);
        profileOutputBoundary.prepareSuccessView(
                new ProfileOutputData(true, "Profile created successfully.", profile.getUsername())
        );
    }

    public Profile createProfile(String username, String password, String email, String phoneNumber)
            throws InvalidUserIDException {
        return new Profile(username, password, email, phoneNumber);
    }

    public Profile getProfile(String username) {
        // TODO: retrieve the profile from storage.
        return profileDataAccess.getProfile(username);
    }

    public void saveProfile(Profile profile) {
        // TODO: persist the profile using the DAO interface.
        profileDataAccess.saveProfile(profile);
    }

    public void updatePreferences(String username, Preference preferences) {
        // TODO: load existing profile, assign preferences, and save changes.
        Profile profile = profileDataAccess.getProfile(username);
        if (profile != null) {
            // the real logic would update profile.getPreferences() here once those methods exist
        }
    }
}
