import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.locks.LockSupport;

/**
 * ตัวเกม (และจุดเริ่มโปรแกรม): เป็นเจ้าของลูปเกม (fixed timestep 60 อัปเดต/วินาที) และ double buffering
 * ไม่รู้กฎเกมใดๆ แค่ส่งต่อ update/render/อินพุตให้ฉากปัจจุบัน และให้ฉากสลับกันผ่าน show()
 *
 * ลูปเกมรันบนเธรดของตัวเอง ส่วนอินพุตมาจาก EDT จึงใช้ lock เดียวกันกับ update/render
 */
@SuppressWarnings("serial")
public class KaiDeng extends JPanel {
    // ---------- ค่าคงที่ของเกม ----------
    static final int WIDTH = 800, HEIGHT = 600;
    static final int TARGET_FPS = 60;
    static final int TEAM_SIZE = 2;

    static final Rectangle ARENA = new Rectangle(200, 150, 400, 300);

    static final Point PLAYER_START = new Point(400, 300);
    static final Point BOSS_EXPLORE_SPAWN = new Point(400, 250);
    static final Point BOSS_BATTLE_SPAWN = new Point(500, 250);
    static final Point HERO_BATTLE_SPAWN = new Point(250, 200);   // ตัวถัดไปเลื่อนลงทีละ HERO_SPAWN_GAP
    static final int HERO_SPAWN_GAP = 80;

    private static final long FRAME_NANOS = 1_000_000_000L / TARGET_FPS;

    /** จุดเริ่มโปรแกรม: สร้างหน้าต่างและเริ่มเกม */
    public static void main(String[] args) {
        // สร้าง UI บน Event Dispatch Thread ตามหลักของ Swing
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("KaiDeng");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(false);

            KaiDeng game = new KaiDeng();
            window.add(game);
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);

            game.requestFocusInWindow();
            game.startGame();
        });
    }


    private final Object lock = new Object();       // ป้องกันสถานะฉากระหว่างเธรดเกมกับอินพุตจาก EDT
    private final Object swapLock = new Object();   // ป้องกันตอนสลับ front/back buffer
    private final UI.Backgrounds backgrounds;

    private volatile Scene current;
    private BufferedImage front, back;
    private Thread loopThread;
    private volatile boolean running;
    private volatile int fps, ups;

    KaiDeng() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        backgrounds = new UI.Backgrounds(WIDTH, HEIGHT, ARENA);
        front = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        back = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        current = new MenuScene(this);
        installInput();
    }

    // ---------- การสลับฉาก ----------
    /** สลับไปฉากอื่น (ฉากเรียกผ่าน Scene.goTo) */
    void show(Scene scene) { current = scene; }
    void exit() { System.exit(0); }
    UI.Backgrounds backgrounds() { return backgrounds; }

    // ---------- อินพุต: ส่งต่อให้ฉากปัจจุบัน ----------
    private void installInput() {
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                synchronized (lock) { current.pointerMoved(e.getX(), e.getY()); current.mousePressed(e); }
            }
            @Override public void mouseDragged(MouseEvent e) {
                synchronized (lock) { current.pointerMoved(e.getX(), e.getY()); current.mouseDragged(e); }
            }
            @Override public void mouseReleased(MouseEvent e) {
                synchronized (lock) { current.pointerMoved(e.getX(), e.getY()); current.mouseReleased(e); }
            }
            @Override public void mouseMoved(MouseEvent e) {
                boolean hand;
                synchronized (lock) { current.pointerMoved(e.getX(), e.getY()); hand = current.wantsHandCursor(); }
                setCursor(Cursor.getPredefinedCursor(hand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
            @Override public void mouseExited(MouseEvent e) {
                synchronized (lock) { current.pointerMoved(-1, -1); }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { synchronized (lock) { current.keyPressed(e); } }
            @Override public void keyReleased(KeyEvent e) { synchronized (lock) { current.keyReleased(e); } }
        });
    }

    // ---------- ลูปเกม ----------
    void startGame() {
        if (running) return;
        running = true;
        loopThread = new Thread(this::loop, "game-loop");
        loopThread.setDaemon(true);
        loopThread.start();
    }

    void stopGame() { running = false; }

    int fps() { return fps; }
    int ups() { return ups; }
    Scene scene() { return current; }

    /** อัปเดตตรรกะหนึ่ง tick (แยกเป็นเมธอดเพื่อให้ทดสอบเรียกเองได้) */
    void update() { current.update(); }

    /**
     * Fixed timestep: อัปเดตตรง 60 ครั้ง/วินาทีตามตารางเวลา จึงไม่กระตุกแบบ 0 ครั้ง/2 ครั้งสลับกัน
     * ถ้าเครื่องช้าจนตามหลังจะข้ามการวาดเพื่อไล่ให้ทัน
     */
    private void loop() {
        long next = System.nanoTime();
        long statTime = next;
        int frames = 0, updates = 0;

        while (running) {
            synchronized (lock) { update(); }
            updates++;
            next += FRAME_NANOS;

            long now = System.nanoTime();
            if (now - next > 5 * FRAME_NANOS) next = now;       // ค้างนาน (เช่นเครื่องหน่วง) ไม่ต้องไล่ย้อนหลัง

            if (now <= next) {                                  // ทันตารางเวลา → วาดแล้วรอเฟรมถัดไป
                synchronized (lock) { renderToBuffer(); }
                repaint();
                frames++;
                waitUntil(next);
            }

            long t = System.nanoTime();
            if (t - statTime >= 1_000_000_000L) {
                fps = frames;
                ups = updates;
                frames = 0;
                updates = 0;
                statTime = t;
            }
        }
    }

    /** park เกือบทั้งหมด แล้ว spin ช่วง ~1 ms สุดท้ายเพื่อความแม่น */
    private static void waitUntil(long deadline) {
        long remaining;
        while ((remaining = deadline - System.nanoTime()) > 0) {
            if (remaining > 2_000_000L) LockSupport.parkNanos(remaining - 1_000_000L);
            else Thread.onSpinWait();
        }
    }

    // ---------- การวาด ----------
    private void renderToBuffer() {
        Graphics2D g = back.createGraphics();
        try {
            // ปิด anti-alias ทั้งหมดให้ขอบคมแบบพิกเซล (สไตล์ Stardew)
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            renderFrame(g);
        } finally {
            g.dispose();
        }
        synchronized (swapLock) {
            BufferedImage tmp = front;
            front = back;
            back = tmp;
        }
    }

    void renderFrame(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        current.render(g);
    }

    /** EDT แค่ copy ภาพที่เธรดเกมวาดเสร็จแล้วขึ้นจอ */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        synchronized (swapLock) {
            g.drawImage(front, 0, 0, null);
        }
        Toolkit.getDefaultToolkit().sync();
    }
}
