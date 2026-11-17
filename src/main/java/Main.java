import data_access.Profile.ProfileDataAccessObject;
import data_access.Profile.ProfileDatabase;
import data_access.Profile.ProfileJsonFileStorage;
import interface_adapter.Profile.ProfileController;
import interface_adapter.Profile.ProfilePresenter;
import interface_adapter.Profile.ProfileViewModel;
import use_case.Profile.ProfileInteractor;
import view.Profile.ProfileView;

import java.io.IOException;
import java.nio.file.Path;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Main {
    private static final Path PROFILE_SAVE_FILE = Path.of("profiles.json");

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
            ProfileView profileView = new ProfileView(profileController);

            JFrame window = new JFrame("Create Profile");
            window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            window.setContentPane(profileView);
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
