import java.awt.*;

/** ระเบิดไข่ที่ Balanced วางไว้ */
class EggMine extends Entity {
    public EggMine(double x, double y) {
        this.x = x;
        this.y = y;
        this.size = 20;
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int px = (int) x, py = (int) y;
        UI.shadowEllipse(g2, centerX() + 1, y + size, size * 0.8, 6, 90);
        g2.setColor(UI.DARK);
        g2.fillOval(px, py - 2, size, size + 2);
        g2.setColor(UI.CREAM);
        g2.fillOval(px + 2, py, size - 4, size - 2);
        g2.setColor(UI.RED);
        g2.fillRect(px + size / 2 - 3, py + size / 2 - 3, 6, 6);
    }
}
