package data_access;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import data_access.Profile.ProfileDatabase;
import data_access.Profile.ProfileDataAccessObject;
import data_access.Profile.ProfileJsonFileStorage;
import entity.Preference;
import entity.Profile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class ProfilePreferenceDataAccessTest {

    @TempDir
    Path temporaryDirectory;

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
    void profileDatabaseRejectsDuplicateEmailAndPhoneNumber() {
        ProfileDatabase profileDatabase = new ProfileDatabase();
        Profile existingProfile = new Profile("alice", "pw", "alice@example.com", "555-1234");
        profileDatabase.saveProfile(existingProfile);

        assertThrows(IllegalArgumentException.class,
                () -> profileDatabase.saveProfile(
                        new Profile("bob", "pw", "ALICE@example.com", "555-9876")));
        assertThrows(IllegalArgumentException.class,
                () -> profileDatabase.saveProfile(
                        new Profile("carol", "pw", "carol@example.com", "(555) 1234")));

        assertEquals(1, profileDatabase.getAllProfiles().size());
        assertSame(existingProfile, profileDatabase.getProfile("alice"));
    }

    @Test
    void writesAllProfilesToJsonFile() throws IOException {
        ProfileDatabase profileDatabase = new ProfileDatabase();
        profileDatabase.saveProfile(new Profile("alice", "pw", "alice@example.com", "555-1234"));
        profileDatabase.saveProfile(new Profile("bob", "pw", "bob@example.com", "555-5678"));
        ProfileJsonFileStorage jsonStorage = new ProfileJsonFileStorage(
                new ProfileDataAccessObject(profileDatabase));
        Path destination = temporaryDirectory.resolve("backup/profiles.json");

        jsonStorage.saveAllProfilesToJson(destination);

        JsonNode profiles = new ObjectMapper().readTree(destination.toFile());
        assertTrue(profiles.isArray());
        assertEquals(2, profiles.size());
        assertTrue(containsUsername(profiles, "alice"));
        assertTrue(containsUsername(profiles, "bob"));
    }

    @Test
    void loadsProfilesFromJsonIntoFreshDatabase() throws IOException {
        ProfileDatabase savedDatabase = new ProfileDatabase();
        Profile savedAlice = new Profile("saved-alice", "pw", "alice@example.com", "555-1234");
        Profile savedBob = new Profile("saved-bob", "pw", "bob@example.com", "555-5678");
        savedDatabase.saveProfile(savedAlice);
        savedDatabase.saveProfile(savedBob);
        Path saveFile = temporaryDirectory.resolve("profiles.json");
        ProfileJsonFileStorage saveStorage = new ProfileJsonFileStorage(
                new ProfileDataAccessObject(savedDatabase));
        saveStorage.saveAllProfilesToJson(saveFile);

        ProfileDatabase loadedDatabase = new ProfileDatabase();
        ProfileJsonFileStorage loadStorage = new ProfileJsonFileStorage(
                new ProfileDataAccessObject(loadedDatabase));
        loadStorage.loadProfilesFromJson(saveFile);

        Profile loadedAlice = loadedDatabase.getProfile("saved-alice");
        assertNotNull(loadedAlice);
        assertEquals(savedAlice.getUserId(), loadedAlice.getUserId());
        assertEquals(savedAlice.getEmail(), loadedAlice.getEmail());
        assertSame(loadedAlice, loadedDatabase.getProfileByUserId(savedAlice.getUserId()));
        assertNotNull(loadedDatabase.getProfile("saved-bob"));
    }

    @Test
    void missingProfileSaveFileLeavesDatabaseEmpty() throws IOException {
        ProfileDatabase profileDatabase = new ProfileDatabase();
        ProfileJsonFileStorage jsonStorage = new ProfileJsonFileStorage(
                new ProfileDataAccessObject(profileDatabase));

        jsonStorage.loadProfilesFromJson(temporaryDirectory.resolve("missing.json"));

        assertTrue(profileDatabase.getAllProfiles().isEmpty());
    }

    private boolean containsUsername(JsonNode profiles, String username) {
        for (JsonNode profile : profiles) {
            if (username.equals(profile.get("username").asText())) {
                return true;
            }
        }
        return false;
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
