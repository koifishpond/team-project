package view.Explore;

import javax.swing.JOptionPane;

import java.util.function.Consumer;

import interface_adapter.discovery.DiscoveryViewModel;

import interface_adapter.Explore.ExploreState;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.List;

public class ExploreView extends JPanel {

    private static final long serialVersionUID = 1L;

    // Temporary fake candidates
    private final List<ExploreState> candidates = List.of(
            new ExploreState("Alex", 21,
                    "I enjoy hiking and gaming.",
                    "Hiking, Gaming",
                    "I study computer science and enjoy exploring new hiking trails."),
            new ExploreState("Jordan", 22,
                    "I love travelling and cooking.",
                    "Travel, Cooking",
                    "I love visiting new countries and trying different recipes."),
            new ExploreState("Taylor", 23,
                    "I enjoy music and playing sports.",
                    "Music, Sports",
                    "I play basketball every weekend and enjoy attending concerts.")
    );

    private int currentIndex = 0;
    private boolean profileOpened = false;
    private boolean externalCandidate = false;

    private final JLabel picture;
    private final JLabel name;
    private final JLabel bio;
    private final JLabel interests;

    private final JButton openButton;
    private final JButton passButton;
    private final JButton likeButton;

    private Consumer<String> interactionListener;

    public ExploreView() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Heading
        JLabel title = new JLabel("Explore", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        // Profile information
        JPanel profilePanel = new JPanel(new GridLayout(1, 2, 20, 0));

        picture = new JLabel("Profile Picture", SwingConstants.CENTER);
        picture.setOpaque(true);
        picture.setBackground(Color.LIGHT_GRAY);

        name = new JLabel("", SwingConstants.CENTER);
        name.setFont(new Font("SansSerif", Font.BOLD, 28));

        bio = new JLabel("", SwingConstants.CENTER);
        interests = new JLabel("", SwingConstants.CENTER);

        JPanel infoPanel = new JPanel(new GridLayout(3, 1));
        infoPanel.add(name);
        infoPanel.add(bio);
        infoPanel.add(interests);

        profilePanel.add(picture);
        profilePanel.add(infoPanel);

        add(profilePanel, BorderLayout.CENTER);

        // Buttons
        openButton = new JButton("Open Profile");
        passButton = new JButton("Pass");
        likeButton = new JButton("Like");

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3));

        buttonPanel.add(openButton);
        buttonPanel.add(passButton);
        buttonPanel.add(likeButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // Button actions
        openButton.addActionListener(event -> toggleProfile());


        passButton.addActionListener(event -> {
            if (externalCandidate) {
                if (interactionListener != null) {
                    interactionListener.accept("PASS");
                }
                return;
            }

            System.out.println("Passed: " + candidates.get(currentIndex).getName());
            nextProfile();
        });

        likeButton.addActionListener(event -> {
            if (externalCandidate) {
                if (interactionListener != null) {
                    interactionListener.accept("LIKE");
                }
                return;
            }

            System.out.println("Liked: " + candidates.get(currentIndex).getName());
            nextProfile();
        });


        // Display the first candidate
        displayProfile();
    }

    public void showCandidate(String displayName, int age,
                              String bioExcerpt, String interestText,
                              String distanceText) {
        externalCandidate = true;

        name.setText(displayName + ", " + age);
        bio.setText(bioExcerpt);
        interests.setText("<html><center>Interests: " + interestText
                + "<br>" + distanceText + "</center></html>");

        profileOpened = false;
        openButton.setText("Open Profile");

        openButton.setEnabled(true);
        passButton.setEnabled(true);
        likeButton.setEnabled(true);
    }

    public void displayDiscoveryResult(DiscoveryViewModel model) {
        if (model.getCard().isPresent()) {
            DiscoveryViewModel.Card card = model.getCard().get();

            showCandidate(
                    card.getDisplayName(),
                    card.getAgeYears(),
                    card.getBioExcerpt().orElse("No bio available"),
                    String.join(", ", card.getInterestLabels()),
                    card.getDistanceLabel().orElse("Distance unavailable")
            );
        } else {
            displayEmptyFeed();
        }
    }

    private void displayProfile() {
        externalCandidate = false;

        ExploreState candidate = candidates.get(currentIndex);

        name.setText(candidate.getName() + ", " + candidate.getAge());
        bio.setText(candidate.getBio());
        interests.setText("Interests: " + candidate.getInterests());
    }

    private void nextProfile() {
        profileOpened = false;
        openButton.setText("Open Profile");
        currentIndex++;

        if (currentIndex < candidates.size()) {
            displayProfile();
        } else {
            displayEmptyFeed();
        }
    }

    public void displayEmptyFeed() {
        profileOpened = false;
        openButton.setText("Open Profile");

        name.setText("No more profiles available");
        bio.setText("");
        interests.setText("");
        picture.setText("");

        openButton.setEnabled(false);
        passButton.setEnabled(false);
        likeButton.setEnabled(false);
    }

    private void toggleProfile() {
        if (externalCandidate) {
            if (interactionListener != null) {
                interactionListener.accept("OPEN_PROFILE");
            }
            return;
        }

        ExploreState candidate = candidates.get(currentIndex);

        profileOpened = !profileOpened;

        if (profileOpened) {
            bio.setText("<html><center>" + candidate.getFullBio()
                    + "</center></html>");
            openButton.setText("Back");
            System.out.println("Opened: " + candidate.getName());
        } else {
            bio.setText(candidate.getBio());
            openButton.setText("Open Profile");
        }
    }

    public void setInteractionListener(Consumer<String> listener) {
        this.interactionListener = listener;
    }

    public void showFullProfile(String displayName, int age,
                                String bio, String area,
                                String interests) {

        String details =
                displayName + ", " + age
                        + "\n\nBio: " + bio
                        + "\n\nLocation: " + area
                        + "\n\nInterests: " + interests;

        JOptionPane.showMessageDialog(
                this,
                details,
                "Full Profile",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

}
