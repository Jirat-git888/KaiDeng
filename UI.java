import java.awt.*;

/**
 * พาเลตต์และตัวช่วยวาดสไตล์ Stardew Valley: กรอบไม้ แผ่นกระดาษครีม ขอบหยักแบบพิกเซล
 * ไม่มีแสงเรือง ไม่มีไล่สี ทุกอย่างเป็นสีเรียบและขอบคม
 */
final class UI {
    private UI() { }

    static final Color DARK        = new Color(86, 40, 20);      // ขอบ/ตัวอักษรน้ำตาลเข้ม
    static final Color WOOD        = new Color(178, 104, 50);
    static final Color WOOD_LIGHT  = new Color(218, 152, 78);
    static final Color PARCH       = new Color(252, 226, 166);   // กระดาษครีม
    static final Color PARCH_DARK  = new Color(232, 194, 126);
    static final Color CREAM       = new Color(255, 246, 214);

    static final Color GRASS       = new Color(104, 170, 64);
    static final Color GRASS_DARK  = new Color(88, 150, 52);
    static final Color GRASS_LIGHT = new Color(128, 192, 82);
    static final Color DIRT        = new Color(200, 152, 100);
    static final Color DIRT_DARK   = new Color(178, 130, 82);
    static final Color SKY         = new Color(132, 198, 240);

    static final Color RED    = new Color(214, 66, 52);
    static final Color GREEN  = new Color(72, 198, 80);
    static final Color YELLOW = new Color(250, 202, 58);
    static final Color BLUE   = new Color(84, 150, 216);
    static final Color PURPLE = new Color(188, 100, 204);
    static final Color GREY   = new Color(150, 140, 130);

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    static Color lerp(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static Color brighter(Color c, double t) { return lerp(c, Color.WHITE, t); }
    static Color darker(Color c, double t)   { return lerp(c, Color.BLACK, t); }

    /** สี่เหลี่ยมมุมหยัก (ตัดมุม n พิกเซล) */
    static void notch(Graphics2D g, int x, int y, int w, int h, int n, Color c) {
        g.setColor(c);
        g.fillRect(x + n, y, w - 2 * n, h);
        g.fillRect(x, y + n, w, h - 2 * n);
    }

    /** แผงเต็ม: ขอบเข้ม + กรอบไม้ + กระดาษครีมด้านใน */
    static void panel(Graphics2D g, int x, int y, int w, int h) {
        notch(g, x, y, w, h, 4, DARK);
        notch(g, x + 2, y + 2, w - 4, h - 4, 2, WOOD);
        g.setColor(WOOD_LIGHT);
        g.fillRect(x + 4, y + 2, w - 8, 2);
        notch(g, x + 6, y + 6, w - 12, h - 12, 2, DARK);
        notch(g, x + 8, y + 8, w - 16, h - 16, 0, PARCH);
    }

    /** ป้ายเล็ก: ขอบเข้มบางๆ + กระดาษครีม */
    static void plate(Graphics2D g, int x, int y, int w, int h) {
        notch(g, x, y, w, h, 2, DARK);
        notch(g, x + 2, y + 2, w - 4, h - 4, 0, PARCH);
        g.setColor(PARCH_DARK);
        g.fillRect(x + 2, y + h - 4, w - 4, 2);
    }

    /** หลอดค่า: กรอบเข้ม พื้นน้ำตาลเข้ม ส่วนเติมสีเรียบ + แถบสว่างด้านบน */
    static void bar(Graphics2D g, int x, int y, int w, int h, double frac, Color fill) {
        frac = Math.max(0, Math.min(1, frac));
        g.setColor(DARK);
        g.fillRect(x, y, w, h);
        g.setColor(new Color(74, 40, 28));
        g.fillRect(x + 2, y + 2, w - 4, h - 4);
        int fw = (int) Math.round((w - 4) * frac);
        if (fw > 0) {
            g.setColor(fill);
            g.fillRect(x + 2, y + 2, fw, h - 4);
            g.setColor(brighter(fill, 0.35));
            g.fillRect(x + 2, y + 2, fw, Math.max(1, (h - 4) / 3));
        }
    }

    static void button(Graphics2D g, Rectangle r, String label, boolean hover) {
        int x = r.x, y = r.y, w = r.width, h = r.height;
        notch(g, x, y, w, h, 4, hover ? new Color(255, 214, 64) : DARK);
        notch(g, x + 2, y + 2, w - 4, h - 4, 2, DARK);
        notch(g, x + 4, y + 4, w - 8, h - 8, 2, hover ? new Color(255, 240, 190) : PARCH);
        g.setColor(hover ? PARCH : PARCH_DARK);
        g.fillRect(x + 6, y + h - 8, w - 12, 4);
        PixelText.center(g, label, x + w / 2, y + h / 2 + 10, PixelText.HEAD, DARK);
    }

    /**
     * ช่องสกิลแบบช่องเก็บของ: แสดงไอคอนสกิล มีป้ายปุ่มลัดเล็กๆ ที่มุมซ้ายบน
     * state 0 = พร้อมใช้ (ขอบเหลือง), 1 = กำลังใช้งาน (พื้นเขียว), 2 = รอคูลดาวน์ (มืดลง + เลขเทิร์นที่เหลือ)
     * ถ้าไม่มีไฟล์ไอคอนจะแสดงตัวอักษรปุ่มแทน
     */
    static void skillTile(Graphics2D g, int x, int y, int size, Image icon, String key, int state, int cooldown) {
        notch(g, x, y, size, size, 4, DARK);
        int inset = 2;
        if (state == 0) {
            notch(g, x + 2, y + 2, size - 4, size - 4, 2, YELLOW);
            inset = 4;
        }
        notch(g, x + inset, y + inset, size - 2 * inset, size - 2 * inset, 0,
                state == 1 ? new Color(160, 214, 110) : PARCH);

        boolean hasIcon = icon != null && icon.getWidth(null) > 0;
        if (hasIcon) {
            g.drawImage(icon, x + (size - icon.getWidth(null)) / 2, y + (size - icon.getHeight(null)) / 2, null);
        } else {
            PixelText.center(g, key, x + size / 2, y + size / 2 + 7, PixelText.BODY, DARK);
        }

        if (state == 2) {
            g.setColor(new Color(60, 30, 20, 150));
            g.fillRect(x + 2, y + 2, size - 4, size - 4);
            PixelText.centerOutlined(g, String.valueOf(cooldown), x + size / 2, y + size / 2 + 10, PixelText.HEAD, CREAM, DARK);
        }

        if (hasIcon) {                                          // ป้ายปุ่มลัดที่มุมซ้ายบน
            int bx = x - 4, by = y - 4;
            g.setColor(DARK);
            g.fillRect(bx, by, 13, 13);
            g.setColor(CREAM);
            g.fillRect(bx + 1, by + 1, 11, 11);
            PixelText.center(g, key, bx + 6, by + 10, PixelText.TINY, DARK);
        }
    }

    static void shadowEllipse(Graphics2D g, double cx, double cy, double w, double h, int alpha) {
        g.setColor(new Color(40, 20, 10, alpha));
        g.fillOval((int) (cx - w / 2), (int) (cy - h / 2), (int) w, (int) h);
    }
}
