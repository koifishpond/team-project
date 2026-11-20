import data_access.Profile.ProfileDataAccessObject;
import data_access.Profile.ProfileDatabase;
import data_access.Profile.ProfileJsonFileStorage;
import interface_adapter.Login.LoginController;
import interface_adapter.Login.LoginPresenter;
import interface_adapter.Login.LoginViewModel;
import interface_adapter.Profile.ProfileController;
import interface_adapter.Profile.ProfilePresenter;
import interface_adapter.Profile.ProfileViewModel;
import use_case.Login.LoginInteractor;
import use_case.Profile.ProfileInteractor;
import view.Login.LoginView;
import view.Profile.ProfileView;

import java.io.IOException;
import java.nio.file.Path;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.CardLayout;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class Main {
    private static final Path PROFILE_SAVE_FILE = Path.of("profiles.json");
    private static final String LOGIN_CARD = "login";
    private static final String PROFILE_CARD = "profile";

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
            LoginPresenter loginPresenter = new LoginPresenter(loginViewModel);
            LoginInteractor loginInteractor = new LoginInteractor(profileDataAccess, loginPresenter);
            LoginController loginController = new LoginController(loginInteractor);

            CardLayout cardLayout = new CardLayout();
            JPanel pages = new JPanel(cardLayout);
            LoginView loginView = new LoginView(
                    loginController,
                    loginViewModel,
                    () -> cardLayout.show(pages, PROFILE_CARD));
            ProfileView profileView = new ProfileView(
                    profileController,
                    profileViewModel, () -> cardLayout.show(pages, LOGIN_CARD)
            );
            pages.add(loginView, LOGIN_CARD);
            pages.add(profileView, PROFILE_CARD);
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
