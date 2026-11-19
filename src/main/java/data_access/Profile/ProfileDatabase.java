package data_access.Profile;

import entity.Profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
        if (hasProfileWithEmail(profile.getEmail(), profile)) {
            throw new IllegalArgumentException("Email already exists.");
        }
        if (hasProfileWithPhoneNumber(profile.getPhoneNumber(), profile)) {
            throw new IllegalArgumentException("Phone number already exists.");
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

    public boolean existsByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        return normalizedEmail != null && hasProfileWithEmail(email, null);
    }

    public boolean existsByPhoneNumber(String phoneNumber) {
        String normalizedPhoneNumber = normalizePhoneNumber(phoneNumber);
        return normalizedPhoneNumber != null && hasProfileWithPhoneNumber(phoneNumber, null);
    }

    public void deleteProfile(String username) {
        Profile profile = profilesByUsername.remove(username);
        if (profile != null) {
            profilesByUserId.remove(profile.getUserId(), profile);
        }
    }

    private boolean hasProfileWithEmail(String email, Profile excludedProfile) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return false;
        }
        return profilesByUsername.values().stream()
                .anyMatch(profile -> profile != excludedProfile
                        && normalizedEmail.equals(normalizeEmail(profile.getEmail())));
    }

    private boolean hasProfileWithPhoneNumber(String phoneNumber, Profile excludedProfile) {
        String normalizedPhoneNumber = normalizePhoneNumber(phoneNumber);
        if (normalizedPhoneNumber == null) {
            return false;
        }
        return profilesByUsername.values().stream()
                .anyMatch(profile -> profile != excludedProfile
                        && normalizedPhoneNumber.equals(normalizePhoneNumber(profile.getPhoneNumber())));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        String digits = phoneNumber.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }
}
