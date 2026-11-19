package data_access.Profile;

import entity.Profile;
import use_case.Profile.ProfileDataAccessInterface;

import java.util.List;

public class ProfileDataAccessObject implements ProfileDataAccessInterface {
    private final ProfileDatabase profileDatabase;

    public ProfileDataAccessObject(ProfileDatabase profileDatabase) {
        this.profileDatabase = profileDatabase;
    }

    @Override
    public Profile getProfile(String username) {
        return profileDatabase.getProfile(username);
    }

    @Override
    public Profile getProfileByUserId(int userId) {
        return profileDatabase.getProfileByUserId(userId);
    }

    @Override
    public List<Profile> getAllProfiles() {
        return profileDatabase.getAllProfiles();
    }

    @Override
    public void saveProfile(Profile profile) {
        profileDatabase.saveProfile(profile);
    }

    @Override
    public boolean existsByUsername(String username) {
        return profileDatabase.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return profileDatabase.existsByEmail(email);
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return profileDatabase.existsByPhoneNumber(phoneNumber);
    }

    @Override
    public void deleteProfile(String username) {
        profileDatabase.deleteProfile(username);
    }
}
