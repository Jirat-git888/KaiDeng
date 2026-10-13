import java.awt.Color;
import java.awt.Graphics2D;

/** เศษสะเก็ดสี่เหลี่ยมเล็กๆ ตอนชน */
class Particle {
    double x, y, vx, vy;
    int life, maxLife, size;
    Color color;

    public Particle(double x, double y, double vx, double vy, Color color) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.color = color;
        this.maxLife = 15 + GameEngine.random.nextInt(15);
        this.life = this.maxLife;
        this.size = 4;
    }

    public void update() {
        x += vx;
        y += vy;
        life--;
    }

    public void draw(Graphics2D g2) {
        if (life < maxLife / 3 && life % 2 == 0) return;   // กะพริบแทนการจาง
        g2.setColor(color);
        g2.fillRect((int) x, (int) y, size, size);
    }
}
