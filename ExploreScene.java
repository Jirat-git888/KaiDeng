import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/** เดินสำรวจทุ่งหญ้า (WASD) ไก่ในทีมเดินตาม เดินชนบอสเพื่อเริ่มต่อสู้ */
final class ExploreScene extends Scene {
    private static final int PLAYER_SPEED = 4;
    private static final int PLAYER_SIZE = 30;

    private final List<HeroChicken> heroes = new ArrayList<>();
    private final BossChicken boss = new BossChicken(KaiDeng.BOSS_EXPLORE_SPAWN.x, KaiDeng.BOSS_EXPLORE_SPAWN.y);
    private int playerX = KaiDeng.PLAYER_START.x, playerY = KaiDeng.PLAYER_START.y;
    private boolean up, down, left, right;

    ExploreScene(KaiDeng game, List<HeroChicken.Type> team) {
        super(game);
        for (int i = 0; i < team.size(); i++) {
            HeroChicken hero = team.get(i).create();
            hero.setPosition(playerX - 40 * (i + 1), playerY);   // เริ่มข้างๆ ผู้เล่น
            heroes.add(hero);
        }
    }

    @Override
    protected void updateScene() {
        if (up)    playerY -= PLAYER_SPEED;
        if (down)  playerY += PLAYER_SPEED;
        if (left)  playerX -= PLAYER_SPEED;
        if (right) playerX += PLAYER_SPEED;
        playerX = Math.max(0, Math.min(KaiDeng.WIDTH - PLAYER_SIZE, playerX));
        playerY = Math.max(0, Math.min(KaiDeng.HEIGHT - PLAYER_SIZE, playerY));

        for (int i = 0; i < heroes.size(); i++) {
            if (i == 0) heroes.get(0).follow(playerX, playerY);
            else heroes.get(i).follow(heroes.get(0).x(), heroes.get(0).y());
        }
        if (boss.isNear(playerX, playerY, 40)) goTo(new BattleScene(game(), heroes, boss));
    }

    @Override
    public void keyPressed(KeyEvent e) { setKey(e.getKeyCode(), true); }

    @Override
    public void keyReleased(KeyEvent e) { setKey(e.getKeyCode(), false); }

    private void setKey(int code, boolean pressed) {
        if (code == KeyEvent.VK_W) up = pressed;
        if (code == KeyEvent.VK_S) down = pressed;
        if (code == KeyEvent.VK_A) left = pressed;
        if (code == KeyEvent.VK_D) right = pressed;
    }

    @Override
    public void render(Graphics2D g) {
        g.drawImage(backgrounds().grass(), 0, 0, null);

        UI.shadowEllipse(g, boss.centerX(), boss.y() + boss.size() * 0.92, boss.size() * 0.85, 12, 90);
        Image sprite = boss.sprite(boss.size());
        if (sprite != null) {
            g.drawImage(sprite, (int) boss.x(), (int) boss.y(), null);
        } else {
            g.setColor(UI.DARK);
            g.fillOval((int) boss.x() - 2, (int) boss.y() - 2, boss.size() + 4, boss.size() + 4);
            g.setColor(boss.getColor());
            g.fillOval((int) boss.x(), (int) boss.y(), boss.size(), boss.size());
        }
        boolean near = boss.isNear(playerX + 15, playerY + 15, 130);
        UI.centerOutlined(g, near ? "Fight!" : "Void Boss", (int) boss.centerX(), (int) boss.y() - 12,
                UI.BODY, near ? UI.YELLOW : UI.CREAM, UI.DARK);

        for (HeroChicken h : heroes) h.draw(g);
        drawPlayer(g);

        UI.plate(g, 290, 548, 220, 32);
        UI.center(g, "WASD to move", KaiDeng.WIDTH / 2, 570, UI.BODY, UI.DARK);
    }

    private void drawPlayer(Graphics2D g) {
        int px = playerX, py = playerY;
        UI.shadowEllipse(g, px + 15, py + 29, 26, 8, 90);
        g.setColor(UI.DARK);
        g.fillOval(px - 2, py - 2, 34, 34);
        g.setColor(UI.BLUE);
        g.fillOval(px, py, 30, 30);
        int bob = (ticks() / 15) % 2 == 0 ? 0 : 2;               // ลูกศรชี้ตัวผู้เล่น
        int ay = py - 16 + bob;
        g.setColor(UI.DARK);
        g.fillPolygon(new int[]{px + 7, px + 23, px + 15}, new int[]{ay - 2, ay - 2, ay + 10}, 3);
        g.setColor(UI.CREAM);
        g.fillPolygon(new int[]{px + 10, px + 20, px + 15}, new int[]{ay, ay, ay + 7}, 3);
    }
}
