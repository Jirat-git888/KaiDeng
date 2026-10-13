import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class CockfightGame {
    public static void main(String[] args) {
        // สร้าง UI บน Event Dispatch Thread ตามหลักของ Swing
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Java Cockfight RPG - Portals & Enrage");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(false);

            GameEngine gameEngine = new GameEngine();
            window.add(gameEngine);
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);

            gameEngine.requestFocusInWindow();
            gameEngine.startGame();
        });
    }
}
