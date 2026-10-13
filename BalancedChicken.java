import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;

/** สมดุล: สกิลวางระเบิดไข่ตามทางที่พุ่งผ่าน */
class BalancedChicken extends HeroChicken {
    private double lastMineX, lastMineY;

    public BalancedChicken() {
        super("Balanced", 150, 12, 4.5, new Color(220, 220, 220), "balanced.png", "skill_balanced.png", 3);
    }

    @Override public HeroChicken cloneHero() { return new BalancedChicken(); }
    @Override public String role() { return "All-rounder"; }
    @Override public String skillName() { return "Egg trail"; }
    @Override public String skillDesc() { return "Drops egg mines\nas it moves.\n25 damage each."; }

    @Override protected void applySkillEffect() {
        lastMineX = x;
        lastMineY = y;
    }

    @Override protected void removeSkillEffect() { }

    @Override
    public void updatePhysics(Rectangle arena, ArrayList<EggMine> mines) {
        super.updatePhysics(arena, mines);
        if (skillActive && isMoving()) {
            if (Math.hypot(x - lastMineX, y - lastMineY) > 70) {
                mines.add(new EggMine(centerX() - 10, centerY() - 10));
                lastMineX = x;
                lastMineY = y;
            }
        }
    }
}
