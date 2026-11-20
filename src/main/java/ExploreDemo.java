
import view.Explore.ExploreView;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class ExploreDemo {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            JFrame window = new JFrame("Explore Demo");

            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setContentPane(new ExploreView());
            window.setSize(900, 600);
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}
