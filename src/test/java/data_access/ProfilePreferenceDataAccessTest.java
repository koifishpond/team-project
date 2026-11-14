package data_access;

import entity.Preference;
import entity.Profile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfilePreferenceDataAccessTest {

    @Test
    void profileDatabaseStoresProfilesByUsernameAndUserId() {
        ProfileDatabase profileDatabase = new ProfileDatabase();
        Profile profile = new Profile("alice", "pw", "alice@example.com", "555-1234", 42);

        profileDatabase.saveProfile(profile);

        assertSame(profile, profileDatabase.getProfile("alice"));
        assertSame(profile, profileDatabase.getProfileByUserId(42));
        assertTrue(profileDatabase.existsByUsername("alice"));
    }

    @Test
    void preferenceDatabaseStoresPreferencesByUserId() {
        PreferenceDatabase preferenceDatabase = new PreferenceDatabase();
        Preference preference = new Preference();
        preference.setUserId(99);

        preferenceDatabase.savePreference(preference);

        assertSame(preference, preferenceDatabase.getPreference(99));
        assertTrue(preferenceDatabase.existsByUserId(99));
    }
}
