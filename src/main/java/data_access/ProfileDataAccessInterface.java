package data_access;

import entity.Profile;

public interface ProfileDataAccessInterface {
    Profile getProfile(String username);

    Profile getProfileByUserId(int userId);

    void saveProfile(Profile profile);

    boolean existsByUsername(String username);

    void deleteProfile(String username);
}
