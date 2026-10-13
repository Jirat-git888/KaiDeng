import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** ย่อ/ขยายรูปครั้งเดียวไว้ล่วงหน้า แทนที่จะย่อรูปใหญ่ๆ ใหม่ทุกเฟรม */
final class ImageUtil {
    private ImageUtil() { }

    private static BufferedImage toArgb(Image src, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return img;
    }

    /** ย่อ/ขยายรูปเป็น w x h คุณภาพดี (ย่อทีละครึ่งเพื่อไม่ให้ภาพแตก) คืนรูปเดิมถ้าโหลดไม่สำเร็จ */
    static Image scale(Image src, int w, int h) {
        int sw = src.getWidth(null), sh = src.getHeight(null);
        if (sw <= 0 || sh <= 0 || w <= 0 || h <= 0) return src;

        BufferedImage cur = toArgb(src, sw, sh);
        int cw = sw, ch = sh;
        while (cw / 2 >= w && ch / 2 >= h) {
            cw /= 2;
            ch /= 2;
            cur = resize(cur, cw, ch, true);
        }
        return resize(cur, w, h, true);
    }

    /**
     * ใส่รูปลงกล่อง box x box แบบรักษาสัดส่วน:
     *  - ตัดขอบโปร่งใสรอบตัวละครทิ้ง ทุกตัวจึงเต็มกล่องพอๆ กัน ไม่ลอยสูงต่ำต่างกัน
     *  - วางกึ่งกลางแนวนอน เท้าชิดขอบล่างเสมอ (ตรงกับเงาใต้ตัว)
     *  - ถ้าต้องขยาย (พิกเซลอาร์ตเล็ก) จะขยายเป็นจำนวนเท่าเต็มแบบ nearest-neighbor ให้พิกเซลคมเท่ากันทุกจุด
     *  - ถ้าต้องย่อ จะย่อแบบนุ่ม
     * คืนรูปขนาด box x box เสมอ จึงเอาไปวาดแทนรูปเดิมได้ทันที
     */
    static Image scaleFit(Image src, int box) {
        int sw = src.getWidth(null), sh = src.getHeight(null);
        if (sw <= 0 || sh <= 0 || box <= 0) return src;

        BufferedImage full = toArgb(src, sw, sh);
        Rectangle b = opaqueBounds(full);
        BufferedImage cropped = full.getSubimage(b.x, b.y, b.width, b.height);

        double f = Math.min((double) box / b.width, (double) box / b.height);
        BufferedImage scaled;
        if (f >= 1) {
            int k = (int) Math.floor(f);                       // ขยายเป็นจำนวนเท่าเต็ม พิกเซลคม
            scaled = resize(cropped, b.width * k, b.height * k, false);
        } else {
            int tw = Math.max(1, (int) Math.round(b.width * f));
            int th = Math.max(1, (int) Math.round(b.height * f));
            scaled = (BufferedImage) scale(cropped, tw, th);
        }

        BufferedImage out = new BufferedImage(box, box, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(scaled, (box - scaled.getWidth()) / 2, box - scaled.getHeight(), null);
        g.dispose();
        return out;
    }

    /** กรอบสี่เหลี่ยมเล็กสุดที่ครอบพิกเซลที่ไม่โปร่งใส (ถ้าทั้งรูปทึบจะได้ทั้งรูป) */
    private static Rectangle opaqueBounds(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((img.getRGB(x, y) >>> 24) > 20) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < 0) return new Rectangle(0, 0, w, h);
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static BufferedImage resize(BufferedImage src, int w, int h, boolean smooth) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, smooth
                ? RenderingHints.VALUE_INTERPOLATION_BILINEAR
                : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }
}
