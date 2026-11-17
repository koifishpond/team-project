package data_access.Profile;

import entity.Profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    public List<Profile> getAllProfiles() {
        return new ArrayList<>(profilesByUsername.values());
    }

    public void saveProfile(Profile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("profile cannot be null");
        }

        Profile usernameMatch = profilesByUsername.get(profile.getUsername());
        if (usernameMatch != null && usernameMatch != profile) {
            throw new IllegalArgumentException("Username already exists.");
        }

        Profile userIdMatch = profilesByUserId.get(profile.getUserId());
        if (userIdMatch != null && userIdMatch != profile) {
            throw new IllegalArgumentException("User ID already exists.");
        }

        profilesByUsername.put(profile.getUsername(), profile);
        profilesByUserId.put(profile.getUserId(), profile);
    }

    public boolean existsByUsername(String username) {
        return profilesByUsername.containsKey(username);
    }

    public boolean existsByUserId(int userId) {
        return profilesByUserId.containsKey(userId);
    }

    public void deleteProfile(String username) {
        Profile profile = profilesByUsername.remove(username);
        if (profile != null) {
            profilesByUserId.remove(profile.getUserId(), profile);
        }
    }
}
