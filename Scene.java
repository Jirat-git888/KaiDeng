import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * หนึ่งหน้าจอของเกม (State pattern): เมนู, เลือกตัวละคร, เดินสำรวจ, ต่อสู้, จบเกม
 * ตัวเกมส่งต่ออินพุตและคำสั่ง update/render ให้ฉากปัจจุบันโดยไม่ต้องรู้ว่าเป็นฉากอะไร
 *
 * คลาสฐานนี้เก็บตัวนับเฟรมไว้ทำแอนิเมชันและตำแหน่งเมาส์ล่าสุด
 * update() เป็น template method: นับเฟรมแล้วเรียก updateScene() ของฉากลูก
 */
abstract class Scene {
    private final KaiDeng game;
    private int ticks;
    private int mouseX = -1, mouseY = -1;

    protected Scene(KaiDeng game) { this.game = game; }

    public final void update() {
        ticks++;
        updateScene();
    }

    protected void updateScene() { }
    public abstract void render(Graphics2D g);

    public void pointerMoved(int x, int y) { mouseX = x; mouseY = y; }
    public void mousePressed(MouseEvent e) { }
    public void mouseDragged(MouseEvent e) { }
    public void mouseReleased(MouseEvent e) { }
    public void keyPressed(KeyEvent e) { }
    public void keyReleased(KeyEvent e) { }

    /** เมาส์อยู่เหนือสิ่งที่กดได้หรือไม่ (ใช้เปลี่ยนเคอร์เซอร์เป็นรูปมือ) */
    public boolean wantsHandCursor() { return false; }

    protected final int ticks() { return ticks; }
    protected final int mouseX() { return mouseX; }
    protected final int mouseY() { return mouseY; }
    protected final KaiDeng game() { return game; }
    protected final UI.Backgrounds backgrounds() { return game.backgrounds(); }

    /** สลับไปฉากอื่น */
    protected final void goTo(Scene next) { game.show(next); }
}
