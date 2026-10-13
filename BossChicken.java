import java.awt.Color;
import java.util.ArrayList;

class BossChicken extends Chicken {
    public boolean enraged = false;

    public BossChicken(int x, int y) {
        super("Void Boss", 400, 15, 5.0, new Color(180, 0, 180), "boss.png");
        this.size = 40;
        this.x = x;
        this.y = y;
    }

    @Override
    protected Color barColor(double frac) { return UI.RED; }

    @Override
    public void takeDamage(int amount, ArrayList<DamageText> texts) {
        super.takeDamage(amount, texts);
        // เข้าโหมดโกรธเมื่อ HP ต่ำกว่า 40%
        if (!enraged && hp <= maxHp * 0.4 && hp > 0) {
            enraged = true;
            this.x -= 7.5;  // ขยายจากจุดกึ่งกลาง ไม่ให้ตัวเลื่อนไปทางขวาล่าง
            this.y -= 7.5;
            this.size += 15;
            this.attack += 5;
            this.color = new Color(220, 20, 20);
            texts.add(new DamageText((int) x, (int) y - 20, "ENRAGED!", Color.RED));
            GameEngine.spawnParticles(centerX(), centerY(), UI.RED, 40);
        }
    }
}
