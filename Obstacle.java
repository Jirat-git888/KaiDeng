import java.awt.*;

/** ก้อนหินกลางสนาม */
class Obstacle extends Entity {
    public Obstacle(double x, double y, int size) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.color = UI.GREY;
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int px = (int) x, py = (int) y;
        UI.shadowEllipse(g2, centerX() + 2, y + size * 0.92, size, size * 0.4, 90);
        g2.setColor(UI.DARK);
        g2.fillOval(px - 2, py - 2, size + 4, size + 4);
        g2.setColor(new Color(150, 142, 134));
        g2.fillOval(px, py, size, size);
        g2.setColor(new Color(186, 180, 172));
        g2.fillRect(px + size / 5, py + size / 6, size / 4, size / 6);
    }
}
