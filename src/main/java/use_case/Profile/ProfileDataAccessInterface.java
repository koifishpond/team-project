package use_case.Profile;

import entity.Profile;

import java.util.List;

public interface ProfileDataAccessInterface {
    Profile getProfile(String username);

    Profile getProfileByUserId(int userId);

    List<Profile> getAllProfiles();

    void saveProfile(Profile profile);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    void deleteProfile(String username);
}
