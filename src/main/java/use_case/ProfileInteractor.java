package use_case;

import data_access.ProfileDataAccessInterface;
import entity.Preference;
import entity.Profile;

public class ProfileInteractor implements ProfileInputBoundary {
    private final ProfileDataAccessInterface profileDataAccess;

    public ProfileInteractor(ProfileDataAccessInterface profileDataAccess) {
        this.profileDataAccess = profileDataAccess;
    }

    @Override
    public void execute(ProfileInputData profileInputData) {
        // TODO: validate input and orchestrate the profile creation flow.
        Profile profile = createProfile(
                profileInputData.getUsername(),
                profileInputData.getPassword(),
                profileInputData.getEmail(),
                profileInputData.getPhoneNumber()
        );
        profileDataAccess.saveProfile(profile);
    }

    public Profile createProfile(String username, String password, String email, String phoneNumber) {
        // TODO: validate user input and create a Profile entity.
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
