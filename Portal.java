import java.awt.*;

/** หลุมวาร์ป ลิงก์เป็นคู่ แต่ละหลุมมีสีประจำตัวให้รู้ว่าคู่กับหลุมไหน */
class Portal extends Entity {
    public Portal linkedPortal;
    Color tint = UI.PURPLE;

    public Portal(double x, double y, int size) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.color = tint;
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int px = (int) x, py = (int) y, d = size;
        g2.setColor(UI.DARK);
        g2.fillOval(px - 2, py - 2, d + 4, d + 4);
        g2.setColor(tint);
        g2.fillOval(px, py, d, d);
        g2.setColor(UI.darker(tint, 0.5));
        g2.fillOval(px + 7, py + 7, d - 14, d - 14);

        double a = GameEngine.tick * 0.07;                 // จุดหมุนรอบวง อย่างเดียวที่เคลื่อนไหว
        int dx = (int) (centerX() + Math.cos(a) * (d / 2.0 - 4)) - 2;
        int dy = (int) (centerY() + Math.sin(a) * (d / 2.0 - 4)) - 2;
        g2.setColor(UI.CREAM);
        g2.fillRect(dx, dy, 4, 4);
    }
}
