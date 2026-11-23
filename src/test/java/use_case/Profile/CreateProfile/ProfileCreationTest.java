package use_case.Profile.CreateProfile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import data_access.Profile.ProfileDataAccessObject;
import data_access.Profile.ProfileDatabase;
import entity.Profile;
import interface_adapter.Profile.CreateProfile.ProfileController;
import interface_adapter.Profile.CreateProfile.ProfilePresenter;
import interface_adapter.Profile.CreateProfile.ProfileViewModel;

class ProfileCreationTest {

    @Test
    void controllerCreatesAndPersistsProfile() {
        ProfileDatabase database = new ProfileDatabase();
        ProfileViewModel viewModel = new ProfileViewModel();
        ProfilePresenter presenter = new ProfilePresenter(viewModel);
        ProfileInteractor interactor = new ProfileInteractor(
                new ProfileDataAccessObject(database), presenter);
        ProfileController controller = new ProfileController(interactor);

        controller.createProfile("alice", "secret", "alice@example.com", "4165551234");

        Profile savedProfile = database.getProfile("alice");
        assertNotNull(savedProfile);
        assertEquals("alice", savedProfile.getUsername());
        assertTrue(viewModel.getState().isSuccess());
        assertEquals("alice", viewModel.getState().getUsername());
    }

    @Test
    void duplicateUsernameIsRejectedWithoutReplacingProfile() {
        ProfileDatabase database = new ProfileDatabase();
        Profile existingProfile = new Profile("alice", "old-secret", "alice@example.com", "4165551234");
        database.saveProfile(existingProfile);
        ProfileViewModel viewModel = new ProfileViewModel();
        ProfilePresenter presenter = new ProfilePresenter(viewModel);
        ProfileInteractor interactor = new ProfileInteractor(
                new ProfileDataAccessObject(database), presenter);
        ProfileController controller = new ProfileController(interactor);

        controller.createProfile("alice", "new-secret", "other@example.com", "4165559876");

        assertSame(existingProfile, database.getProfile("alice"));
        assertFalse(viewModel.getState().isSuccess());
        assertEquals("Username already exists.", viewModel.getState().getMessage());
    }

    @Test
    void duplicateEmailIsRejectedWithoutSavingProfile() {
        ProfileDatabase database = new ProfileDatabase();
        Profile existingProfile = new Profile("alice", "old-secret", "alice@example.com", "4165551234");
        database.saveProfile(existingProfile);
        ProfileViewModel viewModel = new ProfileViewModel();
        ProfilePresenter presenter = new ProfilePresenter(viewModel);
        ProfileInteractor interactor = new ProfileInteractor(
                new ProfileDataAccessObject(database), presenter);
        ProfileController controller = new ProfileController(interactor);

        controller.createProfile("bob", "new-secret", " ALICE@example.com ", "4165559876");

        assertNull(database.getProfile("bob"));
        assertFalse(viewModel.getState().isSuccess());
        assertEquals("Email already exists.", viewModel.getState().getMessage());
    }

    @Test
    void duplicatePhoneNumberIsRejectedWithoutSavingProfile() {
        ProfileDatabase database = new ProfileDatabase();
        Profile existingProfile = new Profile("alice", "old-secret", "alice@example.com", "4165551234");
        database.saveProfile(existingProfile);
        ProfileViewModel viewModel = new ProfileViewModel();
        ProfilePresenter presenter = new ProfilePresenter(viewModel);
        ProfileInteractor interactor = new ProfileInteractor(
                new ProfileDataAccessObject(database), presenter);
        ProfileController controller = new ProfileController(interactor);

        controller.createProfile("bob", "new-secret", "bob@example.com", "4165551234");

        assertNull(database.getProfile("bob"));
        assertFalse(viewModel.getState().isSuccess());
        assertEquals("Phone number already exists.", viewModel.getState().getMessage());
    }

    @Test
    void blankUsernameIsRejected() {
        ProfileDatabase database = new ProfileDatabase();
        ProfileViewModel viewModel = new ProfileViewModel();
        ProfilePresenter presenter = new ProfilePresenter(viewModel);
        ProfileInteractor interactor = new ProfileInteractor(
                new ProfileDataAccessObject(database), presenter);
        ProfileController controller = new ProfileController(interactor);

        controller.createProfile("  ", "secret", "alice@example.com", "4165551234");

        assertNull(database.getProfile("  "));
        assertFalse(viewModel.getState().isSuccess());
        assertEquals("Username cannot be empty.", viewModel.getState().getMessage());
    }

    @Test
    void blankPasswordIsRejected() {
        ProfileDatabase database = new ProfileDatabase();
        ProfileViewModel viewModel = new ProfileViewModel();
        ProfilePresenter presenter = new ProfilePresenter(viewModel);
        ProfileInteractor interactor = new ProfileInteractor(
                new ProfileDataAccessObject(database), presenter);

        interactor.execute(new ProfileInputData("alice", "  ", "alice@example.com", "4165551234"));

        assertNull(database.getProfile("alice"));
        assertFalse(viewModel.getState().isSuccess());
        assertEquals("Password cannot be empty.", viewModel.getState().getMessage());
    }

    @Test
    void emailWithoutAtSignIsRejected() {
        ProfileDatabase database = new ProfileDatabase();
        ProfileViewModel viewModel = new ProfileViewModel();
        ProfilePresenter presenter = new ProfilePresenter(viewModel);
        ProfileInteractor interactor = new ProfileInteractor(
                new ProfileDataAccessObject(database), presenter);

        interactor.execute(new ProfileInputData("alice", "secret", "alice.example.com", "4165551234"));

        assertNull(database.getProfile("alice"));
        assertFalse(viewModel.getState().isSuccess());
        assertEquals("Email must contain an @ symbol.", viewModel.getState().getMessage());
    }

    @Test
    void phoneNumberMustContainExactlyTenDigits() {
        ProfileDatabase database = new ProfileDatabase();
        ProfileViewModel viewModel = new ProfileViewModel();
        ProfilePresenter presenter = new ProfilePresenter(viewModel);
        ProfileInteractor interactor = new ProfileInteractor(
                new ProfileDataAccessObject(database), presenter);

        interactor.execute(new ProfileInputData("alice", "secret", "alice@example.com", "416-555-123"));

        assertNull(database.getProfile("alice"));
        assertFalse(viewModel.getState().isSuccess());
        assertEquals("Phone number must contain exactly 10 digits.", viewModel.getState().getMessage());
    }
}