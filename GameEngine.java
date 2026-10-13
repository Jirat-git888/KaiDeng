import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.locks.LockSupport;

/**
 * ตัวควบคุมเกมหลัก: state machine, ลูปเกม, การวาด และอินพุต
 *
 * ตรรกะเกมอยู่ในคลาสนี้ ส่วนการวาดอยู่ใน GameRenderer
 * ลูปเกมรันบนเธรดของตัวเองแบบ fixed timestep 60 อัปเดต/วินาที (ความเร็วเกมไม่ขึ้นกับความแรงเครื่อง)
 * โดยวาดลง back buffer ในเธรดเกม แล้วให้ EDT แค่ copy ภาพขึ้นจอ
 * ส่วน state ทั้งหมดป้องกันด้วย lock เดียวกับตัวจัดการอินพุต จึงไม่เกิด ConcurrentModificationException
 */
class GameEngine extends JPanel implements KeyListener, MouseListener, MouseMotionListener {
    enum GameState { MENU, SELECT, EXPLORE, BATTLE, VICTORY, GAMEOVER }
    enum BattlePhase { PLAYER_TURN, AIMING, PLAYER_MOVING, BOSS_TURN, BOSS_MOVING }

    static final double MAX_POWER = 22.0;
    static final double POWER_SCALE = 0.035;
    static final int MIN_DRAG = 15;

    static Random random = new Random();
    static ArrayList<Particle> particles = new ArrayList<>();
    static int shakeTimer = 0;
    static int hitStopTimer = 0;

    GameState state = GameState.MENU;
    BattlePhase bPhase = BattlePhase.PLAYER_TURN;

    final int width = 800, height = 600;

    static final int TARGET_FPS = 60;
    static final long FRAME_NANOS = 1_000_000_000L / TARGET_FPS;

    private final Object lock = new Object();      // ป้องกัน state ระหว่างเธรดเกมกับอินพุตจาก EDT
    private final Object swapLock = new Object();  // ป้องกันตอนสลับ front/back buffer
    private BufferedImage frontBuffer, backBuffer;
    private Thread loopThread;
    private volatile boolean running = false;
    private volatile int currentFps = 0, currentUps = 0;
    boolean showFps = true;                        // กด F3 เพื่อเปิด/ปิดตัวนับ FPS
    private GameRenderer renderer;

    /** ตัวนับเฟรมสำหรับแอนิเมชันของ UI (พัลส์, หมุน, ลอย) */
    static int tick = 0;
    int mouseX = -1, mouseY = -1;

    static final Rectangle START_BTN = new Rectangle(280, 290, 240, 52);
    static final Rectangle EXIT_BTN = new Rectangle(280, 354, 240, 52);
    static Rectangle cardRect(int i) { return new Rectangle(24 + i * 256, 110, 240, 378); }

    int playerX = 400, playerY = 300, playerSpeed = 4;
    boolean up, down, left, right;

    Image bgImg = new ImageIcon("bg.png").getImage();
    Image bgScaled;   // bg.png ที่ย่อเป็นขนาดสนามไว้ล่วงหน้า

    ArrayList<HeroChicken> availableChickens = new ArrayList<>();
    ArrayList<HeroChicken> myChickens = new ArrayList<>();
    BossChicken enemy;

    ArrayList<EggMine> eggMines = new ArrayList<>();
    ArrayList<BuffItem> activeBuffs = new ArrayList<>();
    ArrayList<DamageText> damageTexts = new ArrayList<>();
    ArrayList<Obstacle> obstacles = new ArrayList<>();
    ArrayList<Portal> portals = new ArrayList<>();

    Rectangle arena = new Rectangle(200, 150, 400, 300);

    HeroChicken selectedChicken = null;
    int dragX, dragY;
    int turnCounter = 0;

    public GameEngine() {
        setPreferredSize(new Dimension(width, height));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);

        availableChickens.add(new TankyChicken());
        availableChickens.add(new SpeedyChicken());
        availableChickens.add(new BalancedChicken());

        enemy = new BossChicken(400, 250);

        if (bgImg.getWidth(null) > 0) bgScaled = ImageUtil.scale(bgImg, arena.width, arena.height);
        frontBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        backBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        renderer = new GameRenderer(this);
    }

    public void startGame() {
        if (running) return;
        running = true;
        loopThread = new Thread(this::gameLoop, "game-loop");
        loopThread.setDaemon(true);
        loopThread.start();
    }

    public void stopGame() { running = false; }

    int getFps() { return currentFps; }
    int getUps() { return currentUps; }

    /**
     * Fixed timestep: อัปเดตตรง 60 ครั้ง/วินาทีตามตารางเวลา (ไม่ใช่ตามเวลาที่ผ่านไปจริง
     * จึงไม่กระตุกแบบ 0 ครั้ง/2 ครั้งสลับกัน) ถ้าเครื่องช้าจนตามหลังจะข้ามการวาดเพื่อไล่ให้ทัน
     */
    private void gameLoop() {
        long next = System.nanoTime();
        long statTime = next;
        int frames = 0, updates = 0;

        while (running) {
            synchronized (lock) { update(); }
            updates++;
            next += FRAME_NANOS;

            long now = System.nanoTime();
            if (now - next > 5 * FRAME_NANOS) next = now;   // ค้างนาน (เช่นเครื่องหน่วง) ไม่ต้องไล่ย้อนหลัง

            if (now <= next) {                               // ทันตารางเวลา → วาดแล้วรอเฟรมถัดไป
                synchronized (lock) { renderToBuffer(); }
                repaint();
                frames++;
                waitUntil(next);
            }                                                // ตามหลังอยู่ → ข้ามการวาด อัปเดตต่อทันที

            long t = System.nanoTime();
            if (t - statTime >= 1_000_000_000L) {
                currentFps = frames;
                currentUps = updates;
                frames = 0;
                updates = 0;
                statTime = t;
            }
        }
    }

    /** พักเธรดจนถึงเวลาที่กำหนด: park เกือบทั้งหมด แล้ว spin ช่วง ~1 ms สุดท้ายเพื่อความแม่น */
    private static void waitUntil(long deadline) {
        long remaining;
        while ((remaining = deadline - System.nanoTime()) > 0) {
            if (remaining > 2_000_000L) LockSupport.parkNanos(remaining - 1_000_000L);
            else Thread.onSpinWait();
        }
    }

    private void renderToBuffer() {
        Graphics2D g2 = backBuffer.createGraphics();
        try {
            // ปิด anti-alias ทั้งหมดให้ขอบคมแบบพิกเซล (สไตล์ Stardew)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            renderFrame(g2);
        } finally {
            g2.dispose();
        }
        synchronized (swapLock) {
            BufferedImage tmp = frontBuffer;
            frontBuffer = backBuffer;
            backBuffer = tmp;
        }
    }

    public static void spawnParticles(double px, double py, Color c, int amount) {
        for (int i = 0; i < amount; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double speed = 2 + random.nextDouble() * 5;
            particles.add(new Particle(px, py, speed * Math.cos(angle), speed * Math.sin(angle), c));
        }
    }

    static double calcPower(HeroChicken c, double dragDist) {
        return Math.min(dragDist * POWER_SCALE * c.speed, MAX_POWER);
    }

    boolean hasChickenOfType(HeroChicken template) {
        for (HeroChicken c : myChickens) {
            if (c.getClass() == template.getClass()) return true;
        }
        return false;
    }

    private void clearInput() { up = down = left = right = false; }

    private void resetGame() {
        myChickens.clear();
        enemy = new BossChicken(400, 250);
        eggMines.clear();
        activeBuffs.clear();
        damageTexts.clear();
        obstacles.clear();
        portals.clear();
        particles.clear();
        selectedChicken = null;
        bPhase = BattlePhase.PLAYER_TURN;
        playerX = 400;
        playerY = 300;
        turnCounter = 0;
        shakeTimer = 0;
        hitStopTimer = 0;
        clearInput();
        state = GameState.MENU;
    }

    // ==========================================
    // UPDATE
    // ==========================================
    void update() {
        tick++;
        if (state == GameState.EXPLORE) {
            updateExplore();
        } else if (state == GameState.BATTLE) {
            if (hitStopTimer > 0) { hitStopTimer--; return; }
            updateBattle();
            if (shakeTimer > 0) shakeTimer--;
        }
    }

    private void updateExplore() {
        if (up)    playerY -= playerSpeed;
        if (down)  playerY += playerSpeed;
        if (left)  playerX -= playerSpeed;
        if (right) playerX += playerSpeed;
        playerX = Math.max(0, Math.min(width - 30, playerX));
        playerY = Math.max(0, Math.min(height - 30, playerY));

        for (int i = 0; i < myChickens.size(); i++) {
            HeroChicken c = myChickens.get(i);
            int targetX = (i == 0) ? playerX : myChickens.get(0).getX();
            int targetY = (i == 0) ? playerY : myChickens.get(0).getY();
            c.followTarget(targetX, targetY);
        }
        if (enemy.isCollidingWithPoint(playerX, playerY, 40)) startBattle();
    }

    private void updateBattle() {
        enemy.updatePhysics(arena, eggMines);
        enemy.handleObstacles(obstacles);
        enemy.handlePortals(portals);

        updateParticles();
        updateHeroes();
        updateTurnPhase();
        updateMines();
        updateBuffs();
        updateDamageTexts();
        checkEndConditions();
    }

    private void updateParticles() {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle pt = it.next();
            pt.update();
            if (pt.life <= 0) it.remove();
        }
    }

    private void updateHeroes() {
        for (int i = 0; i < myChickens.size(); i++) {
            HeroChicken c1 = myChickens.get(i);
            if (!c1.isAlive()) continue;

            c1.updatePhysics(arena, eggMines);
            c1.handleObstacles(obstacles);
            c1.handlePortals(portals);
            c1.checkCollisionWith(enemy, damageTexts);

            for (int j = i + 1; j < myChickens.size(); j++) {
                HeroChicken c2 = myChickens.get(j);
                if (c2.isAlive() && c1.isCollidingWith(c2)) resolveHeroCollision(c1, c2);
            }
        }
    }

    private void resolveHeroCollision(HeroChicken c1, HeroChicken c2) {
        double dx = c1.centerX() - c2.centerX();
        double dy = c1.centerY() - c2.centerY();
        double dist = Math.hypot(dx, dy);
        if (dist == 0) { dx = 1; dy = 0; dist = 1; } // กันหารศูนย์
        double overlap = ((c1.size + c2.size) / 2.0) - dist;
        if (overlap > 0) {
            c1.x += (dx / dist) * overlap / 2.0;
            c1.y += (dy / dist) * overlap / 2.0;
            c2.x -= (dx / dist) * overlap / 2.0;
            c2.y -= (dy / dist) * overlap / 2.0;
        }
        double tempVx = c1.vx, tempVy = c1.vy;
        c1.vx = c2.vx;  c1.vy = c2.vy;
        c2.vx = tempVx; c2.vy = tempVy;
        spawnParticles(c1.centerX(), c1.centerY(), Color.WHITE, 5);
    }

    private void updateTurnPhase() {
        boolean anyoneMoving = enemy.isMoving();
        for (HeroChicken c : myChickens) {
            if (c.isAlive() && c.isMoving()) anyoneMoving = true;
        }

        switch (bPhase) {
            case PLAYER_MOVING:
                if (!anyoneMoving) bPhase = BattlePhase.BOSS_TURN;
                break;
            case BOSS_TURN:
                launchBoss();
                break;
            case BOSS_MOVING:
                if (!anyoneMoving) startNextPlayerTurn();
                break;
            default:
                break;
        }
    }

    private void launchBoss() {
        HeroChicken target = null;
        double minDist = Double.MAX_VALUE;
        for (HeroChicken c : myChickens) {
            if (!c.isAlive()) continue;
            double d = Math.hypot(c.x - enemy.x, c.y - enemy.y);
            if (d < minDist) { minDist = d; target = c; }
        }
        double angle = (target != null)
                ? Math.atan2(target.y - enemy.y, target.x - enemy.x) + (random.nextDouble() - 0.5) * 0.3
                : random.nextDouble() * 2 * Math.PI;

        double bossPower = enemy.enraged ? 25.0 : 17.0; // โหมดโกรธพุ่งแรงขึ้น
        enemy.vx = bossPower * Math.cos(angle);
        enemy.vy = bossPower * Math.sin(angle);
        bPhase = BattlePhase.BOSS_MOVING;
    }

    private void startNextPlayerTurn() {
        bPhase = BattlePhase.PLAYER_TURN;
        turnCounter++;
        for (HeroChicken c : myChickens) {
            if (c.isAlive()) c.onTurnStart();
        }
        if (turnCounter % 2 == 0 && activeBuffs.size() < 4) {
            activeBuffs.add(new BuffItem(
                    arena.x + 20 + random.nextInt(arena.width - 40),
                    arena.y + 20 + random.nextInt(arena.height - 40)));
        }
    }

    private void updateMines() {
        Iterator<EggMine> it = eggMines.iterator();
        while (it.hasNext()) {
            EggMine m = it.next();
            if (enemy.isAlive() && enemy.isCollidingWith(m)) {
                enemy.takeDamage(25, damageTexts);
                enemy.vx = 0;
                enemy.vy = 0;
                shakeTimer = 15;
                hitStopTimer = 8;
                spawnParticles(m.centerX(), m.centerY(), new Color(255, 100, 50), 30);
                it.remove();
            }
        }
    }

    private void updateBuffs() {
        Iterator<BuffItem> it = activeBuffs.iterator();
        while (it.hasNext()) {
            BuffItem b = it.next();
            for (HeroChicken c : myChickens) {
                if (c.isAlive() && c.isCollidingWith(b)) {
                    b.applyBuffTo(c, damageTexts);
                    it.remove();
                    break;
                }
            }
        }
    }

    private void updateDamageTexts() {
        Iterator<DamageText> it = damageTexts.iterator();
        while (it.hasNext()) {
            DamageText dt = it.next();
            dt.update();
            if (dt.isDead()) it.remove();
        }
    }

    private void checkEndConditions() {
        if (!enemy.isAlive()) state = GameState.VICTORY;
        boolean allDead = true;
        for (HeroChicken c : myChickens) if (c.isAlive()) allDead = false;
        if (allDead) state = GameState.GAMEOVER;
    }

    private void startBattle() {
        state = GameState.BATTLE;
        bPhase = BattlePhase.PLAYER_TURN;
        selectedChicken = null;
        activeBuffs.clear();
        eggMines.clear();
        obstacles.clear();
        portals.clear();
        particles.clear();
        turnCounter = 0;
        clearInput();

        enemy.spawnInArena(500, 250);
        int startY = 200;
        for (HeroChicken c : myChickens) {
            c.spawnInArena(250, startY);
            c.skillCooldown = 0;
            c.resetSkill();
            startY += 80;
        }

        spawnObstacles();
        spawnPortals();
    }

    private void spawnObstacles() {
        int obsCount = 1 + random.nextInt(3);
        for (int i = 0; i < obsCount; i++) {
            boolean valid = false;
            int ox = 0, oy = 0, attempts = 0;
            while (!valid && attempts < 100) {
                ox = arena.x + 50 + random.nextInt(arena.width - 100);
                oy = arena.y + 50 + random.nextInt(arena.height - 100);
                if (Math.hypot(ox - 500, oy - 250) > 60
                        && Math.hypot(ox - 250, oy - 200) > 80
                        && Math.hypot(ox - 250, oy - 280) > 80) {
                    valid = true;
                    for (Obstacle o : obstacles) {
                        if (Math.hypot(ox - o.x, oy - o.y) < 60) valid = false;
                    }
                }
                attempts++;
            }
            if (valid) obstacles.add(new Obstacle(ox, oy, 35));
        }
    }

    /** สุ่มหลุมวาร์ป 1 คู่ที่ลิงก์กัน (เลี่ยงจุดเกิดของบอสและฮีโร่ ไม่งั้นจะโดนวาร์ปตั้งแต่ขยับครั้งแรก) */
    private void spawnPortals() {
        Portal[] pair = new Portal[2];
        for (int i = 0; i < 2; i++) {
            boolean valid = false;
            int px = 0, py = 0, attempts = 0;
            while (!valid && attempts < 100) {
                px = arena.x + 40 + random.nextInt(arena.width - 80);
                py = arena.y + 40 + random.nextInt(arena.height - 80);
                valid = true;
                if (Math.hypot(px - 500, py - 250) < 70) valid = false;
                if (Math.hypot(px - 250, py - 200) < 70) valid = false;
                if (Math.hypot(px - 250, py - 280) < 70) valid = false;
                for (Obstacle o : obstacles) {
                    if (Math.hypot(px - o.x, py - o.y) < 60) valid = false;
                }
                if (i == 1 && Math.hypot(px - pair[0].x, py - pair[0].y) < 100) valid = false;
                attempts++;
            }
            pair[i] = new Portal(px, py, 45);
            portals.add(pair[i]);
        }
        pair[0].linkedPortal = pair[1];
        pair[1].linkedPortal = pair[0];
        pair[0].tint = UI.BLUE;                   // คู่ละสี จะได้เห็นว่าหลุมไหนไปออกหลุมไหน
        pair[1].tint = UI.PURPLE;
    }

    // ==========================================
    // DRAW
    // ==========================================
    /** EDT แค่ copy ภาพที่เธรดเกมวาดเสร็จแล้วขึ้นจอ */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        synchronized (swapLock) {
            g.drawImage(frontBuffer, 0, 0, null);
        }
        Toolkit.getDefaultToolkit().sync();
    }

    /** วาดทั้งเฟรมลง g2 (เรียกจากเธรดเกมขณะถือ lock) */
    void renderFrame(Graphics2D g2) { renderer.render(g2); }

    // ==========================================
    // INPUT
    // ==========================================
    private void onMousePressed(MouseEvent e) {
        int mx = e.getX(), my = e.getY();

        if (state == GameState.MENU) {
            if (START_BTN.contains(mx, my)) state = GameState.SELECT;
            else if (EXIT_BTN.contains(mx, my)) System.exit(0);

        } else if (state == GameState.SELECT) {
            for (int i = 0; i < availableChickens.size(); i++) {
                boolean hit = cardRect(i).contains(mx, my);
                if (!hit) continue;
                HeroChicken template = availableChickens.get(i);
                // เดิมเช็ค contains(template) ซึ่งไม่เคยจริงเพราะเก็บเป็น clone จึงเลือกซ้ำได้ → เช็คตามชนิดแทน
                if (myChickens.size() < 2 && !hasChickenOfType(template)) {
                    myChickens.add(template.cloneHero());
                }
            }
            if (myChickens.size() >= 2) enterExplore();

        } else if (state == GameState.BATTLE && bPhase == BattlePhase.PLAYER_TURN) {
            if (!SwingUtilities.isLeftMouseButton(e)) return;
            for (HeroChicken c : myChickens) {
                if (c.isAlive() && c.isCollidingWithPoint(mx, my, c.size)) {
                    selectedChicken = c;
                    bPhase = BattlePhase.AIMING;
                    dragX = mx;
                    dragY = my;
                    break;
                }
            }

        } else if (state == GameState.VICTORY || state == GameState.GAMEOVER) {
            resetGame();
        }
    }

    private void enterExplore() {
        for (int i = 0; i < myChickens.size(); i++) {
            HeroChicken c = myChickens.get(i);
            c.x = playerX - 40 * (i + 1); // เริ่มข้างๆ ผู้เล่น ไม่ใช่มุมจอซ้ายบน
            c.y = playerY;
        }
        clearInput();
        state = GameState.EXPLORE;
    }

    private void onMouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        if (state == GameState.BATTLE && bPhase == BattlePhase.AIMING) {
            dragX = e.getX();
            dragY = e.getY();
        }
    }

    private void onMouseReleased(MouseEvent e) {
        if (state != GameState.BATTLE || bPhase != BattlePhase.AIMING || selectedChicken == null) return;

        double dx = selectedChicken.centerX() - e.getX();
        double dy = selectedChicken.centerY() - e.getY();
        double dragDist = Math.hypot(dx, dy);

        if (SwingUtilities.isRightMouseButton(e) || dragDist < MIN_DRAG) {
            selectedChicken = null;
            bPhase = BattlePhase.PLAYER_TURN;
            return;
        }

        double power = calcPower(selectedChicken, dragDist);
        double angle = Math.atan2(dy, dx);
        selectedChicken.vx = power * Math.cos(angle);
        selectedChicken.vy = power * Math.sin(angle);
        selectedChicken = null;
        bPhase = BattlePhase.PLAYER_MOVING;
    }

    private void onKeyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_F3) { showFps = !showFps; return; }
        if (state == GameState.EXPLORE) {
            if (k == KeyEvent.VK_W) up = true;
            if (k == KeyEvent.VK_S) down = true;
            if (k == KeyEvent.VK_A) left = true;
            if (k == KeyEvent.VK_D) right = true;
        } else if (state == GameState.BATTLE && bPhase == BattlePhase.PLAYER_TURN) {
            if (k == KeyEvent.VK_Z && myChickens.size() > 0) myChickens.get(0).activateSkill();
            if (k == KeyEvent.VK_X && myChickens.size() > 1) myChickens.get(1).activateSkill();
        } else if (state == GameState.VICTORY || state == GameState.GAMEOVER) {
            if (k == KeyEvent.VK_ENTER) resetGame();
        }
    }

    private void onKeyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_W) up = false;
        if (k == KeyEvent.VK_S) down = false;
        if (k == KeyEvent.VK_A) left = false;
        if (k == KeyEvent.VK_D) right = false;
    }

    // อินพุตมาจาก EDT แต่ state ถูกแก้โดยเธรดเกม จึงต้องผ่าน lock เดียวกัน
    @Override public void mousePressed(MouseEvent e)  { synchronized (lock) { onMousePressed(e); } }
    @Override public void mouseDragged(MouseEvent e)  { synchronized (lock) { onMouseDragged(e); } }
    @Override public void mouseReleased(MouseEvent e) { synchronized (lock) { onMouseReleased(e); } }
    @Override public void keyPressed(KeyEvent e)      { synchronized (lock) { onKeyPressed(e); } }
    @Override public void keyReleased(KeyEvent e)     { synchronized (lock) { onKeyReleased(e); } }

    @Override
    public void mouseMoved(MouseEvent e) {
        boolean hand;
        synchronized (lock) {
            mouseX = e.getX();
            mouseY = e.getY();
            hand = isHoveringInteractive();
        }
        setCursor(Cursor.getPredefinedCursor(hand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private boolean isHoveringInteractive() {
        switch (state) {
            case MENU:
                return START_BTN.contains(mouseX, mouseY) || EXIT_BTN.contains(mouseX, mouseY);
            case SELECT:
                if (myChickens.size() >= 2) return false;
                for (int i = 0; i < availableChickens.size(); i++) {
                    if (cardRect(i).contains(mouseX, mouseY) && !hasChickenOfType(availableChickens.get(i))) return true;
                }
                return false;
            case BATTLE:
                if (bPhase != BattlePhase.PLAYER_TURN) return false;
                for (HeroChicken c : myChickens) {
                    if (c.isAlive() && c.isCollidingWithPoint(mouseX, mouseY, c.size)) return true;
                }
                return false;
            default:
                return false;
        }
    }
    @Override public void mouseClicked(MouseEvent e) { }
    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e) { synchronized (lock) { mouseX = -1; mouseY = -1; } }
    @Override public void keyTyped(KeyEvent e) { }
}
