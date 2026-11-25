package integration.Profile;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import data_access.Profile.ProfileDataAccessObject;
import data_access.Profile.ProfileDatabase;
import data_access.Profile.ProfileJsonFileStorage;
import entity.Profile;
import interface_adapter.Profile.Login.LoginController;
import interface_adapter.Profile.Login.LoginPresenter;
import interface_adapter.Profile.Login.LoginToken;
import interface_adapter.Profile.Login.LoginViewModel;
import use_case.Profile.Login.LoginInteractor;

class LoginFlowIntegrationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void loadsSavedProfileAndLogsInThroughRealDataAccess() throws IOException {
        ProfileDatabase savedDatabase = new ProfileDatabase();
        Profile savedProfile = new Profile("alice", "secret", "alice@example.com", "4165551234");
        savedDatabase.saveProfile(savedProfile);
        Path saveFile = temporaryDirectory.resolve("profiles.json");
        new ProfileJsonFileStorage(new ProfileDataAccessObject(savedDatabase))
                .saveAllProfilesToJson(saveFile);

        ProfileDatabase loadedDatabase = new ProfileDatabase();
        ProfileDataAccessObject profileDataAccess = new ProfileDataAccessObject(loadedDatabase);
        new ProfileJsonFileStorage(profileDataAccess).loadProfilesFromJson(saveFile);

        LoginToken loginToken = new LoginToken();
        LoginViewModel viewModel = new LoginViewModel();
        LoginPresenter presenter = new LoginPresenter(viewModel, loginToken);
        LoginInteractor interactor = new LoginInteractor(profileDataAccess, presenter);
        LoginController controller = new LoginController(interactor);

        controller.login("alice", "secret");

        assertTrue(viewModel.getState().isSuccess());
        assertEquals(savedProfile.getUserId(), loginToken.getUserId());
    }
}