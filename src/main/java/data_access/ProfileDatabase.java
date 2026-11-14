package data_access;

import entity.Profile;

import java.util.HashMap;
import java.util.Map;

public class ProfileDatabase {
    private final Map<String, Profile> profilesByUsername = new HashMap<>();
    private final Map<Integer, Profile> profilesByUserId = new HashMap<>();

    public Profile getProfile(String username) {
        return profilesByUsername.get(username);
    }

    public Profile getProfileByUserId(int userId) {
        return profilesByUserId.get(userId);
    }

    public void saveProfile(Profile profile) {
        if (profile == null) {
            return;
        }

        profilesByUsername.put(profile.getUsername(), profile);
        if (profile.getUserId() >= 0) {
            profilesByUserId.put(profile.getUserId(), profile);
        }
    }

    public boolean existsByUsername(String username) {
        return profilesByUsername.containsKey(username);
    }

    public void deleteProfile(String username) {
        Profile profile = profilesByUsername.remove(username);
        if (profile != null && profile.getUserId() >= 0) {
            profilesByUserId.remove(profile.getUserId());
        }
    }
}
