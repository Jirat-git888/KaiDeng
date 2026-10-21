import javax.swing.ImageIcon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * ไก่ทุกตัว (ฮีโร่และบอส): มีเลือด พลังโจมตี ความเร็ว และฟิสิกส์การเคลื่อนที่
 * ข้อมูลสถานะเป็น private คลาสลูกแก้ไขผ่านเมธอด protected เท่านั้น
 */
abstract class Chicken extends Entity {
    private static final double FRICTION = 0.94;

    private final String name;
    private final int maxHp;
    private int hp;
    private int attack;
    private double speed;
    private Color color;
    private int hitCooldown;
    private Arena.Portal lastWarpedPortal;

    private final Image sprite;
    private final Map<Integer, Image> spriteCache = new HashMap<>();
    private final List<Trail> trails = new ArrayList<>();

    /** เงารอยพุ่งที่ค่อยๆ จาง */
    private static final class Trail {
        final double x, y;
        int alpha = 100;
        Trail(double x, double y) { this.x = x; this.y = y; }
    }

    protected Chicken(String name, int hp, int attack, double speed, Color color, int size, String spritePath) {
        super(0, 0, size);
        this.name = name;
        this.hp = this.maxHp = hp;
        this.attack = attack;
        this.speed = speed;
        this.color = color;
        this.sprite = new ImageIcon(spritePath).getImage();
    }

    // ---------- สถานะ ----------
    public final String getName() { return name; }
    public final int getHp() { return hp; }
    public final int getMaxHp() { return maxHp; }
    public final int getAttack() { return attack; }
    public final double getSpeed() { return speed; }
    public final Color getColor() { return color; }
    public final boolean isAlive() { return hp > 0; }
    public final double hpFraction() { return maxHp == 0 ? 0 : Math.max(0, hp) / (double) maxHp; }
    public final boolean isOnHitCooldown() { return hitCooldown > 0; }
    public final Arena.Portal lastWarpedPortal() { return lastWarpedPortal; }

    protected final void setAttack(int attack) { this.attack = attack; }
    protected final void setSpeed(double speed) { this.speed = speed; }
    protected final void setColor(Color color) { this.color = color; }

    // ---------- พฤติกรรม ----------
    public void takeDamage(int amount, Effects fx) {
        hp -= amount;
        fx.floatText((int) (x() + size() / 2.0), (int) y(), "-" + amount, UI.RED);
        hitCooldown = 30;
    }

    public final void heal(int amount) { hp = Math.min(hp + amount, maxHp); }

    /** วางไก่ที่ตำแหน่งใหม่ในสภาพนิ่งและสะอาด (ใช้ตอนเริ่มต่อสู้) */
    public void spawnAt(double x, double y) {
        setPosition(x, y);
        setVelocity(0, 0);
        trails.clear();
        lastWarpedPortal = null;
    }

    public void updatePhysics(Arena arena) {
        if (hitCooldown > 0) hitCooldown--;
        if (isMoving() && Math.hypot(vx(), vy()) > 4.0) trails.add(new Trail(x(), y()));

        translate(vx(), vy());
        scaleVelocity(FRICTION);
        setVelocity(Math.abs(vx()) < 0.2 ? 0 : vx(), Math.abs(vy()) < 0.2 ? 0 : vy());

        int left = arena.left(), top = arena.top(), right = left + arena.width(), bottom = top + arena.height();
        if (x() <= left)             { setPosition(left, y());               reflectX(); }
        if (x() + size() >= right)   { setPosition(right - size(), y());     reflectX(); }
        if (y() <= top)              { setPosition(x(), top);                reflectY(); }
        if (y() + size() >= bottom)  { setPosition(x(), bottom - size());    reflectY(); }

        Iterator<Trail> it = trails.iterator();
        while (it.hasNext()) {
            Trail t = it.next();
            t.alpha -= 8;
            if (t.alpha <= 0) it.remove();
        }
    }

    public void handleObstacles(Arena arena, Effects fx) {
        for (Arena.Obstacle obs : arena.obstacles()) {
            if (!collidesWith(obs)) continue;

            double dx = centerX() - obs.centerX();
            double dy = centerY() - obs.centerY();
            double dist = Math.hypot(dx, dy);
            if (dist == 0) { dx = 1; dy = 0; dist = 1; }

            double nx = dx / dist, ny = dy / dist;
            double dot = vx() * nx + vy() * ny;
            if (dot < 0) {
                setVelocity(vx() - 2 * dot * nx, vy() - 2 * dot * ny);
                fx.burst(obs.centerX(), obs.centerY(), Color.WHITE, 4);
                fx.shake(1);
            }
            double overlap = (size() + obs.size()) / 2.0 - dist;
            if (overlap > 0) translate(nx * overlap, ny * overlap);
        }
    }

    public void handlePortals(Arena arena, Effects fx) {
        // ออกจากรัศมีหลุมที่เพิ่งโผล่แล้วค่อยปลดล็อก ไม่งั้นจะโดนดูดกลับทันที
        if (lastWarpedPortal != null && !collidesWith(lastWarpedPortal)) lastWarpedPortal = null;

        for (Arena.Portal p : arena.portals()) {
            if (p == lastWarpedPortal || p.linked() == null || !collidesWith(p)) continue;
            Arena.Portal exit = p.linked();
            setPosition(exit.x() + (exit.size() - size()) / 2.0, exit.y() + (exit.size() - size()) / 2.0);
            lastWarpedPortal = exit;
            fx.burst(centerX(), centerY(), Color.CYAN, 15);
            fx.shake(3);
            break;
        }
    }

    // ---------- การวาด ----------
    /** ภาพสไปรต์ที่ใส่กล่อง s x s ไว้แล้ว (รักษาสัดส่วน เท้าชิดล่าง) คืน null ถ้าโหลดรูปไม่สำเร็จ */
    public final Image sprite(int s) {
        if (sprite == null || sprite.getWidth(null) <= 0) return null;
        return spriteCache.computeIfAbsent(s, k -> UI.scaleFit(sprite, k));
    }

    /** ฮุคให้คลาสลูกวาดเอฟเฟกต์ใต้ตัว (วงแหวนบัฟ/ออร่า) ก่อนวาดตัวไก่ */
    protected void drawAura(Graphics2D g) { }

    /** สีหลอดเลือดเหนือหัว (ฮีโร่เขียว เหลือน้อยเป็นแดง) */
    protected Color barColor(double frac) { return frac < 0.3 ? UI.RED : UI.GREEN; }

    @Override
    public void draw(Graphics2D g) {
        drawAura(g);
        for (Trail t : trails) {
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), t.alpha));
            g.fillOval((int) t.x, (int) t.y, size(), size());
        }

        UI.shadowEllipse(g, centerX(), y() + size() * 0.92, size() * 0.85, Math.max(5, size() * 0.3), 90);

        Image img = sprite(size());
        if (img != null) {
            g.drawImage(img, (int) x(), (int) y(), null);
        } else {
            g.setColor(UI.DARK);
            g.fillOval((int) x() - 2, (int) y() - 2, size() + 4, size() + 4);
            g.setColor(color);
            g.fillOval((int) x(), (int) y(), size(), size());
        }
        drawHpBar(g);
    }

    private void drawHpBar(Graphics2D g) {
        double frac = hpFraction();
        int bw = Math.max(size(), 36), bh = 8;
        int bx = (int) (x() + size() / 2.0 - bw / 2.0), by = (int) y() - 14;
        UI.bar(g, bx, by, bw, bh, frac, barColor(frac));
    }

    /** วงแหวนล้อมตัวไก่ใช้กับบัฟ/ออร่า/การเลือก */
    protected final void drawRing(Graphics2D g, Color ring, int margin) {
        g.setColor(ring);
        g.setStroke(new BasicStroke(3f));
        g.drawOval((int) x() - margin, (int) y() - margin, size() + margin * 2, size() + margin * 2);
        g.setStroke(new BasicStroke(1f));
    }
}
