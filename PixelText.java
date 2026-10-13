import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;

/**
 * ฟอนต์พิกเซลบิตแมป 5x7 ที่เขียนเองในโค้ด (ไม่ต้องมีไฟล์ฟอนต์ และหน้าตาเหมือนกันทุกเครื่อง)
 * ตัวอักษรถูกวาดเป็นภาพ 1 พิกเซลต่อ 1 จุด แล้วขยายด้วยตัวคูณ (scale) แบบ nearest-neighbor
 * ภาพของแต่ละข้อความถูกแคชไว้ จึงวาดซ้ำทุกเฟรมได้ถูก
 *
 * ตัวอักษรที่รองรับ: A-Z a-z 0-9 และ . , ! ? + - / % > :
 */
final class PixelText {
    private PixelText() { }

    // ขนาด = ตัวคูณการขยาย (ตัวพิมพ์ใหญ่สูง 7 จุด x ตัวคูณ พิกเซล)
    static final int TINY = 1, SMALL = 2, BODY = 2, HEAD = 3, BIG = 4, TITLE = 6;

    private static final int ROWS = 9, ABOVE = 7, PAD = 1;   // 9 แถว: 7 แถวเหนือเส้นฐาน + 2 แถวหางตัวอักษร

    private static final class Glyph {
        boolean[][] px = new boolean[ROWS][];
        int minC, maxC, adv;
    }

    private static final HashMap<Character, Glyph> GLYPHS = new HashMap<>();
    private static final HashMap<String, BufferedImage> CACHE = new HashMap<>();

    /** rows คั่นด้วย '/' แถวที่ขาดถือว่าว่าง ('#' = จุดที่ติด) */
    private static void def(char ch, String rows) {
        String[] r = rows.split("/", -1);
        int width = 0;
        for (String s : r) width = Math.max(width, s.length());
        Glyph g = new Glyph();
        g.minC = Integer.MAX_VALUE;
        g.maxC = -1;
        for (int y = 0; y < ROWS; y++) {
            g.px[y] = new boolean[width];
            if (y >= r.length) continue;
            for (int x = 0; x < r[y].length(); x++) {
                if (r[y].charAt(x) == '#') {
                    g.px[y][x] = true;
                    g.minC = Math.min(g.minC, x);
                    g.maxC = Math.max(g.maxC, x);
                }
            }
        }
        if (g.maxC < 0) { g.minC = 0; g.maxC = 1; }            // ช่องว่าง
        g.adv = g.maxC - g.minC + 2;                           // ความกว้างตัวอักษร + เว้น 1 จุด
        GLYPHS.put(ch, g);
    }

    static {
        def(' ', "");
        // ---- ตัวพิมพ์ใหญ่ ----
        def('A', ".###./#...#/#...#/#####/#...#/#...#/#...#");
        def('B', "####./#...#/#...#/####./#...#/#...#/####.");
        def('C', ".###./#...#/#..../#..../#..../#...#/.###.");
        def('D', "####./#...#/#...#/#...#/#...#/#...#/####.");
        def('E', "#####/#..../#..../####./#..../#..../#####");
        def('F', "#####/#..../#..../####./#..../#..../#....");
        def('G', ".###./#...#/#..../#.###/#...#/#...#/.###.");
        def('H', "#...#/#...#/#...#/#####/#...#/#...#/#...#");
        def('I', "#####/..#../..#../..#../..#../..#../#####");
        def('J', "..###/...#./...#./...#./...#./#..#./.##..");
        def('K', "#...#/#..#./#.#../##.../#.#../#..#./#...#");
        def('L', "#..../#..../#..../#..../#..../#..../#####");
        def('M', "#...#/##.##/#.#.#/#.#.#/#...#/#...#/#...#");
        def('N', "#...#/##..#/#.#.#/#..##/#...#/#...#/#...#");
        def('O', ".###./#...#/#...#/#...#/#...#/#...#/.###.");
        def('P', "####./#...#/#...#/####./#..../#..../#....");
        def('Q', ".###./#...#/#...#/#...#/#.#.#/#..#./.##.#");
        def('R', "####./#...#/#...#/####./#.#../#..#./#...#");
        def('S', ".####/#..../#..../.###./....#/....#/####.");
        def('T', "#####/..#../..#../..#../..#../..#../..#..");
        def('U', "#...#/#...#/#...#/#...#/#...#/#...#/.###.");
        def('V', "#...#/#...#/#...#/#...#/#...#/.#.#./..#..");
        def('W', "#...#/#...#/#...#/#.#.#/#.#.#/##.##/#...#");
        def('X', "#...#/#...#/.#.#./..#../.#.#./#...#/#...#");
        def('Y', "#...#/#...#/.#.#./..#../..#../..#../..#..");
        def('Z', "#####/....#/...#./..#../.#.../#..../#####");
        // ---- ตัวพิมพ์เล็ก ----
        def('a', "...../...../.###./....#/.####/#...#/.####");
        def('b', "#..../#..../####./#...#/#...#/#...#/####.");
        def('c', "...../...../.###./#...#/#..../#...#/.###.");
        def('d', "....#/....#/.####/#...#/#...#/#...#/.####");
        def('e', "...../...../.###./#...#/#####/#..../.###.");
        def('f', "..##./.#..#/.#.../###../.#.../.#.../.#...");
        def('g', "...../...../.####/#...#/#...#/#...#/.####/....#/.###.");
        def('h', "#..../#..../####./#...#/#...#/#...#/#...#");
        def('i', "..#../...../.##../..#../..#../..#../.###.");
        def('j', "...#./...../..##./...#./...#./...#./...#./#..#./.##..");
        def('k', "#..../#..../#..#./#.#../##.../#.#../#..#.");
        def('l', ".##../..#../..#../..#../..#../..#../.###.");
        def('m', "...../...../##.#./#.#.#/#.#.#/#.#.#/#.#.#");
        def('n', "...../...../####./#...#/#...#/#...#/#...#");
        def('o', "...../...../.###./#...#/#...#/#...#/.###.");
        def('p', "...../...../####./#...#/#...#/#...#/####./#..../#....");
        def('q', "...../...../.####/#...#/#...#/#...#/.####/....#/....#");
        def('r', "...../...../#.##./##..#/#..../#..../#....");
        def('s', "...../...../.####/#..../.###./....#/####.");
        def('t', "...../.#.../###../.#.../.#.../.#..#/..##.");
        def('u', "...../...../#...#/#...#/#...#/#..##/.##.#");
        def('v', "...../...../#...#/#...#/#...#/.#.#./..#..");
        def('w', "...../...../#...#/#.#.#/#.#.#/#.#.#/.#.#.");
        def('x', "...../...../#...#/.#.#./..#../.#.#./#...#");
        def('y', "...../...../#...#/#...#/#...#/.####/....#/#...#/.###.");
        def('z', "...../...../#####/...#./..#../.#.../#####");
        // ---- ตัวเลข ----
        def('0', ".###./#...#/#..##/#.#.#/##..#/#...#/.###.");
        def('1', "..#../.##../..#../..#../..#../..#../.###.");
        def('2', ".###./#...#/....#/...#./..#../.#.../#####");
        def('3', "####./....#/....#/.###./....#/....#/####.");
        def('4', "...#./..##./.#.#./#..#./#####/...#./...#.");
        def('5', "#####/#..../####./....#/....#/#...#/.###.");
        def('6', ".###./#..../#..../####./#...#/#...#/.###.");
        def('7', "#####/....#/...#./..#../.#.../.#.../.#...");
        def('8', ".###./#...#/#...#/.###./#...#/#...#/.###.");
        def('9', ".###./#...#/#...#/.####/....#/....#/.###.");
        // ---- เครื่องหมาย ----
        def('.', "././././././#");
        def(',', "././././././#/#");
        def('!', "#/#/#/#/#/./#");
        def('?', ".###./#...#/....#/..##./..#../...../..#..");
        def('+', "...../..#../..#../#####/..#../..#../.....");
        def('-', "...../...../...../#####");
        def('/', "....#/....#/...#./..#../.#.../#..../#....");
        def('%', "##..#/##..#/...#./..#../.#.../#..##/#..##");
        def('>', "..../#.../.#../..#./.#../#.../....");
        def(':', "././#/././#/.");
    }

    private static Glyph glyph(char c) {
        Glyph g = GLYPHS.get(c);
        return g != null ? g : GLYPHS.get('?');
    }

    /** ความกว้างของข้อความเป็นจำนวน "จุด" (ยังไม่คูณขนาด) */
    private static int cells(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) w += glyph(s.charAt(i)).adv;
        return Math.max(0, w - 1);
    }

    static int width(String s, int scale) { return cells(s) * scale; }

    private static BufferedImage image(String s, Color c, Color outline) {
        String key = c.getRGB() + "|" + (outline == null ? 0 : outline.getRGB()) + "|" + s;
        BufferedImage img = CACHE.get(key);
        if (img != null) return img;
        if (CACHE.size() > 800) CACHE.clear();      // กันแคชโตไม่จำกัด

        int w = cells(s) + PAD * 2, h = ROWS + PAD * 2;
        img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] outlineRgb = outline == null ? null : new int[]{outline.getRGB()};

        for (int pass = 0; pass < 2; pass++) {       // รอบ 0 = ขอบ, รอบ 1 = ตัวอักษร
            if (pass == 0 && outlineRgb == null) continue;
            int x0 = PAD;
            for (int i = 0; i < s.length(); i++) {
                Glyph g = glyph(s.charAt(i));
                for (int y = 0; y < ROWS; y++) {
                    for (int x = g.minC; x <= g.maxC; x++) {
                        if (x >= g.px[y].length || !g.px[y][x]) continue;
                        int ix = x0 + x - g.minC, iy = PAD + y;
                        if (pass == 0) {
                            for (int dx = -1; dx <= 1; dx++) {
                                for (int dy = -1; dy <= 1; dy++) {
                                    int nx = ix + dx, ny = iy + dy;
                                    if (nx >= 0 && ny >= 0 && nx < w && ny < h) img.setRGB(nx, ny, outlineRgb[0]);
                                }
                            }
                        } else {
                            img.setRGB(ix, iy, c.getRGB());
                        }
                    }
                }
                x0 += g.adv;
            }
        }
        CACHE.put(key, img);
        return img;
    }

    /** x = ขอบซ้ายของข้อความ, y = เส้นฐานตัวอักษร (ขอบล่างของตัวพิมพ์ใหญ่) */
    static void draw(Graphics2D g, String s, int x, int y, int scale, Color c) {
        drawOutlined(g, s, x, y, scale, c, null);
    }

    static void drawOutlined(Graphics2D g, String s, int x, int y, int scale, Color c, Color outline) {
        BufferedImage img = image(s, c, outline);
        g.drawImage(img, x - PAD * scale, y - (PAD + ABOVE) * scale, img.getWidth() * scale, img.getHeight() * scale, null);
    }

    static void center(Graphics2D g, String s, int cx, int y, int scale, Color c) {
        draw(g, s, cx - width(s, scale) / 2, y, scale, c);
    }

    static void centerOutlined(Graphics2D g, String s, int cx, int y, int scale, Color c, Color outline) {
        drawOutlined(g, s, cx - width(s, scale) / 2, y, scale, c, outline);
    }

    static void right(Graphics2D g, String s, int right, int y, int scale, Color c) {
        draw(g, s, right - width(s, scale), y, scale, c);
    }

    static void rightOutlined(Graphics2D g, String s, int right, int y, int scale, Color c, Color outline) {
        drawOutlined(g, s, right - width(s, scale), y, scale, c, outline);
    }
}
