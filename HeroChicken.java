import javax.swing.ImageIcon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.ArrayList;

/** ไก่ฝั่งผู้เล่น: มีสกิล, บัฟ, และการชนกับบอส */
abstract class HeroChicken extends Chicken {
    /** บัฟ SPEED_UP อยู่ได้กี่เทิร์น (นับตั้งแต่เก็บ) */
    private static final int SPEED_BUFF_TURNS = 3;
    private static final double SPEED_BUFF_AMOUNT = 2.0;

    protected boolean hasDoubleDamage = false;
    protected int skillCooldown = 0, maxSkillCooldown;
    protected Image skillIcon;
    protected boolean skillActive = false;
    protected int stuckTimer = 0;

    protected final double baseSpeed;
    protected int speedBuffTurns = 0;

    public HeroChicken(String name, int hp, int attack, double speed, Color color,
                       String imgPath, String skillImgPath, int maxCD) {
        super(name, hp, attack, speed, color, imgPath);
        this.baseSpeed = speed;
        this.maxSkillCooldown = maxCD;
        Image icon = new ImageIcon(skillImgPath).getImage();
        this.skillIcon = (icon.getWidth(null) > 0) ? ImageUtil.scaleFit(icon, 32) : icon;   // 16x16 → 32x32 แบบพิกเซลคม
    }

    public abstract HeroChicken cloneHero();

    /** ข้อความที่แสดงในหน้าเลือกตัวละครและ HUD */
    public abstract String role();
    public abstract String skillName();
    public abstract String skillDesc();   // ขึ้นบรรทัดใหม่ด้วย \n
    protected abstract void applySkillEffect();
    protected abstract void removeSkillEffect();

    public void activateSkill() {
        if (hp > 0 && skillCooldown <= 0 && !skillActive) {
            skillActive = true;
            skillCooldown = maxSkillCooldown;
            applySkillEffect();
        }
    }

    public void resetSkill() {
        if (skillActive) {
            skillActive = false;
            removeSkillEffect();
        }
    }

    /** บัฟความเร็วไม่สะสม: เก็บซ้ำแค่รีเซ็ตเวลา */
    public void applySpeedBuff() {
        speed = baseSpeed + SPEED_BUFF_AMOUNT;
        speedBuffTurns = SPEED_BUFF_TURNS;
    }

    /** เรียกตอนเริ่มเทิร์นผู้เล่นใหม่ */
    public void onTurnStart() {
        if (skillCooldown > 0) skillCooldown--;
        if (speedBuffTurns > 0) {
            speedBuffTurns--;
            if (speedBuffTurns == 0) speed = baseSpeed;
        }
        resetSkill();
    }

    public void followTarget(int targetX, int targetY) {
        double dx = targetX - x, dy = targetY - y;
        if (Math.sqrt(dx * dx + dy * dy) > 50) {
            x += dx * 0.05;
            y += dy * 0.05;
        }
    }

    public void checkCollisionWith(BossChicken boss, ArrayList<DamageText> texts) {
        double dx = centerX() - boss.centerX();
        double dy = centerY() - boss.centerY();
        double dist = Math.hypot(dx, dy);
        double minDist = (this.size + boss.size) / 2.0;
        if (dist == 0) { dx = 1; dy = 0; dist = 1; } // กันหารศูนย์

        if (dist <= minDist + 2.0) {
            stuckTimer++;
            if (stuckTimer >= 90) {
                double pushForce = 12.0;
                this.vx = (dx / dist) * pushForce;
                this.vy = (dy / dist) * pushForce;
                boss.vx = -(dx / dist) * pushForce;
                boss.vy = -(dy / dist) * pushForce;
                stuckTimer = 0;
                return;
            }
        } else {
            stuckTimer = 0;
        }

        if (dist < minDist) {
            if (this.cooldown <= 0 && boss.cooldown <= 0) {
                int dmg = this.attack;
                if (this.hasDoubleDamage) { dmg *= 2; this.hasDoubleDamage = false; }
                boss.takeDamage(dmg, texts);
                this.takeDamage(boss.attack, texts);

                GameEngine.shakeTimer = 15;
                GameEngine.hitStopTimer = 5;
                GameEngine.spawnParticles(centerX(), centerY(), this.color, 15);
                GameEngine.spawnParticles(boss.centerX(), boss.centerY(), boss.color, 15);

                handleBossCollision(boss);
            }
            double overlap = minDist - dist;
            if (overlap > 0) {
                this.x += (dx / dist) * overlap / 2.0;
                this.y += (dy / dist) * overlap / 2.0;
                boss.x -= (dx / dist) * overlap / 2.0;
                boss.y -= (dy / dist) * overlap / 2.0;
            }
        }
    }

    protected void handleBossCollision(BossChicken boss) {
        double tempVx = this.vx, tempVy = this.vy;
        this.vx = boss.vx;
        this.vy = boss.vy;
        boss.vx = tempVx;
        boss.vy = tempVy;
    }

    @Override
    public void draw(Graphics g) {
        if (hasDoubleDamage) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(UI.RED);
            g2.setStroke(new BasicStroke(3f));
            g2.drawOval((int) x - 5, (int) y - 5, size + 10, size + 10);
            g2.setStroke(new BasicStroke(1f));
        }
        super.draw(g);
    }
}
