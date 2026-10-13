import java.awt.*;

/** ตัวเลขดาเมจ ขาวมีขอบน้ำตาล ลอยขึ้นแล้วกะพริบก่อนหาย */
class DamageText {
    private static final int LIFE = 60;
    int x, y;
    String text;
    Color color;
    int life = LIFE;

    public DamageText(int x, int y, String text, Color color) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = color;
    }

    public void update() {
        if (life % 2 == 0) y -= 1;
        life--;
    }

    public boolean isDead() { return life <= 0; }

    public void draw(Graphics g) {
        if (life < 14 && life % 4 < 2) return;   // กะพริบตอนใกล้หาย
        PixelText.centerOutlined((Graphics2D) g, text, x, y, PixelText.HEAD, color, UI.DARK);
    }
}
