import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ศูนย์รวมเอฟเฟกต์ของการต่อสู้หนึ่งครั้ง: สะเก็ด ตัวเลขลอย จอสั่น และ hit-stop
 * (แทนตัวแปร static ส่วนกลางเดิม ทำให้แต่ละการต่อสู้มีสถานะเอฟเฟกต์ของตัวเอง)
 * สะเก็ดและตัวเลขลอยเป็นคลาสซ้อนในที่นี้ เพราะไม่มีใครใช้นอกจากคลาสนี้
 */
final class Effects {
    private final Random random = new Random();
    private final List<Particle> particles = new ArrayList<>();
    private final List<DamageText> texts = new ArrayList<>();
    private int shakeFrames, hitStopFrames;

    Random random() { return random; }

    void burst(double x, double y, Color color, int amount) {
        for (int i = 0; i < amount; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double speed = 2 + random.nextDouble() * 5;
            particles.add(new Particle(x, y, speed * Math.cos(angle), speed * Math.sin(angle), color, 15 + random.nextInt(15)));
        }
    }

    void floatText(int x, int y, String text, Color color) { texts.add(new DamageText(x, y, text, color)); }

    void shake(int frames) { shakeFrames = frames; }
    void hitStop(int frames) { hitStopFrames = frames; }

    /** ถ้ากำลังหยุดชั่วขณะ (hit-stop) ให้นับถอยหลังแล้วคืน true เพื่อให้ผู้เรียกข้ามการอัปเดตเฟรมนี้ */
    boolean consumeHitStop() {
        if (hitStopFrames > 0) { hitStopFrames--; return true; }
        return false;
    }

    boolean isShaking() { return shakeFrames > 0; }
    void decayShake() { if (shakeFrames > 0) shakeFrames--; }
    int shakeOffset() { return random.nextInt(8) - 4; }

    void updateParticles() { particles.removeIf(p -> { p.update(); return p.isDead(); }); }
    void updateTexts() { texts.removeIf(t -> { t.update(); return t.isDead(); }); }

    void drawParticles(Graphics2D g) { for (Particle p : particles) p.draw(g); }
    void drawTexts(Graphics2D g) { for (DamageText t : texts) t.draw(g); }

    /** เศษสะเก็ดสี่เหลี่ยมเล็กๆ ตอนชน */
    private static final class Particle {
        private double x, y;
        private final double vx, vy;
        private final int maxLife;
        private int life;
        private final Color color;

        Particle(double x, double y, double vx, double vy, Color color, int life) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.maxLife = life;
            this.life = life;
        }

        void update() { x += vx; y += vy; life--; }
        boolean isDead() { return life <= 0; }

        void draw(Graphics2D g) {
            if (life < maxLife / 3 && life % 2 == 0) return;   // กะพริบแทนการจาง
            g.setColor(color);
            g.fillRect((int) x, (int) y, 4, 4);
        }
    }

    /** ตัวเลขดาเมจ ขาวมีขอบน้ำตาล ลอยขึ้นแล้วกะพริบก่อนหาย */
    private static final class DamageText {
        private static final int LIFE = 60;
        private final int x;
        private int y;
        private final String text;
        private final Color color;
        private int life = LIFE;

        DamageText(int x, int y, String text, Color color) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
        }

        void update() { if (life % 2 == 0) y -= 1; life--; }
        boolean isDead() { return life <= 0; }

        void draw(Graphics2D g) {
            if (life < 14 && life % 4 < 2) return;   // กะพริบตอนใกล้หาย
            UI.centerOutlined(g, text, x, y, UI.HEAD, color, UI.DARK);
        }
    }
}
