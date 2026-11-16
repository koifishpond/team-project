package use_case.Profile;

import data_access.Profile.ProfileDataAccessObject;
import data_access.Profile.ProfileDatabase;
import entity.Profile;
import interface_adapter.Profile.ProfileController;
import interface_adapter.Profile.ProfilePresenter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileCreationTest {

    @Test
    void controllerCreatesAndPersistsProfile() {
        ProfileDatabase database = new ProfileDatabase();
        ProfilePresenter presenter = new ProfilePresenter();
        ProfileInteractor interactor = new ProfileInteractor(
                new ProfileDataAccessObject(database), presenter);
        ProfileController controller = new ProfileController(interactor);

        controller.createProfile("alice", "secret", "alice@example.com", "555-1234");

        Profile savedProfile = database.getProfile("alice");
        assertNotNull(savedProfile);
        assertEquals("alice", savedProfile.getUsername());
        assertTrue(presenter.getOutputData().isSuccess());
        assertEquals("alice", presenter.getOutputData().getUsername());
    }

    @Test
    void duplicateUsernameIsRejectedWithoutReplacingProfile() {
        ProfileDatabase database = new ProfileDatabase();
        Profile existingProfile = new Profile("alice", "old-secret", "alice@example.com", "555-1234");
        database.saveProfile(existingProfile);
        ProfilePresenter presenter = new ProfilePresenter();
        ProfileInteractor interactor = new ProfileInteractor(
                new ProfileDataAccessObject(database), presenter);
        ProfileController controller = new ProfileController(interactor);

        controller.createProfile("alice", "new-secret", "other@example.com", "555-9876");

        assertSame(existingProfile, database.getProfile("alice"));
        assertFalse(presenter.getOutputData().isSuccess());
        assertEquals("Username already exists.", presenter.getOutputData().getMessage());
    }

    @Test
    void blankUsernameIsRejected() {
        ProfileDatabase database = new ProfileDatabase();
        ProfilePresenter presenter = new ProfilePresenter();
        ProfileInteractor interactor = new ProfileInteractor(
                new ProfileDataAccessObject(database), presenter);
        ProfileController controller = new ProfileController(interactor);

        controller.createProfile("  ", "secret", "alice@example.com", "555-1234");

        assertNull(database.getProfile("  "));
        assertFalse(presenter.getOutputData().isSuccess());
        assertEquals("Username cannot be empty.", presenter.getOutputData().getMessage());
    }
}