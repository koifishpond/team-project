import java.awt.CardLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import data_access.Profile.ProfileDataAccessObject;
import data_access.Profile.ProfileDatabase;
import data_access.Profile.ProfileJsonFileStorage;
import interface_adapter.Profile.CreateProfile.ProfileController;
import interface_adapter.Profile.CreateProfile.ProfilePresenter;
import interface_adapter.Profile.CreateProfile.ProfileViewModel;
import interface_adapter.Profile.Login.LoginController;
import interface_adapter.Profile.Login.LoginPresenter;
import interface_adapter.Profile.Login.LoginToken;
import interface_adapter.Profile.Login.LoginViewModel;
import interface_adapter.Profile.ResetPassword.ResetPasswordController;
import interface_adapter.Profile.ResetPassword.ResetPasswordPresenter;
import interface_adapter.Profile.ResetPassword.ResetPasswordViewModel;
import use_case.Profile.CreateProfile.ProfileInteractor;
import use_case.Profile.Login.LoginInteractor;
import use_case.Profile.ResetPassword.ResetPasswordInteractor;
import view.Profile.CreateProfile.ProfileView;
import view.Profile.Login.LoginView;
import view.Profile.ResetPassword.ResetPasswordView;

public class Main {
    private static final Path PROFILE_SAVE_FILE = Path.of("profiles.json");
    private static final String LOGIN_CARD = "login";
    private static final String PROFILE_CARD = "profile";
    private static final String RESET_PASSWORD_CARD = "reset-password";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ProfileDatabase profileDatabase = new ProfileDatabase();
            ProfileDataAccessObject profileDataAccess = new ProfileDataAccessObject(profileDatabase);
            ProfileJsonFileStorage profileJsonFileStorage = new ProfileJsonFileStorage(profileDataAccess);
            try {
                profileJsonFileStorage.loadProfilesFromJson(PROFILE_SAVE_FILE);
            } catch (IOException exception) {
                JOptionPane.showMessageDialog(null,
                        "Could not load profiles: " + exception.getMessage(),
                        "Profile load error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            ProfileViewModel profileViewModel = new ProfileViewModel();
            ProfilePresenter profilePresenter = new ProfilePresenter(profileViewModel);
            ProfileInteractor profileInteractor = new ProfileInteractor(profileDataAccess, profilePresenter);
            ProfileController profileController = new ProfileController(profileInteractor);

            LoginViewModel loginViewModel = new LoginViewModel();
            LoginToken loginToken = new LoginToken();
            LoginPresenter loginPresenter = new LoginPresenter(loginViewModel, loginToken);
            LoginInteractor loginInteractor = new LoginInteractor(profileDataAccess, loginPresenter);
            LoginController loginController = new LoginController(loginInteractor);

            ResetPasswordViewModel resetPasswordViewModel = new ResetPasswordViewModel();
            ResetPasswordPresenter resetPasswordPresenter = new ResetPasswordPresenter(resetPasswordViewModel);
            ResetPasswordInteractor resetPasswordInteractor = new ResetPasswordInteractor(
                    profileDataAccess, resetPasswordPresenter);
            ResetPasswordController resetPasswordController = new ResetPasswordController(resetPasswordInteractor);

            CardLayout cardLayout = new CardLayout();
            JPanel pages = new JPanel(cardLayout);
            LoginView loginView = new LoginView(
                    loginController,
                    loginViewModel,
                    () -> cardLayout.show(pages, PROFILE_CARD),
                    () -> cardLayout.show(pages, RESET_PASSWORD_CARD));
            ProfileView profileView = new ProfileView(
                    profileController,
                    profileViewModel, () -> cardLayout.show(pages, LOGIN_CARD)
            );
            ResetPasswordView resetPasswordView = new ResetPasswordView(
                    resetPasswordController,
                    resetPasswordViewModel,
                    () -> cardLayout.show(pages, LOGIN_CARD));
            pages.add(loginView, LOGIN_CARD);
            pages.add(profileView, PROFILE_CARD);
            pages.add(resetPasswordView, RESET_PASSWORD_CARD);
            cardLayout.show(pages, LOGIN_CARD);

            JFrame window = new JFrame("Log in");
            window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            window.setContentPane(pages);
            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    try {
                        profileJsonFileStorage.saveAllProfilesToJson(PROFILE_SAVE_FILE);
                        window.dispose();
                    } catch (IOException exception) {
                        JOptionPane.showMessageDialog(window,
                                "Could not save profiles: " + exception.getMessage(),
                                "Profile save error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}
