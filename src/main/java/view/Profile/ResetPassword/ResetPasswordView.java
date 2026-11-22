package view.Profile.ResetPassword;

import interface_adapter.Profile.ResetPassword.ResetPasswordController;
import interface_adapter.Profile.ResetPassword.ResetPasswordViewModel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.GridLayout;

public class ResetPasswordView extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String VERIFY_CARD = "verify";
    private static final String PASSWORD_CARD = "new-password";
    private static final Color SUCCESS_COLOR = new Color(35, 120, 65);
    private static final Color FAILURE_COLOR = new Color(175, 45, 45);

    private final ResetPasswordController controller;
    private final ResetPasswordViewModel viewModel;
    private final Runnable onReturnToLogin;
    private final CardLayout stepLayout = new CardLayout();
    private final JPanel stepCards = new JPanel(stepLayout);
    private final JTextField usernameField = new JTextField(20);
    private final JTextField emailField = new JTextField(20);
    private final JPasswordField newPasswordField = new JPasswordField(20);
    private final JLabel statusLabel = new JLabel(" ");

    public ResetPasswordView(ResetPasswordController controller,
                             ResetPasswordViewModel viewModel,
                             Runnable onReturnToLogin) {
        this.controller = controller;
        this.viewModel = viewModel;
        this.onReturnToLogin = onReturnToLogin;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        stepCards.add(createVerificationPanel(), VERIFY_CARD);
        stepCards.add(createNewPasswordPanel(), PASSWORD_CARD);
        add(stepCards, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        viewModel.addPropertyChangeListener(event -> updateView());
    }

    private JPanel createVerificationPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
        addField(fields, "Username", usernameField);
        addField(fields, "Email", emailField);

        JButton verifyButton = new JButton("Continue");
        verifyButton.addActionListener(event -> verifyAccount());
        JButton backButton = new JButton("Back to login");
        backButton.addActionListener(event -> onReturnToLogin.run());

        JPanel buttons = new JPanel();
        buttons.add(backButton);
        buttons.add(verifyButton);
        panel.add(fields, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createNewPasswordPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
        addField(fields, "New password", newPasswordField);

        JButton backButton = new JButton("Back to login");
        backButton.addActionListener(event -> onReturnToLogin.run());
        JButton resetButton = new JButton("Reset password");
        resetButton.addActionListener(event -> resetPassword());

        JPanel buttons = new JPanel();
        buttons.add(backButton);
        buttons.add(resetButton);
        panel.add(fields, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void addField(JPanel panel, String label, JComponent field) {
        panel.add(new JLabel(label));
        panel.add(field);
    }

    private void verifyAccount() {
        controller.verifyAccount(usernameField.getText(), emailField.getText());
    }

    private void resetPassword() {
        controller.resetPassword(
                usernameField.getText(),
                emailField.getText(),
                new String(newPasswordField.getPassword())
        );
    }

    private void updateView() {
        Runnable update = () -> {
            ResetPasswordViewModel.State state = viewModel.getState();
            String card = state.getStep() == ResetPasswordViewModel.Step.VERIFY_ACCOUNT
                    ? VERIFY_CARD : PASSWORD_CARD;
            stepLayout.show(stepCards, card);
            statusLabel.setText(state.getMessage());
            statusLabel.setForeground(state.isCompleted() ? SUCCESS_COLOR : FAILURE_COLOR);
            if (state.isCompleted()) {
                onReturnToLogin.run();
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            update.run();
        } else {
            SwingUtilities.invokeLater(update);
        }
    }
}