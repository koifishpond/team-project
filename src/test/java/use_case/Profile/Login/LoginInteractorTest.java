package use_case.Profile.Login;

import data_access.Profile.ProfileDataAccessObject;
import data_access.Profile.ProfileDatabase;
import entity.Profile;
import interface_adapter.Profile.Login.LoginController;
import interface_adapter.Profile.Login.LoginPresenter;
import interface_adapter.Profile.Login.LoginViewModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginInteractorTest {

    @Test
    void acceptsMatchingStoredCredentials() {
        ProfileDatabase database = new ProfileDatabase();
        database.saveProfile(new Profile("alice", "secret", "alice@example.com", "555-1234"));
        LoginViewModel viewModel = new LoginViewModel();
        LoginPresenter presenter = new LoginPresenter(viewModel);
        LoginInteractor interactor = new LoginInteractor(
                new ProfileDataAccessObject(database), presenter);
        LoginController controller = new LoginController(interactor);

        controller.login("alice", "secret");

        assertTrue(viewModel.getState().isSuccess());
        assertEquals("alice", viewModel.getState().getUsername());
    }

    @Test
    void rejectsWrongPasswordAndUnknownUsernameWithSameMessage() {
        ProfileDatabase database = new ProfileDatabase();
        database.saveProfile(new Profile("alice", "secret", "alice@example.com", "555-1234"));
        LoginViewModel viewModel = new LoginViewModel();
        LoginPresenter presenter = new LoginPresenter(viewModel);
        LoginInteractor interactor = new LoginInteractor(
                new ProfileDataAccessObject(database), presenter);

        interactor.execute(new LoginInputData("alice", "wrong"));
        assertFalse(viewModel.getState().isSuccess());
        assertEquals("Invalid username or password.", viewModel.getState().getMessage());

        interactor.execute(new LoginInputData("unknown", "secret"));
        assertFalse(viewModel.getState().isSuccess());
        assertEquals("Invalid username or password.", viewModel.getState().getMessage());
    }
}