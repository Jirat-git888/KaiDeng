import javax.swing.ImageIcon;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * สกิลของฮีโร่ (Strategy pattern): ฮีโร่ "มี" สกิลหนึ่งอัน แทนที่จะเขียนพฤติกรรมสกิลลงใน subclass ของไก่
 * คลาสนี้จัดการวงจรร่วม (คูลดาวน์, สถานะกำลังใช้งาน) แบบ template method
 * ส่วนสกิลแต่ละตัวเขียนเฉพาะของตัวเองผ่านฮุก onActivate / afterPhysics / interact / collide / draw
 */
abstract class Skill {
    private final String name;
    private final String description;
    private final int maxCooldown;
    private final Image icon;
    private int cooldown;
    private boolean active;

    protected Skill(String name, String description, int maxCooldown, String iconPath) {
        this.name = name;
        this.description = description;
        this.maxCooldown = maxCooldown;
        Image raw = new ImageIcon(iconPath).getImage();
        this.icon = raw.getWidth(null) > 0 ? UI.scaleFit(raw, 32) : null;   // 16x16 → 32x32 แบบพิกเซลคม
    }

    // ---------- ข้อมูลสำหรับแสดงผล ----------
    public final String name() { return name; }
    public final String description() { return description; }   // ขึ้นบรรทัดใหม่ด้วย \n
    public final int maxCooldown() { return maxCooldown; }
    public final int cooldown() { return cooldown; }
    public final boolean isActive() { return active; }
    public final boolean isReady() { return cooldown <= 0 && !active; }
    public final Image icon() { return icon; }               // null ถ้าไม่มีไฟล์ไอคอน

    // ---------- วงจรชีวิต ----------
    public final void activate(HeroChicken hero) {
        if (!isReady()) return;
        active = true;
        cooldown = maxCooldown;
        onActivate(hero);
    }

    /** เริ่มเทิร์นใหม่: คูลดาวน์ลดลง 1 และสกิลที่ใช้ค้างอยู่จบลง */
    public final void startTurn(HeroChicken hero) {
        if (cooldown > 0) cooldown--;
        deactivate(hero);
        onTurnStart(hero);
    }

    /** เริ่มการต่อสู้ใหม่: ล้างคูลดาวน์ทั้งหมด */
    public final void resetForBattle(HeroChicken hero) {
        cooldown = 0;
        deactivate(hero);
        onTurnStart(hero);
    }

    private void deactivate(HeroChicken hero) {
        if (active) {
            active = false;
            onDeactivate(hero);
        }
    }

    /** ให้สกิลจบการใช้งานทันทีโดยไม่เรียก onDeactivate (สกิลที่ "ยิงครั้งเดียว") */
    protected final void finish() { active = false; }

    // ---------- ฮุกให้สกิลแต่ละตัวทับ ----------
    protected void onActivate(HeroChicken hero) { }
    protected void onDeactivate(HeroChicken hero) { }
    protected void onTurnStart(HeroChicken hero) { }

    /** เรียกหลังฟิสิกส์ของฮีโร่ในแต่ละ tick */
    public void afterPhysics(HeroChicken hero, Arena arena, Effects fx) { }
    /** เรียกหลังฮีโร่ชนหิน/วาร์ปแล้ว (สำหรับวัตถุที่สกิลสร้างขึ้นมาเอง) */
    public void interact(Arena arena, Effects fx) { }
    /** เรียกหลังฮีโร่ชนบอส */
    public void collide(HeroChicken hero, BossChicken boss, Effects fx) { }
    /** สกิลยังมีวัตถุที่เคลื่อนไหวอยู่ (ทำให้เทิร์นยังไม่จบ) */
    public boolean isBusy() { return false; }
    public void draw(Graphics2D g, HeroChicken hero) { }

    // ================= สกิลที่มีให้ =================

    /** Tanky: ขยายร่างใหญ่และพลังโจมตีเป็นสองเท่าตลอดเทิร์น */
    static final class TitanForm extends Skill {
        TitanForm() {
            super("Titan form", "Grows huge and\nhits twice as\nhard this turn.", 4, "skill_tanky.png");
        }

        @Override protected void onActivate(HeroChicken hero) {
            hero.resizeAroundCenter(70);
            hero.scaleAttack(2);
        }

        @Override protected void onDeactivate(HeroChicken hero) {
            hero.resizeAroundCenter(30);
            hero.scaleAttack(0.5);
        }
    }

    /** Speedy: ตอนพุ่งจะแยกร่างเงาออกไป ±25 องศา ร่างเงาชนบอสได้ดาเมจ 75% */
    static final class ShadowSplit extends Skill {
        private static final double SPLIT_ANGLE = Math.toRadians(25);
        private static final double CLONE_DAMAGE = 0.75;

        private final List<ShadowClone> clones = new ArrayList<>();
        private boolean triggered;

        ShadowSplit() {
            super("Shadow split", "Splits into two\nclones that deal\n75% damage.", 3, "skill_speedy.png");
        }

        @Override protected void onActivate(HeroChicken hero) { triggered = true; }

        @Override protected void onTurnStart(HeroChicken hero) {
            triggered = false;
            clones.clear();
        }

        @Override public boolean isBusy() {
            for (ShadowClone c : clones) if (c.active) return true;
            return false;
        }

        @Override
        public void afterPhysics(HeroChicken hero, Arena arena, Effects fx) {
            if (isActive() && triggered && hero.isMoving()) {
                double speed = Math.hypot(hero.vx(), hero.vy());
                if (speed > 2.0) {
                    double angle = Math.atan2(hero.vy(), hero.vx());
                    clones.add(new ShadowClone(hero, speed, angle - SPLIT_ANGLE));
                    clones.add(new ShadowClone(hero, speed, angle + SPLIT_ANGLE));
                    triggered = false;
                    finish();                                     // สกิลยิงครั้งเดียว: จบทันทีหลังแยกร่าง
                }
            }
            Iterator<ShadowClone> it = clones.iterator();
            while (it.hasNext()) {
                ShadowClone c = it.next();
                c.update(arena);
                if (!c.active) it.remove();
            }
        }

        @Override
        public void interact(Arena arena, Effects fx) {
            for (ShadowClone c : clones) {
                if (!c.active) continue;
                c.bounceOffObstacles(arena, fx);
                c.warp(arena, fx);
            }
        }

        @Override
        public void collide(HeroChicken hero, BossChicken boss, Effects fx) {
            for (ShadowClone c : clones) {
                if (!c.active) continue;
                double dx = c.centerX() - boss.centerX();
                double dy = c.centerY() - boss.centerY();
                double dist = Math.hypot(dx, dy);
                double minDist = (c.size + boss.size()) / 2.0;
                if (dist >= minDist) continue;

                boss.takeDamage(Math.max(1, (int) (hero.getAttack() * CLONE_DAMAGE)), fx);
                fx.shake(8);
                fx.hitStop(3);
                fx.burst(boss.centerX(), boss.centerY(), Color.YELLOW, 10);

                double overlap = minDist - dist;
                if (overlap > 0 && dist > 0) {
                    c.x += dx / dist * overlap;
                    c.y += dy / dist * overlap;
                }
                double angle = Math.atan2(dy, dx), speed = Math.hypot(c.vx, c.vy);
                c.vx = speed * Math.cos(angle);
                c.vy = speed * Math.sin(angle);
                boss.setVelocity(boss.vx() - Math.cos(angle) * 2, boss.vy() - Math.sin(angle) * 2);
            }
        }

        @Override
        public void draw(Graphics2D g, HeroChicken hero) {
            for (ShadowClone c : clones) c.draw(g, hero);
        }

        /** ร่างเงา: วัตถุเล็กๆ ที่สกิลเป็นเจ้าของ (ไม่ใช่ Entity เพราะอยู่ภายใต้สกิลนี้เท่านั้น) */
        private static final class ShadowClone {
            double x, y, vx, vy;
            final int size;
            Arena.Portal lastWarped;
            boolean active = true;

            ShadowClone(HeroChicken hero, double speed, double angle) {
                this.x = hero.x();
                this.y = hero.y();
                this.size = hero.size();
                this.vx = speed * Math.cos(angle);
                this.vy = speed * Math.sin(angle);
            }

            double centerX() { return x + size / 2.0; }
            double centerY() { return y + size / 2.0; }

            void update(Arena arena) {
                x += vx;
                y += vy;
                vx *= 0.94;
                vy *= 0.94;
                if (Math.abs(vx) < 0.2) vx = 0;
                if (Math.abs(vy) < 0.2) vy = 0;
                if (vx == 0 && vy == 0) active = false;

                int left = arena.left(), top = arena.top(), right = left + arena.width(), bottom = top + arena.height();
                if (x <= left)            { x = left;            vx *= -1; }
                if (x + size >= right)    { x = right - size;    vx *= -1; }
                if (y <= top)             { y = top;             vy *= -1; }
                if (y + size >= bottom)   { y = bottom - size;   vy *= -1; }
            }

            void bounceOffObstacles(Arena arena, Effects fx) {
                for (Arena.Obstacle obs : arena.obstacles()) {
                    double dx = centerX() - obs.centerX();
                    double dy = centerY() - obs.centerY();
                    double dist = Math.hypot(dx, dy);
                    if (dist == 0) { dx = 1; dy = 0; dist = 1; }
                    double nx = dx / dist, ny = dy / dist;
                    double dot = vx * nx + vy * ny;
                    if (dot < 0) {
                        vx -= 2 * dot * nx;
                        vy -= 2 * dot * ny;
                        fx.burst(obs.centerX(), obs.centerY(), Color.YELLOW, 4);
                    }
                    double overlap = (size + obs.size()) / 2.0 - dist;
                    if (overlap > 0) {
                        x += nx * overlap;
                        y += ny * overlap;
                    }
                }
            }

            void warp(Arena arena, Effects fx) {
                if (lastWarped != null
                        && Math.hypot(centerX() - lastWarped.centerX(), centerY() - lastWarped.centerY()) >= (size + lastWarped.size()) / 2.0) {
                    lastWarped = null;
                }
                for (Arena.Portal p : arena.portals()) {
                    if (p == lastWarped || p.linked() == null) continue;
                    if (Math.hypot(centerX() - p.centerX(), centerY() - p.centerY()) < (size + p.size()) / 2.0) {
                        Arena.Portal exit = p.linked();
                        x = exit.x() + (exit.size() - size) / 2.0;
                        y = exit.y() + (exit.size() - size) / 2.0;
                        lastWarped = exit;
                        fx.burst(centerX(), centerY(), Color.CYAN, 10);
                        break;
                    }
                }
            }

            void draw(Graphics2D g, HeroChicken hero) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                Image img = hero.sprite(size);
                if (img != null) {
                    g.drawImage(img, (int) x, (int) y, null);
                } else {
                    g.setColor(hero.getColor());
                    g.fillOval((int) x, (int) y, size, size);
                }
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            }
        }
    }

    /** Balanced: ทิ้งระเบิดไข่ไว้ตามทางที่พุ่งผ่านทุกๆ 70 พิกเซล */
    static final class EggTrail extends Skill {
        private static final double MINE_SPACING = 70;
        private double lastX, lastY;

        EggTrail() {
            super("Egg trail", "Drops egg mines\nas it moves.\n25 damage each.", 3, "skill_balanced.png");
        }

        @Override protected void onActivate(HeroChicken hero) {
            lastX = hero.x();
            lastY = hero.y();
        }

        @Override
        public void afterPhysics(HeroChicken hero, Arena arena, Effects fx) {
            if (isActive() && hero.isMoving() && Math.hypot(hero.x() - lastX, hero.y() - lastY) > MINE_SPACING) {
                arena.addMine(new Arena.EggMine(hero.centerX() - 10, hero.centerY() - 10));
                lastX = hero.x();
                lastY = hero.y();
            }
        }
    }
}
