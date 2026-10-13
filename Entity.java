import java.awt.Graphics;
import java.awt.Color;

/** วัตถุพื้นฐานทุกชนิดในเกม (มีตำแหน่ง ความเร็ว ขนาด และเช็คการชนแบบวงกลม) */
abstract class Entity {
    public double x, y, vx, vy;
    public int size;
    protected Color color;

    public int getX() { return (int) x; }
    public int getY() { return (int) y; }

    public double centerX() { return x + size / 2.0; }
    public double centerY() { return y + size / 2.0; }

    public boolean isCollidingWith(Entity other) {
        double dx = centerX() - other.centerX();
        double dy = centerY() - other.centerY();
        return Math.sqrt(dx * dx + dy * dy) < (this.size + other.size) / 2.0;
    }

    public boolean isCollidingWithPoint(int px, int py, int tolerance) {
        double dx = px - centerX();
        double dy = py - centerY();
        return Math.sqrt(dx * dx + dy * dy) < tolerance;
    }

    public abstract void draw(Graphics g);
}
