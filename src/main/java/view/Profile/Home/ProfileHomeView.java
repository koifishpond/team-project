package view.Profile.Home;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.GridLayout;

public class ProfileHomeView extends JPanel {
    private static final long serialVersionUID = 1L;

    public enum Section {
        TEXTING,
        MATCHING,
        EDIT_PREFERENCE
    }

    @FunctionalInterface
    public interface NavigationHandler {
        void openSection(Section section);
    }

    public ProfileHomeView(NavigationHandler navigationHandler) {
        setLayout(new GridLayout(1, 3, 8, 8));
        add(createButton("Texting", Section.TEXTING, navigationHandler));
        add(createButton("Matching", Section.MATCHING, navigationHandler));
        add(createButton("Edit Preference", Section.EDIT_PREFERENCE, navigationHandler));
    }

    private JButton createButton(String label, Section section, NavigationHandler navigationHandler) {
        JButton button = new JButton(label);
        button.addActionListener(event -> navigationHandler.openSection(section));
        return button;
    }
}