import java.awt.Graphics2D;

/**
 * วัตถุทุกชนิดในโลกเกม: มีตำแหน่ง ความเร็ว ขนาด (วงกลมชน) และอายุ (ใช้ทำแอนิเมชัน)
 * ข้อมูลเป็น private คลาสลูกและภายนอกต้องเข้าถึงผ่านเมธอดเท่านั้น
 */
abstract class Entity {
    private double x, y, vx, vy;
    private int size;
    private int age;

    protected Entity(double x, double y, int size) {
        this.x = x;
        this.y = y;
        this.size = size;
    }

    public final double x() { return x; }
    public final double y() { return y; }
    public final double vx() { return vx; }
    public final double vy() { return vy; }
    public final int size() { return size; }
    public final double centerX() { return x + size / 2.0; }
    public final double centerY() { return y + size / 2.0; }
    public final int age() { return age; }

    public final void setPosition(double x, double y) { this.x = x; this.y = y; }
    public final void translate(double dx, double dy) { x += dx; y += dy; }
    public final void setVelocity(double vx, double vy) { this.vx = vx; this.vy = vy; }
    public final void scaleVelocity(double factor) { vx *= factor; vy *= factor; }
    public final void reflectX() { vx = -vx; }
    public final void reflectY() { vy = -vy; }
    protected final void setSize(int size) { this.size = size; }

    /** เดินหน้าอนิเมชันหนึ่งก้าว (เรียกทุก tick โดยฉากที่เป็นเจ้าของ) */
    public void tick() { age++; }

    /** วาดตัวเองลงจอ */
    public abstract void draw(Graphics2D g);

    public boolean isMoving() { return Math.abs(vx) > 0.1 || Math.abs(vy) > 0.1; }

    public boolean collidesWith(Entity other) {
        double dx = centerX() - other.centerX();
        double dy = centerY() - other.centerY();
        return Math.hypot(dx, dy) < (size + other.size) / 2.0;
    }

    /** จุด (px, py) อยู่ห่างจากจุดกึ่งกลางไม่เกิน tolerance หรือไม่ */
    public boolean isNear(double px, double py, int tolerance) {
        return Math.hypot(px - centerX(), py - centerY()) < tolerance;
    }
}
