package use_case.Profile.ResetPassword;

import data_access.Profile.ProfileDataAccessObject;
import data_access.Profile.ProfileDatabase;
import entity.Profile;
import interface_adapter.Profile.ResetPassword.ResetPasswordController;
import interface_adapter.Profile.ResetPassword.ResetPasswordPresenter;
import interface_adapter.Profile.ResetPassword.ResetPasswordViewModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResetPasswordInteractorTest {

    @Test
    void verifiesAccountAndUpdatesStoredPassword() {
        ProfileDatabase database = new ProfileDatabase();
        Profile profile = new Profile("alice", "old-password", "alice@example.com", "555-1234");
        database.saveProfile(profile);
        ResetPasswordViewModel viewModel = new ResetPasswordViewModel();
        ResetPasswordPresenter presenter = new ResetPasswordPresenter(viewModel);
        ResetPasswordInteractor interactor = new ResetPasswordInteractor(
                new ProfileDataAccessObject(database), presenter);
        ResetPasswordController controller = new ResetPasswordController(interactor);

        controller.verifyAccount("alice", "ALICE@example.com");
        assertEquals(ResetPasswordViewModel.Step.NEW_PASSWORD, viewModel.getState().getStep());

        controller.resetPassword("alice", "alice@example.com", "new-password");

        assertEquals("new-password", database.getProfile("alice").getPassword());
        assertTrue(viewModel.getState().isCompleted());
    }

    @Test
    void doesNotChangePasswordWhenEmailDoesNotMatch() {
        ProfileDatabase database = new ProfileDatabase();
        Profile profile = new Profile("alice", "old-password", "alice@example.com", "555-1234");
        database.saveProfile(profile);
        ResetPasswordViewModel viewModel = new ResetPasswordViewModel();
        ResetPasswordPresenter presenter = new ResetPasswordPresenter(viewModel);
        ResetPasswordInteractor interactor = new ResetPasswordInteractor(
                new ProfileDataAccessObject(database), presenter);

        interactor.verifyAccount(new ResetPasswordVerificationInputData("alice", "wrong@example.com"));

        assertFalse(viewModel.getState().isCompleted());
        assertEquals("Could not verify username and email.", viewModel.getState().getMessage());
        assertEquals("old-password", database.getProfile("alice").getPassword());
    }
}