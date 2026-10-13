import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;

/** เร็ว: สกิลแยกร่างเงา 2 ร่างออกไปด้านข้างของเส้นทางพุ่ง */
class SpeedyChicken extends HeroChicken {

    /** ร่างเงา (ไม่ใช่ Entity เพราะเป็นแค่ตัวช่วยชั่วคราว) */
    class ShadowClone {
        double x, y, vx, vy;
        int size;
        Portal lastWarpedPortal = null;
        boolean active = true;

        public ShadowClone(double x, double y, double vx, double vy, int size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
        }

        double centerX() { return x + size / 2.0; }
        double centerY() { return y + size / 2.0; }

        public void update(Rectangle arena) {
            x += vx;
            y += vy;
            vx *= 0.94;
            vy *= 0.94;
            if (Math.abs(vx) < 0.2) vx = 0;
            if (Math.abs(vy) < 0.2) vy = 0;
            if (vx == 0 && vy == 0) active = false;

            if (x <= arena.x)                       { x = arena.x;                        vx *= -1; }
            if (x + size >= arena.x + arena.width)  { x = arena.x + arena.width - size;   vx *= -1; }
            if (y <= arena.y)                       { y = arena.y;                        vy *= -1; }
            if (y + size >= arena.y + arena.height) { y = arena.y + arena.height - size;  vy *= -1; }
        }

        public void draw(Graphics2D g2, Image img, Color c) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            if (img != null && img.getWidth(null) > 0) {
                g2.drawImage(img, (int) x, (int) y, size, size, null);
            } else {
                g2.setColor(c);
                g2.fillOval((int) x, (int) y, size, size);
            }
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }

    private final ArrayList<ShadowClone> clones = new ArrayList<>();
    private boolean skillTriggered = false;

    public SpeedyChicken() {
        super("Speedy", 100, 15, 6.0, new Color(255, 230, 50), "speedy.png", "skill_speedy.png", 3);
    }

    @Override public HeroChicken cloneHero() { return new SpeedyChicken(); }
    @Override public String role() { return "Skirmisher"; }
    @Override public String skillName() { return "Shadow split"; }
    @Override public String skillDesc() { return "Splits into two\nclones that deal\n75% damage."; }

    @Override
    public void activateSkill() {
        boolean wasActive = skillActive;
        super.activateSkill();
        if (skillActive && !wasActive) skillTriggered = true; // ตั้งธงเฉพาะตอนใช้สกิลสำเร็จจริง
    }

    @Override
    public void resetSkill() {
        super.resetSkill();
        skillTriggered = false;
        clones.clear();
    }

    @Override protected void applySkillEffect() { }
    @Override protected void removeSkillEffect() { }

    @Override
    public boolean isMoving() {
        if (super.isMoving()) return true;
        for (ShadowClone c : clones) if (c.active) return true;
        return false;
    }

    @Override
    public void updatePhysics(Rectangle arena, ArrayList<EggMine> mines) {
        super.updatePhysics(arena, mines);

        if (skillActive && skillTriggered && isMoving()) {
            double currentSpeed = Math.hypot(vx, vy);
            if (currentSpeed > 2.0) {
                double angle = Math.atan2(vy, vx);
                double a1 = angle - Math.toRadians(25), a2 = angle + Math.toRadians(25);
                clones.add(new ShadowClone(x, y, currentSpeed * Math.cos(a1), currentSpeed * Math.sin(a1), this.size));
                clones.add(new ShadowClone(x, y, currentSpeed * Math.cos(a2), currentSpeed * Math.sin(a2), this.size));
                skillTriggered = false;
                skillActive = false;
            }
        }

        Iterator<ShadowClone> iter = clones.iterator();
        while (iter.hasNext()) {
            ShadowClone c = iter.next();
            c.update(arena);
            if (!c.active) iter.remove();
        }
    }

    @Override
    public void checkCollisionWith(BossChicken boss, ArrayList<DamageText> texts) {
        super.checkCollisionWith(boss, texts);
        for (ShadowClone c : clones) {
            if (!c.active) continue;
            double dx = c.centerX() - boss.centerX();
            double dy = c.centerY() - boss.centerY();
            double dist = Math.hypot(dx, dy);
            double minDist = (c.size + boss.size) / 2.0;

            if (dist < minDist) {
                int dmg = Math.max(1, (int) (this.attack * 0.75));
                boss.takeDamage(dmg, texts);
                GameEngine.shakeTimer = 8;
                GameEngine.hitStopTimer = 3;
                GameEngine.spawnParticles(boss.centerX(), boss.centerY(), Color.YELLOW, 10);

                double overlap = minDist - dist;
                if (overlap > 0 && dist > 0) {
                    c.x += (dx / dist) * overlap;
                    c.y += (dy / dist) * overlap;
                }
                double angle = Math.atan2(dy, dx), currentSpeed = Math.hypot(c.vx, c.vy);
                c.vx = currentSpeed * Math.cos(angle);
                c.vy = currentSpeed * Math.sin(angle);
                boss.vx -= Math.cos(angle) * 2;
                boss.vy -= Math.sin(angle) * 2;
            }
        }
    }

    @Override
    public void handleObstacles(ArrayList<Obstacle> obstacles) {
        super.handleObstacles(obstacles);
        for (ShadowClone c : clones) {
            if (!c.active) continue;
            for (Obstacle obs : obstacles) {
                double dx = c.centerX() - obs.centerX();
                double dy = c.centerY() - obs.centerY();
                double dist = Math.hypot(dx, dy);
                if (dist == 0) { dx = 1; dy = 0; dist = 1; }
                double nx = dx / dist, ny = dy / dist;
                double dot = c.vx * nx + c.vy * ny;
                if (dot < 0) {
                    c.vx = c.vx - 2 * dot * nx;
                    c.vy = c.vy - 2 * dot * ny;
                    GameEngine.spawnParticles(obs.centerX(), obs.centerY(), Color.YELLOW, 4);
                }
                double overlap = ((c.size + obs.size) / 2.0) - dist;
                if (overlap > 0) {
                    c.x += nx * overlap;
                    c.y += ny * overlap;
                }
            }
        }
    }

    @Override
    public void handlePortals(ArrayList<Portal> portals) {
        super.handlePortals(portals);
        for (ShadowClone c : clones) {
            if (!c.active) continue;

            if (c.lastWarpedPortal != null) {
                double dx = c.centerX() - (c.lastWarpedPortal.x + c.lastWarpedPortal.size / 2.0);
                double dy = c.centerY() - (c.lastWarpedPortal.y + c.lastWarpedPortal.size / 2.0);
                if (Math.hypot(dx, dy) >= (c.size + c.lastWarpedPortal.size) / 2.0) {
                    c.lastWarpedPortal = null;
                }
            }

            for (Portal p : portals) {
                if (p == c.lastWarpedPortal) continue;

                double dx = c.centerX() - p.centerX();
                double dy = c.centerY() - p.centerY();
                if (Math.hypot(dx, dy) < (c.size + p.size) / 2.0 && p.linkedPortal != null) {
                    c.x = p.linkedPortal.x + (p.linkedPortal.size - c.size) / 2.0;
                    c.y = p.linkedPortal.y + (p.linkedPortal.size - c.size) / 2.0;
                    c.lastWarpedPortal = p.linkedPortal;
                    GameEngine.spawnParticles(c.centerX(), c.centerY(), Color.CYAN, 10);
                    break;
                }
            }
        }
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);
        Graphics2D g2 = (Graphics2D) g;
        for (ShadowClone c : clones) c.draw(g2, imageForSize(c.size), this.color);
    }
}
