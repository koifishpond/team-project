package view.Profile.CreateProfile;

import interface_adapter.Profile.CreateProfile.ProfileController;
import interface_adapter.Profile.CreateProfile.ProfileViewModel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.SwingUtilities;

public class ProfileView extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color SUCCESS_COLOR = new Color(35, 120, 65);
    private static final Color FAILURE_COLOR = new Color(175, 45, 45);

    private final ProfileController profileController;
    private final ProfileViewModel profileViewModel;
    private final Runnable onReturnToLogin;
    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JTextField emailField = new JTextField(20);
    private final JTextField phoneNumberField = new JTextField(20);
    private final JLabel statusLabel = new JLabel(" ");

    public ProfileView(ProfileController profileController, ProfileViewModel profileViewModel,
                       Runnable onReturnToLogin) {
        this.profileController = profileController;
        this.profileViewModel = profileViewModel;
        this.onReturnToLogin = onReturnToLogin;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel fieldsPanel = new JPanel(new GridLayout(0, 2, 8, 8));
        addField(fieldsPanel, "Username", usernameField);
        addField(fieldsPanel, "Password", passwordField);
        addField(fieldsPanel, "Email", emailField);
        addField(fieldsPanel, "Phone number", phoneNumberField);

        JButton createButton = new JButton("Create profile");
        createButton.addActionListener(event -> createProfile());
        JButton returnButton = new JButton("Back to login");
        returnButton.addActionListener(event -> onReturnToLogin.run());

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.add(returnButton);
        actionPanel.add(createButton);

        add(fieldsPanel, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);
        add(statusLabel, BorderLayout.NORTH);
        profileViewModel.addPropertyChangeListener(event -> updateStatus());
    }

    private void addField(JPanel panel, String label, JComponent field) {
        panel.add(new JLabel(label));
        panel.add(field);
    }

    private void createProfile() {
        profileController.createProfile(
                usernameField.getText(),
                new String(passwordField.getPassword()),
                emailField.getText(),
                phoneNumberField.getText()
        );
    }

    private void updateStatus() {
        Runnable update = () -> {
            ProfileViewModel.State state = profileViewModel.getState();
            statusLabel.setText(state.getMessage());
            statusLabel.setForeground(state.isSuccess() ? SUCCESS_COLOR : FAILURE_COLOR);
        };
        if (SwingUtilities.isEventDispatchThread()) {
            update.run();
        } else {
            SwingUtilities.invokeLater(update);
        }
    }
}