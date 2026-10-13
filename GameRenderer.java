import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * วาดทุกฉากแบบเรียบ สไตล์ Stardew Valley: พื้นหญ้า กรอบไม้ แผ่นกระดาษครีม ตัวอักษรพิกเซล
 * พื้นหลังวาดไว้ล่วงหน้าครั้งเดียว ที่เหลือเป็นสี่เหลี่ยมสีเรียบ จึงเบามาก
 */
class GameRenderer {
    private static final int FENCE = 10;   // ความหนารั้วไม้รอบสนามที่ยื่นออกมาจากขอบสนามจริง

    private final GameEngine e;
    private final int W, H;
    private final BufferedImage grassBg, menuBg, arenaLayer;

    GameRenderer(GameEngine engine) {
        this.e = engine;
        this.W = e.width;
        this.H = e.height;
        grassBg = makeGrass();
        menuBg = makeMenuBg();
        arenaLayer = makeArenaLayer();
    }

    // ==========================================================
    //  ภาพพื้นหลังที่วาดล่วงหน้า
    // ==========================================================
    private BufferedImage rgb() { return new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB); }

    private void paintGrass(Graphics2D g, int y0, int y1, long seed) {
        g.setColor(UI.GRASS);
        g.fillRect(0, y0, W, y1 - y0);
        Random r = new Random(seed);
        for (int i = 0; i < 260; i++) {                         // กอหญ้าสี่เหลี่ยมเล็กๆ
            int x = r.nextInt(W / 4) * 4, y = y0 + r.nextInt(Math.max(1, (y1 - y0) / 4)) * 4;
            g.setColor(r.nextBoolean() ? UI.GRASS_DARK : UI.GRASS_LIGHT);
            g.fillRect(x, y, 4, 4);
            if (r.nextInt(3) == 0) g.fillRect(x + 4, y + 4, 4, 4);
        }
        Color[] petals = {new Color(255, 255, 255), new Color(255, 170, 190), UI.YELLOW};
        for (int i = 0; i < 16; i++) {                          // ดอกไม้
            int x = r.nextInt(W / 4) * 4, y = y0 + r.nextInt(Math.max(1, (y1 - y0) / 4)) * 4;
            g.setColor(petals[r.nextInt(petals.length)]);
            g.fillRect(x, y, 6, 6);
            g.setColor(UI.YELLOW);
            g.fillRect(x + 2, y + 2, 2, 2);
        }
    }

    private BufferedImage makeGrass() {
        BufferedImage img = rgb();
        Graphics2D g = img.createGraphics();
        paintGrass(g, 0, H, 3);
        g.dispose();
        return img;
    }

    private BufferedImage makeMenuBg() {
        BufferedImage img = rgb();
        Graphics2D g = img.createGraphics();
        g.setColor(UI.SKY);
        g.fillRect(0, 0, W, H);
        g.setColor(UI.GRASS_DARK);                              // เนินไกลๆ
        g.fillOval(-160, 330, 640, 300);
        g.fillOval(380, 350, 640, 300);
        g.setColor(UI.GRASS_LIGHT);
        g.fillOval(180, 370, 520, 260);
        paintGrass(g, 410, H, 9);
        g.setColor(UI.DARK);                                    // เส้นขอบพื้นหญ้า
        g.fillRect(0, 408, W, 4);
        g.dispose();
        return img;
    }

    private BufferedImage makeArenaLayer() {
        int pad = FENCE + 8;
        int aw = e.arena.width, ah = e.arena.height;
        BufferedImage img = new BufferedImage(aw + pad * 2, ah + pad * 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        UI.notch(g, pad - FENCE, pad - FENCE, aw + FENCE * 2, ah + FENCE * 2, 4, UI.DARK);          // รั้วไม้
        UI.notch(g, pad - FENCE + 2, pad - FENCE + 2, aw + FENCE * 2 - 4, ah + FENCE * 2 - 4, 2, UI.WOOD);
        g.setColor(UI.WOOD_LIGHT);
        g.fillRect(pad - FENCE + 4, pad - FENCE + 2, aw + FENCE * 2 - 8, 2);

        g.setColor(UI.DARK);                                    // ขอบด้านในของรั้ว
        g.fillRect(pad - 2, pad - 2, aw + 4, ah + 4);
        g.setColor(UI.DIRT);                                    // พื้นดิน
        g.fillRect(pad, pad, aw, ah);
        g.setColor(UI.DIRT_DARK);
        g.fillRect(pad, pad, aw, 4);                            // เงาใต้รั้วด้านบน

        Random r = new Random(21);
        for (int i = 0; i < 90; i++) {                          // เม็ดดินสี่เหลี่ยมเล็กๆ
            int x = pad + 8 + r.nextInt((aw - 16) / 2) * 2, y = pad + 8 + r.nextInt((ah - 16) / 2) * 2;
            g.setColor(r.nextBoolean() ? UI.DIRT_DARK : new Color(216, 172, 122));
            g.fillRect(x, y, r.nextBoolean() ? 4 : 2, 2);
        }

        int[][] posts = {{0, 0}, {aw, 0}, {0, ah}, {aw, ah}, {aw / 2, 0}, {aw / 2, ah}};   // เสารั้ว
        for (int[] p : posts) {
            int px = pad + p[0] - 8, py = pad + p[1] - 8;
            g.setColor(UI.DARK);
            g.fillRect(px - 2, py - 2, 20, 20);
            g.setColor(UI.WOOD_LIGHT);
            g.fillRect(px, py, 16, 16);
            g.setColor(UI.WOOD);
            g.fillRect(px, py + 10, 16, 6);
        }
        g.dispose();
        return img;
    }

    // ==========================================================
    //  จุดเข้าหลัก
    // ==========================================================
    void render(Graphics2D g2) {
        int ox = 0, oy = 0;
        if (e.state == GameEngine.GameState.BATTLE && GameEngine.shakeTimer > 0) {
            ox = GameEngine.random.nextInt(8) - 4;
            oy = GameEngine.random.nextInt(8) - 4;
            g2.translate(ox, oy);
        }

        switch (e.state) {
            case MENU:     drawMenu(g2); break;
            case SELECT:   drawSelection(g2); break;
            case EXPLORE:  drawExplore(g2); break;
            case BATTLE:   drawBattle(g2); break;
            case VICTORY:  drawEndScreen(g2, true); break;
            case GAMEOVER: drawEndScreen(g2, false); break;
        }

        if (e.state == GameEngine.GameState.BATTLE && GameEngine.shakeTimer > 0) g2.translate(-ox, -oy);

        if (e.showFps) {
            PixelText.rightOutlined(g2, e.getFps() + " fps", W - 8, H - 8, PixelText.SMALL, UI.CREAM, UI.DARK);
        }
    }

    // ==========================================================
    //  เมนู
    // ==========================================================
    private void drawMenu(Graphics2D g2) {
        g2.drawImage(menuBg, 0, 0, null);
        drawCloud(g2, 60, 70, 0.25, 1.0);
        drawCloud(g2, 420, 130, 0.15, 0.7);

        drawMascot(g2, e.availableChickens.get(1), 150, 478, 84);
        drawMascot(g2, e.enemy, 650, 478, 92);

        PixelText.centerOutlined(g2, "Cockfight", W / 2 + 3, 183, PixelText.TITLE, UI.DARK, UI.DARK);   // เงา
        PixelText.centerOutlined(g2, "Cockfight", W / 2, 178, PixelText.TITLE, UI.YELLOW, UI.DARK);

        UI.button(g2, GameEngine.START_BTN, "Start game", GameEngine.START_BTN.contains(e.mouseX, e.mouseY));
        UI.button(g2, GameEngine.EXIT_BTN, "Exit", GameEngine.EXIT_BTN.contains(e.mouseX, e.mouseY));
    }

    private void drawCloud(Graphics2D g2, int baseX, int y, double speed, double scale) {
        int span = W + 260;
        int x = (int) ((baseX + GameEngine.tick * speed) % span) - 130;
        int s = (int) (24 * scale);
        g2.setColor(Color.WHITE);
        g2.fillOval(x, y, s * 3, s * 2);
        g2.fillOval(x + s, y - s / 2, s * 2, s * 2);
        g2.fillOval(x + s * 2, y + s / 4, s * 3, s * 2 - s / 3);
        g2.setColor(new Color(214, 232, 246));
        g2.fillRect(x + s / 2, y + s * 2 - s / 4, s * 4, s / 4);
    }

    private void drawMascot(Graphics2D g2, Chicken c, int cx, int cy, int size) {
        UI.shadowEllipse(g2, cx, cy + size * 0.5, size * 0.8, 14, 90);
        Image s = c.imageForSize(size);
        if (s != null) {
            g2.drawImage(s, cx - size / 2, cy - size / 2, null);
        } else {
            g2.setColor(UI.DARK);
            g2.fillOval(cx - size / 2 - 3, cy - size / 2 - 3, size + 6, size + 6);
            g2.setColor(c.color);
            g2.fillOval(cx - size / 2, cy - size / 2, size, size);
        }
    }

    // ==========================================================
    //  เลือกตัวละคร
    // ==========================================================
    private void drawSelection(Graphics2D g2) {
        g2.drawImage(grassBg, 0, 0, null);

        UI.panel(g2, 210, 20, 380, 64);
        PixelText.center(g2, "Pick 2 chickens  " + e.myChickens.size() + "/2", W / 2, 61, PixelText.HEAD, UI.DARK);

        for (int i = 0; i < e.availableChickens.size(); i++) drawCard(g2, i);
    }

    private void drawCard(Graphics2D g2, int i) {
        HeroChicken h = e.availableChickens.get(i);
        Rectangle r = GameEngine.cardRect(i);
        boolean selected = e.hasChickenOfType(h);
        boolean full = e.myChickens.size() >= 2;
        boolean hover = r.contains(e.mouseX, e.mouseY) && !selected && !full;
        int x = r.x, y = r.y, w = r.width, hh = r.height;
        int cx = x + w / 2;

        if (selected || hover) {                                // ไฮไลต์: กรอบสีรอบการ์ด (เลือกแล้ว = ฟ้า, ชี้อยู่ = เหลือง)
            UI.notch(g2, x - 8, y - 8, w + 16, hh + 16, 4, UI.DARK);
            UI.notch(g2, x - 6, y - 6, w + 12, hh + 12, 4, selected ? UI.BLUE : UI.YELLOW);
        }
        UI.panel(g2, x, y, w, hh);

        // ช่องใส่รูปแบบช่องเก็บของ: ขอบเข้ม พื้นเบจเข้ม เงาขอบด้านบน/ซ้ายให้ดูจมลงไป
        int sx = cx - 58, sy = y + 18, ss = 116;
        UI.notch(g2, sx, sy, ss, ss, 4, UI.DARK);
        UI.notch(g2, sx + 2, sy + 2, ss - 4, ss - 4, 2, new Color(198, 150, 90));
        UI.notch(g2, sx + 4, sy + 4, ss - 8, ss - 8, 0, new Color(224, 184, 118));
        UI.shadowEllipse(g2, cx, sy + ss - 14, 64, 10, 70);          // เงาใต้เท้า
        Image s = h.imageForSize(96);
        if (s != null) {
            g2.drawImage(s, cx - 48, sy + ss - 96 - 8, null);       // เท้าชิดล่างของช่อง เหนือขอบ 8 พิกเซล
        } else {
            g2.setColor(UI.DARK);
            g2.fillOval(cx - 43, sy + 15, 86, 86);
            g2.setColor(h.color);
            g2.fillOval(cx - 40, sy + 18, 80, 80);
        }

        PixelText.center(g2, h.name, cx, y + 162, PixelText.HEAD, UI.DARK);

        int lx = x + 24, rx = x + w - 24;
        String[] labels = {"HP", "Attack", "Speed"};
        String[] values = {String.valueOf(h.maxHp), String.valueOf(h.attack), String.valueOf(h.baseSpeed)};
        for (int k = 0; k < 3; k++) {
            int ty = y + 198 + k * 24;
            PixelText.draw(g2, labels[k], lx, ty, PixelText.BODY, UI.DARK);
            PixelText.right(g2, values[k], rx, ty, PixelText.BODY, UI.DARK);
        }

        g2.setColor(UI.PARCH_DARK);
        g2.fillRect(lx, y + 258, w - 48, 2);

        boolean hasIcon = h.skillIcon != null && h.skillIcon.getWidth(null) > 0;
        int nameX = lx;
        if (hasIcon) {                                          // ไอคอนสกิลอยู่ซ้ายของชื่อสกิล
            int iy = y + 266;
            UI.notch(g2, lx, iy, 36, 36, 2, UI.DARK);
            UI.notch(g2, lx + 2, iy + 2, 32, 32, 0, new Color(224, 184, 118));
            g2.drawImage(h.skillIcon, lx + 2, iy + 2, null);
            nameX = lx + 46;
        }
        PixelText.draw(g2, h.skillName(), nameX, y + 289, PixelText.BODY, UI.RED);
        PixelText.draw(g2, "Cooldown " + h.maxSkillCooldown, nameX, y + 301, PixelText.TINY, new Color(150, 100, 60));

        String[] lines = h.skillDesc().split("\n");
        for (int k = 0; k < lines.length; k++) PixelText.draw(g2, lines[k], lx, y + 326 + k * 18, PixelText.SMALL, UI.DARK);

        if (selected) {
            UI.plate(g2, cx - 44, y - 30, 88, 30);
            PixelText.center(g2, "Picked", cx, y - 8, PixelText.SMALL, new Color(40, 110, 30));
        } else if (full) {
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillRect(x, y, w, hh);
        }
    }

    // ==========================================================
    //  เดินสำรวจ
    // ==========================================================
    private void drawExplore(Graphics2D g2) {
        g2.drawImage(grassBg, 0, 0, null);

        BossChicken b = e.enemy;
        UI.shadowEllipse(g2, b.centerX(), b.y + b.size * 0.92, b.size * 0.85, 12, 90);
        Image s = b.imageForSize(b.size);
        if (s != null) {
            g2.drawImage(s, (int) b.x, (int) b.y, null);
        } else {
            g2.setColor(UI.DARK);
            g2.fillOval((int) b.x - 2, (int) b.y - 2, b.size + 4, b.size + 4);
            g2.setColor(b.color);
            g2.fillOval((int) b.x, (int) b.y, b.size, b.size);
        }
        boolean near = Math.hypot(e.playerX + 15 - b.centerX(), e.playerY + 15 - b.centerY()) < 130;
        PixelText.centerOutlined(g2, near ? "Fight!" : "Void Boss", (int) b.centerX(), (int) b.y - 12,
                PixelText.BODY, near ? UI.YELLOW : UI.CREAM, UI.DARK);

        for (HeroChicken c : e.myChickens) c.draw(g2);

        int px = e.playerX, py = e.playerY;
        UI.shadowEllipse(g2, px + 15, py + 29, 26, 8, 90);
        g2.setColor(UI.DARK);
        g2.fillOval(px - 2, py - 2, 34, 34);
        g2.setColor(UI.BLUE);
        g2.fillOval(px, py, 30, 30);
        int bob = (GameEngine.tick / 15) % 2 == 0 ? 0 : 2;       // ลูกศรชี้ตัวผู้เล่น
        int ay = py - 16 + bob;
        g2.setColor(UI.DARK);
        g2.fillPolygon(new int[]{px + 7, px + 23, px + 15}, new int[]{ay - 2, ay - 2, ay + 10}, 3);
        g2.setColor(UI.CREAM);
        g2.fillPolygon(new int[]{px + 10, px + 20, px + 15}, new int[]{ay, ay, ay + 7}, 3);

        UI.plate(g2, 290, 548, 220, 32);
        PixelText.center(g2, "WASD to move", W / 2, 570, PixelText.BODY, UI.DARK);
    }

    // ==========================================================
    //  ต่อสู้
    // ==========================================================
    private void drawBattle(Graphics2D g2) {
        g2.drawImage(grassBg, 0, 0, null);
        int pad = FENCE + 8;
        g2.drawImage(arenaLayer, e.arena.x - pad, e.arena.y - pad, null);

        for (Portal p : e.portals) p.draw(g2);
        for (Obstacle o : e.obstacles) o.draw(g2);
        for (EggMine m : e.eggMines) m.draw(g2);
        for (BuffItem b : e.activeBuffs) b.draw(g2);

        for (HeroChicken c : e.myChickens) if (c.isAlive()) drawHeroRing(g2, c);
        if (e.enemy.enraged && e.enemy.isAlive()) {
            g2.setColor(UI.RED);
            g2.setStroke(new BasicStroke(3f));
            g2.drawOval((int) e.enemy.x - 5, (int) e.enemy.y - 5, e.enemy.size + 10, e.enemy.size + 10);
            g2.setStroke(new BasicStroke(1f));
        }
        e.enemy.draw(g2);
        for (HeroChicken c : e.myChickens) if (c.isAlive()) c.draw(g2);

        for (Particle p : GameEngine.particles) p.draw(g2);

        drawBossBar(g2);
        drawTurnTab(g2);
        drawHeroHud(g2);

        if (e.bPhase == GameEngine.BattlePhase.AIMING && e.selectedChicken != null) drawAimGuide(g2);
        for (DamageText dt : e.damageTexts) dt.draw(g2);

        String hint = e.bPhase == GameEngine.BattlePhase.PLAYER_TURN ? "Drag a chicken back to aim"
                : e.bPhase == GameEngine.BattlePhase.AIMING ? "Let go to launch" : "";
        if (!hint.isEmpty()) PixelText.centerOutlined(g2, hint, W / 2, 506, PixelText.BODY, UI.CREAM, UI.DARK);
    }

    private void drawHeroRing(Graphics2D g2, HeroChicken c) {
        boolean selected = e.bPhase == GameEngine.BattlePhase.AIMING && c == e.selectedChicken;
        boolean hover = e.bPhase == GameEngine.BattlePhase.PLAYER_TURN && c.isCollidingWithPoint(e.mouseX, e.mouseY, c.size);
        if (!selected && !hover) return;
        g2.setColor(UI.YELLOW);
        g2.setStroke(new BasicStroke(3f));
        g2.drawOval((int) c.x - 6, (int) c.y - 6, c.size + 12, c.size + 12);
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawBossBar(Graphics2D g2) {
        BossChicken b = e.enemy;
        int x = 220, y = 10, w = 360, h = 50;
        UI.plate(g2, x, y, w, h);
        PixelText.draw(g2, b.enraged ? "Void Boss  is angry!" : "Void Boss", x + 12, y + 22, PixelText.BODY, b.enraged ? UI.RED : UI.DARK);
        PixelText.right(g2, Math.max(0, b.hp) + "/" + b.maxHp, x + w - 12, y + 22, PixelText.SMALL, UI.DARK);
        UI.bar(g2, x + 12, y + 28, w - 24, 14, Math.max(0, b.hp) / (double) b.maxHp, UI.RED);
    }

    private void drawTurnTab(Graphics2D g2) {
        GameEngine.BattlePhase p = e.bPhase;
        boolean mine = p == GameEngine.BattlePhase.PLAYER_TURN || p == GameEngine.BattlePhase.AIMING
                || p == GameEngine.BattlePhase.PLAYER_MOVING;
        UI.plate(g2, 330, 66, 140, 32);
        PixelText.center(g2, mine ? "Your turn" : "Boss turn", W / 2, 89, PixelText.BODY, mine ? new Color(40, 110, 30) : UI.RED);
    }

    private void drawHeroHud(Graphics2D g2) {
        for (int i = 0; i < e.myChickens.size(); i++) {
            HeroChicken c = e.myChickens.get(i);
            int x = 10, y = 10 + i * 58, w = 176, h = 50;
            boolean alive = c.isAlive();
            UI.plate(g2, x, y, w, h);

            g2.setColor(UI.DARK);                               // ช่องรูปเหลี่ยม
            g2.fillRect(x + 6, y + 8, 34, 34);
            g2.setColor(UI.PARCH_DARK);
            g2.fillRect(x + 8, y + 10, 30, 30);
            Image s = c.imageForSize(28);
            if (s != null) g2.drawImage(s, x + 9, y + 11, null);
            else { g2.setColor(c.color); g2.fillOval(x + 12, y + 14, 22, 22); }

            double frac = c.maxHp == 0 ? 0 : Math.max(0, c.hp) / (double) c.maxHp;
            UI.bar(g2, x + 46, y + 10, 72, 12, frac, frac < 0.3 ? UI.RED : UI.GREEN);
            PixelText.draw(g2, Math.max(0, c.hp) + "/" + c.maxHp, x + 46, y + 40, PixelText.SMALL, UI.DARK);

            if (!alive) {
                g2.setColor(new Color(0, 0, 0, 100));
                g2.fillRect(x, y, w, h);
                continue;
            }
            boolean ready = c.skillCooldown <= 0 && !c.skillActive;
            int state = c.skillActive ? 1 : ready ? 0 : 2;
            UI.skillTile(g2, x + w - 46, y + 6, 38, c.skillIcon, i == 0 ? "Z" : "X", state, c.skillCooldown);
        }
    }

    // ----------------------------------------------------------
    //  เส้นเล็ง: จุดสี่เหลี่ยมตามวิถีจริง สีเปลี่ยนตามแรง
    // ----------------------------------------------------------
    private void drawAimGuide(Graphics2D g2) {
        HeroChicken c = e.selectedChicken;
        double cx = c.centerX(), cy = c.centerY();

        g2.setColor(UI.DARK);
        g2.setStroke(new BasicStroke(3f));
        g2.drawLine((int) cx, (int) cy, e.dragX, e.dragY);
        g2.setStroke(new BasicStroke(1f));
        g2.fillRect(e.dragX - 4, e.dragY - 4, 8, 8);

        double dx = cx - e.dragX, dy = cy - e.dragY;
        double dragDist = Math.hypot(dx, dy);
        if (dragDist < GameEngine.MIN_DRAG) return;   // ใกล้เกินไป = โซนยกเลิก

        double power = GameEngine.calcPower(c, dragDist);
        Color dot = UI.lerp(UI.CREAM, UI.RED, power / GameEngine.MAX_POWER);
        double angle = Math.atan2(dy, dx);
        AimPredictor.Result res = AimPredictor.predict(
                c, power * Math.cos(angle), power * Math.sin(angle), e.arena, e.obstacles, e.portals);

        double travelled = 0, nextDot = 16, spacing = 15;
        double[] last = res.segments.get(0).get(0);
        for (java.util.ArrayList<double[]> seg : res.segments) {
            for (int i = 1; i < seg.size(); i++) {
                double[] a = seg.get(i - 1), b = seg.get(i);
                double len = Math.hypot(b[0] - a[0], b[1] - a[1]);
                if (len == 0) continue;
                while (nextDot <= travelled + len) {
                    double t = (nextDot - travelled) / len;
                    int px = (int) (a[0] + (b[0] - a[0]) * t), py = (int) (a[1] + (b[1] - a[1]) * t);
                    g2.setColor(UI.DARK);
                    g2.fillRect(px - 4, py - 4, 8, 8);
                    g2.setColor(dot);
                    g2.fillRect(px - 2, py - 2, 4, 4);
                    nextDot += spacing;
                }
                travelled += len;
            }
            last = seg.get(seg.size() - 1);
        }

        double a = res.endAngle;                                // หัวลูกศรหมุนตามทิศก้าวสุดท้ายจริง
        int as = 16;
        int[] xp = {(int) last[0], (int) (last[0] - as * Math.cos(a - Math.PI / 6)), (int) (last[0] - as * Math.cos(a + Math.PI / 6))};
        int[] yp = {(int) last[1], (int) (last[1] - as * Math.sin(a - Math.PI / 6)), (int) (last[1] - as * Math.sin(a + Math.PI / 6))};
        g2.setColor(dot);
        g2.fillPolygon(xp, yp, 3);
        g2.setColor(UI.DARK);
        g2.drawPolygon(xp, yp, 3);
    }

    // ==========================================================
    //  จบเกม
    // ==========================================================
    private void drawEndScreen(Graphics2D g2, boolean victory) {
        drawBattle(g2);
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRect(0, 0, W, H);

        int pw = 380, ph = 200, px = (W - pw) / 2, py = 200;
        UI.panel(g2, px, py, pw, ph);
        PixelText.center(g2, victory ? "You win!" : "You lost", W / 2, py + 70, PixelText.BIG, victory ? new Color(40, 110, 30) : UI.RED);

        PixelText.center(g2, "Turns  " + (e.turnCounter + 1), W / 2, py + 108, PixelText.BODY, UI.DARK);
        if (victory) {
            int alive = 0;
            for (HeroChicken c : e.myChickens) if (c.isAlive()) alive++;
            PixelText.center(g2, "Chickens left  " + alive + "/" + e.myChickens.size(), W / 2, py + 134, PixelText.BODY, UI.DARK);
        } else {
            PixelText.center(g2, "Boss HP left  " + Math.max(0, e.enemy.hp), W / 2, py + 134, PixelText.BODY, UI.DARK);
        }

        if ((GameEngine.tick / 30) % 2 == 0) {
            PixelText.center(g2, "Press Enter to play again", W / 2, py + 170, PixelText.SMALL, UI.DARK);
        }
    }
}
