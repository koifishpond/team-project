import data_access.Profile.ProfileDataAccessObject;
import data_access.Profile.ProfileDatabase;
import interface_adapter.Profile.ProfileController;
import interface_adapter.Profile.ProfilePresenter;
import interface_adapter.Profile.ProfileViewModel;
import use_case.Profile.ProfileInteractor;
import view.Profile.ProfileView;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			ProfileDatabase profileDatabase = new ProfileDatabase();
			ProfileDataAccessObject profileDataAccess = new ProfileDataAccessObject(profileDatabase);
			ProfileViewModel profileViewModel = new ProfileViewModel();
			ProfilePresenter profilePresenter = new ProfilePresenter(profileViewModel);
			ProfileInteractor profileInteractor = new ProfileInteractor(profileDataAccess, profilePresenter);
			ProfileController profileController = new ProfileController(profileInteractor);
			ProfileView profileView = new ProfileView(profileController);

			JFrame window = new JFrame("Create Profile");
			window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			window.setContentPane(profileView);
			window.pack();
			window.setLocationRelativeTo(null);
			window.setVisible(true);
		});
	}
}
