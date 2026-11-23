package e2e;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import data_access.Profile.ProfileDataAccessObject;
import data_access.Profile.ProfileDatabase;
import interface_adapter.Profile.CreateProfile.ProfileController;
import interface_adapter.Profile.CreateProfile.ProfilePresenter;
import interface_adapter.Profile.CreateProfile.ProfileViewModel;
import interface_adapter.Profile.Login.LoginController;
import interface_adapter.Profile.Login.LoginPresenter;
import interface_adapter.Profile.Login.LoginToken;
import interface_adapter.Profile.Login.LoginViewModel;
import use_case.Profile.CreateProfile.ProfileInteractor;
import use_case.Profile.Login.LoginInteractor;
import view.Profile.CreateProfile.ProfileView;
import view.Profile.Login.LoginView;

class ProfileAuthenticationFlowTest {

    @Test
    void userCanCreateAProfileAndLogInThroughSwingPanels()
            throws InterruptedException, InvocationTargetException {
        AtomicReference<Throwable> failure = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            try {
                runProfileAndLoginFlow();
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });

        if (failure.get() != null) {
            throw new AssertionError("Swing profile flow failed", failure.get());
        }
    }

    private void runProfileAndLoginFlow() {
        ProfileDatabase database = new ProfileDatabase();
        ProfileDataAccessObject profileDataAccess = new ProfileDataAccessObject(database);

        ProfileViewModel profileViewModel = new ProfileViewModel();
        ProfilePresenter profilePresenter = new ProfilePresenter(profileViewModel);
        ProfileController profileController = new ProfileController(
                new ProfileInteractor(profileDataAccess, profilePresenter));

        LoginToken loginToken = new LoginToken();
        LoginViewModel loginViewModel = new LoginViewModel();
        LoginPresenter loginPresenter = new LoginPresenter(loginViewModel, loginToken);
        LoginController loginController = new LoginController(
                new LoginInteractor(profileDataAccess, loginPresenter));

        CardLayout cardLayout = new CardLayout();
        JPanel pages = new JPanel(cardLayout);
        ProfileView profileView = new ProfileView(
                profileController, profileViewModel, () -> cardLayout.show(pages, "login"));
        LoginView loginView = new LoginView(
                loginController,
                loginViewModel,
                () -> cardLayout.show(pages, "create-profile"),
                () -> { });
        pages.add(loginView, "login");
        pages.add(profileView, "create-profile");
        cardLayout.show(pages, "login");

        clickButton(loginView, "Create profile");
        assertTrue(profileView.isVisible());
        List<JTextField> creationFields = findComponents(profileView, JTextField.class);
        creationFields.get(0).setText("alice");
        ((JPasswordField) creationFields.get(1)).setText("secret");
        creationFields.get(2).setText("alice@example.com");
        creationFields.get(3).setText("4165551234");
        clickButton(profileView, "Create profile");

        assertTrue(profileViewModel.getState().isSuccess());
        assertTrue(database.existsByUsername("alice"));

        clickButton(profileView, "Back to login");
        assertTrue(loginView.isVisible());
        List<JTextField> loginFields = findComponents(loginView, JTextField.class);
        loginFields.get(0).setText("alice");
        ((JPasswordField) loginFields.get(1)).setText("secret");
        clickButton(loginView, "Log in");

        assertTrue(loginViewModel.getState().isSuccess());
        assertEquals(database.getProfile("alice").getUserId(), loginToken.getUserId());
    }

    private void clickButton(Container container, String text) {
        for (JButton button : findComponents(container, JButton.class)) {
            if (text.equals(button.getText())) {
                button.doClick();
                return;
            }
        }
        throw new AssertionError("Button not found: " + text);
    }

    private <T extends Component> List<T> findComponents(Container container, Class<T> componentType) {
        List<T> matches = new ArrayList<>();
        for (Component component : container.getComponents()) {
            if (componentType.isInstance(component)) {
                matches.add(componentType.cast(component));
            }
            if (component instanceof Container) {
                matches.addAll(findComponents((Container) component, componentType));
            }
        }
        return matches;
    }
}