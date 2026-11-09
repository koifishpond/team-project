package data_access;

import entity.Profile;

import java.util.HashMap;
import java.util.Map;

public class Database {
    private final Map<String, Profile> profiles = new HashMap<>();

    public Profile getProfile(String username) {
        // TODO: pull the matching profile from the database.
        return profiles.get(username);
    }

    public void saveProfile(Profile profile) {
        // TODO: persist the profile into the database.
        String key = profile == null ? "unknown-profile" : profile.toString();
        profiles.put(key, profile);
    }

    public boolean existsByUsername(String username) {
        // TODO: check whether this username already exists.
        return profiles.containsKey(username);
    }

    public void deleteProfile(String username) {
        // TODO: remove the profile for the given username.
        profiles.remove(username);
    }
}
