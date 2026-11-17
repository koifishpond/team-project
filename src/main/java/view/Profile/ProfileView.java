package view.Profile;

import interface_adapter.Profile.ProfileController;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

public class ProfileView extends JPanel {
    private static final long serialVersionUID = 1L;

    private final ProfileController profileController;
    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JTextField emailField = new JTextField(20);
    private final JTextField phoneNumberField = new JTextField(20);

    public ProfileView(ProfileController profileController) {
        this.profileController = profileController;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel fieldsPanel = new JPanel(new GridLayout(0, 2, 8, 8));
        addField(fieldsPanel, "Username", usernameField);
        addField(fieldsPanel, "Password", passwordField);
        addField(fieldsPanel, "Email", emailField);
        addField(fieldsPanel, "Phone number", phoneNumberField);

        JButton createButton = new JButton("Create profile");
        createButton.addActionListener(event -> createProfile());

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actionPanel.add(createButton);

        add(fieldsPanel, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);
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
}