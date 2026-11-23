package use_case.Profile.Login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import data_access.Profile.ProfileDataAccessObject;
import data_access.Profile.ProfileDatabase;
import entity.Profile;
import interface_adapter.Profile.Login.LoginController;
import interface_adapter.Profile.Login.LoginPresenter;
import interface_adapter.Profile.Login.LoginToken;
import interface_adapter.Profile.Login.LoginViewModel;

class LoginInteractorTest {

    @Test
    void acceptsMatchingStoredCredentials() {
        ProfileDatabase database = new ProfileDatabase();
        database.saveProfile(new Profile("alice", "secret", "alice@example.com", "555-1234"));
        LoginViewModel viewModel = new LoginViewModel();
        LoginToken loginToken = new LoginToken();
        LoginPresenter presenter = new LoginPresenter(viewModel, loginToken);
        LoginInteractor interactor = new LoginInteractor(
                new ProfileDataAccessObject(database), presenter);
        LoginController controller = new LoginController(interactor);

        assertNull(loginToken.getUserId());
        controller.login("alice", "secret");

        assertTrue(viewModel.getState().isSuccess());
        assertEquals("alice", viewModel.getState().getUsername());
        assertEquals(database.getProfile("alice").getUserId(), loginToken.getUserId());
    }

    @Test
    void rejectsWrongPasswordAndUnknownUsernameWithSameMessage() {
        ProfileDatabase database = new ProfileDatabase();
        database.saveProfile(new Profile("alice", "secret", "alice@example.com", "555-1234"));
        LoginViewModel viewModel = new LoginViewModel();
        LoginToken loginToken = new LoginToken();
        LoginPresenter presenter = new LoginPresenter(viewModel, loginToken);
        LoginInteractor interactor = new LoginInteractor(
                new ProfileDataAccessObject(database), presenter);

        interactor.execute(new LoginInputData("alice", "secret"));
        assertEquals(database.getProfile("alice").getUserId(), loginToken.getUserId());

        interactor.execute(new LoginInputData("alice", "wrong"));
        assertFalse(viewModel.getState().isSuccess());
        assertEquals("Invalid username or password.", viewModel.getState().getMessage());
        assertNull(loginToken.getUserId());

        interactor.execute(new LoginInputData("unknown", "secret"));
        assertFalse(viewModel.getState().isSuccess());
        assertEquals("Invalid username or password.", viewModel.getState().getMessage());
        assertNull(loginToken.getUserId());
    }
}