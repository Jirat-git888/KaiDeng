import java.awt.*;
import java.util.ArrayList;

class BuffItem extends Entity {
    BuffType type;

    public BuffItem(int x, int y) {
        this.x = x;
        this.y = y;
        this.size = 22;
        BuffType[] all = BuffType.values();
        this.type = all[GameEngine.random.nextInt(all.length)];
        this.color = colorOf(type);
    }

    static Color colorOf(BuffType t) {
        switch (t) {
            case HEAL:      return UI.GREEN;
            case DAMAGE_X2: return UI.RED;
            default:        return UI.BLUE;
        }
    }

    static String symbolOf(BuffType t) {
        switch (t) {
            case HEAL:      return "+";
            case DAMAGE_X2: return "x2";
            default:        return ">>";
        }
    }

    public void applyBuffTo(HeroChicken c, ArrayList<DamageText> texts) {
        switch (type) {
            case HEAL:
                c.hp = Math.min(c.hp + 50, c.maxHp);
                texts.add(new DamageText(c.getX(), c.getY(), "+50", UI.GREEN));
                break;
            case DAMAGE_X2:
                c.hasDoubleDamage = true;
                texts.add(new DamageText(c.getX(), c.getY(), "x2 DMG", UI.YELLOW));
                break;
            case SPEED_UP:
                c.applySpeedBuff();
                texts.add(new DamageText(c.getX(), c.getY(), "SPEED UP", UI.BLUE));
                break;
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int bob = (GameEngine.tick / 20 + (int) x) % 2 == 0 ? 0 : -2;   // ขยับขึ้นลง 2 พิกเซลช้าๆ
        int px = (int) x, py = (int) y + bob;
        UI.shadowEllipse(g2, centerX(), y + size + 2, size * 0.8, 6, 90);
        g2.setColor(UI.DARK);
        g2.fillOval(px - 2, py - 2, size + 4, size + 4);
        g2.setColor(color);
        g2.fillOval(px, py, size, size);
        PixelText.center(g2, symbolOf(type), px + size / 2, py + size / 2 + 3, PixelText.TINY, UI.CREAM);
    }
}
