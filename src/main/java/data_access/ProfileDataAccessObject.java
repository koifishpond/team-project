package data_access;

import entity.Profile;

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
    public void saveProfile(Profile profile) {
        profileDatabase.saveProfile(profile);
    }

    @Override
    public boolean existsByUsername(String username) {
        return profileDatabase.existsByUsername(username);
    }

    @Override
    public void deleteProfile(String username) {
        profileDatabase.deleteProfile(username);
    }
}
