package view.Login;

import interface_adapter.Login.LoginController;
import interface_adapter.Login.LoginViewModel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;

public class LoginView extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color SUCCESS_COLOR = new Color(35, 120, 65);
    private static final Color FAILURE_COLOR = new Color(175, 45, 45);

    private final LoginController loginController;
    private final Runnable onCreateProfile;
    private final LoginViewModel loginViewModel;
    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JLabel statusLabel = new JLabel(" ");

    public LoginView(LoginController loginController, LoginViewModel loginViewModel,
                     Runnable onCreateProfile) {
        this.loginController = loginController;
        this.loginViewModel = loginViewModel;
        this.onCreateProfile = onCreateProfile;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel fieldsPanel = new JPanel(new GridLayout(0, 2, 8, 8));
        addField(fieldsPanel, "Username", usernameField);
        addField(fieldsPanel, "Password", passwordField);

        JButton loginButton = new JButton("Log in");
        loginButton.addActionListener(event -> login());
        JButton createProfileButton = new JButton("Create profile");
        createProfileButton.addActionListener(event -> onCreateProfile.run());

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(loginButton);
        buttonsPanel.add(createProfileButton);

        JPanel footerPanel = new JPanel(new BorderLayout(8, 8));
        footerPanel.add(statusLabel, BorderLayout.CENTER);
        footerPanel.add(buttonsPanel, BorderLayout.EAST);

        add(fieldsPanel, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);
        loginViewModel.addPropertyChangeListener(event -> updateStatus());
    }

    private void addField(JPanel panel, String label, JComponent field) {
        panel.add(new JLabel(label));
        panel.add(field);
    }

    private void login() {
        loginController.login(usernameField.getText(), new String(passwordField.getPassword()));
    }

    private void updateStatus() {
        Runnable update = () -> {
            LoginViewModel.State state = loginViewModel.getState();
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