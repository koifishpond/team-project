package data_access;

import data_access.Profile.ProfileDatabase;
import entity.Preference;
import entity.Profile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfilePreferenceDataAccessTest {

    @Test
    void profileDatabaseStoresProfilesByUsernameAndUserId() {
        ProfileDatabase profileDatabase = new ProfileDatabase();
        Profile priorProfile = new Profile("prior", "pw", "prior@example.com", "555-0000");
        int reservedUserId = priorProfile.getUserId() + 3;
        Profile profile = new Profile("alice", "pw", "alice@example.com", "555-1234", reservedUserId);

        profileDatabase.saveProfile(profile);

        assertSame(profile, profileDatabase.getProfile("alice"));
        assertSame(profile, profileDatabase.getProfileByUserId(reservedUserId));
        assertTrue(profileDatabase.existsByUsername("alice"));

        Profile generatedProfile = new Profile("generated", "pw", "generated@example.com", "555-1111");
        assertTrue(generatedProfile.getUserId() > reservedUserId);
    }

    @Test
    void duplicateUsernameDoesNotCorruptProfileIndexes() {
        ProfileDatabase profileDatabase = new ProfileDatabase();
        Profile existingProfile = new Profile("alice", "old", "alice@example.com", "555-1234");
        Profile conflictingProfile = new Profile("alice", "new", "other@example.com", "555-9876");
        profileDatabase.saveProfile(existingProfile);

        assertThrows(IllegalArgumentException.class, () -> profileDatabase.saveProfile(conflictingProfile));

        assertSame(existingProfile, profileDatabase.getProfile("alice"));
        assertSame(existingProfile, profileDatabase.getProfileByUserId(existingProfile.getUserId()));
        assertNull(profileDatabase.getProfileByUserId(conflictingProfile.getUserId()));
    }

    @Test
    void preferenceDatabaseStoresPreferencesByUserId() {
        PreferenceDatabase preferenceDatabase = new PreferenceDatabase();
        Preference preference = new Preference();
        preference.setUserId(100000099);

        preferenceDatabase.savePreference(preference);

        assertSame(preference, preferenceDatabase.getPreference(100000099));
        assertTrue(preferenceDatabase.existsByUserId(100000099));
    }
}
