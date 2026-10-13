import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/** ไก่ทุกตัว (ฮีโร่และบอส) มีฟิสิกส์ การชนขอบ/สิ่งกีดขวาง/หลุมวาร์ป และหลอด HP */
abstract class Chicken extends Entity {
    protected String name;
    protected int hp, maxHp, attack;
    protected double speed;
    protected int cooldown = 0;
    protected Portal lastWarpedPortal = null;
    protected Image img;
    protected ArrayList<TrailNode> trails = new ArrayList<>();
    private final HashMap<Integer, Image> scaledCache = new HashMap<>();

    public Chicken(String name, int hp, int attack, double speed, Color color, String imgPath) {
        this.name = name;
        this.hp = this.maxHp = hp;
        this.attack = attack;
        this.speed = speed;
        this.color = color;
        this.size = 30;
        this.img = new ImageIcon(imgPath).getImage();
    }

    /** ภาพสไปรต์ที่ใส่กล่อง s x s ไว้แล้ว (รักษาสัดส่วน เท้าชิดล่าง) คืน null ถ้าโหลดรูปไม่สำเร็จ */
    protected Image imageForSize(int s) {
        if (img == null || img.getWidth(null) <= 0) return null;
        return scaledCache.computeIfAbsent(s, k -> ImageUtil.scaleFit(img, k));
    }

    public boolean isAlive() { return hp > 0; }

    public boolean isMoving() { return Math.abs(vx) > 0.1 || Math.abs(vy) > 0.1; }

    public void spawnInArena(int startX, int startY) {
        this.x = startX;
        this.y = startY;
        this.vx = 0;
        this.vy = 0;
        trails.clear();
        lastWarpedPortal = null;
    }

    public void updatePhysics(Rectangle arena, ArrayList<EggMine> mines) {
        if (cooldown > 0) cooldown--;

        if (isMoving() && Math.hypot(vx, vy) > 4.0) trails.add(new TrailNode(x, y));

        x += vx;
        y += vy;
        vx *= 0.94;
        vy *= 0.94;
        if (Math.abs(vx) < 0.2) vx = 0;
        if (Math.abs(vy) < 0.2) vy = 0;

        if (x <= arena.x)                          { x = arena.x;                           vx *= -1; }
        if (x + size >= arena.x + arena.width)     { x = arena.x + arena.width - size;      vx *= -1; }
        if (y <= arena.y)                          { y = arena.y;                           vy *= -1; }
        if (y + size >= arena.y + arena.height)    { y = arena.y + arena.height - size;     vy *= -1; }

        Iterator<TrailNode> iter = trails.iterator();
        while (iter.hasNext()) {
            TrailNode t = iter.next();
            t.alpha -= 8;
            if (t.alpha <= 0) iter.remove();
        }
    }

    public void handleObstacles(ArrayList<Obstacle> obstacles) {
        for (Obstacle obs : obstacles) {
            if (!isCollidingWith(obs)) continue;

            double dx = centerX() - obs.centerX();
            double dy = centerY() - obs.centerY();
            double dist = Math.hypot(dx, dy);
            if (dist == 0) { dx = 1; dy = 0; dist = 1; }

            double nx = dx / dist, ny = dy / dist;
            double dot = vx * nx + vy * ny;
            if (dot < 0) {
                vx = vx - 2 * dot * nx;
                vy = vy - 2 * dot * ny;
                GameEngine.spawnParticles(obs.centerX(), obs.centerY(), Color.WHITE, 4);
                GameEngine.shakeTimer = 1;
            }
            double overlap = ((this.size + obs.size) / 2.0) - dist;
            if (overlap > 0) {
                this.x += nx * overlap;
                this.y += ny * overlap;
            }
        }
    }

    public void handlePortals(ArrayList<Portal> portals) {
        // ออกจากรัศมีหลุมที่เพิ่งโผล่แล้วค่อยปลดล็อก ไม่งั้นจะโดนดูดกลับทันที
        if (lastWarpedPortal != null && !isCollidingWith(lastWarpedPortal)) {
            lastWarpedPortal = null;
        }

        for (Portal p : portals) {
            if (p == lastWarpedPortal) continue;

            if (isCollidingWith(p) && p.linkedPortal != null) {
                this.x = p.linkedPortal.x + (p.linkedPortal.size - this.size) / 2.0;
                this.y = p.linkedPortal.y + (p.linkedPortal.size - this.size) / 2.0;
                this.lastWarpedPortal = p.linkedPortal;

                GameEngine.spawnParticles(centerX(), centerY(), Color.CYAN, 15);
                GameEngine.shakeTimer = 3;
                break;
            }
        }
    }

    public void takeDamage(int amount, ArrayList<DamageText> texts) {
        hp -= amount;
        texts.add(new DamageText((int) x + size / 2, (int) y, "-" + amount, Color.RED));
        cooldown = 30;
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        for (TrailNode t : trails) {
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), t.alpha));
            g2.fillOval((int) t.x, (int) t.y, size, size);
        }

        UI.shadowEllipse(g2, centerX(), y + size * 0.92, size * 0.85, Math.max(5, size * 0.3), 90);

        Image sprite = imageForSize(size);
        if (sprite != null) {
            g2.drawImage(sprite, (int) x, (int) y, null);
        } else {
            g2.setColor(UI.DARK);
            g2.fillOval((int) x - 2, (int) y - 2, size + 4, size + 4);
            g2.setColor(color);
            g2.fillOval((int) x, (int) y, size, size);
        }
        drawHpBar(g2);
    }

    /** สีหลอด HP เหนือหัว (ฮีโร่เขียว เหลือน้อยเป็นแดง) */
    protected Color barColor(double frac) { return frac < 0.3 ? UI.RED : UI.GREEN; }

    protected void drawHpBar(Graphics2D g2) {
        double frac = maxHp == 0 ? 0 : hp / (double) maxHp;
        int bw = Math.max(size, 36), bh = 8;
        int bx = (int) (x + size / 2.0 - bw / 2.0), by = (int) y - 14;
        UI.bar(g2, bx, by, bw, bh, frac, barColor(frac));
    }
}
