import java.awt.Color;

/** อึด: สกิลทำให้ตัวใหญ่ขึ้นและ ATK x2 */
class TankyChicken extends HeroChicken {
    public TankyChicken() {
        super("Tanky", 200, 10, 3.0, new Color(255, 140, 0), "tanky.png", "skill_tanky.png", 4);
    }

    @Override public HeroChicken cloneHero() { return new TankyChicken(); }
    @Override public String role() { return "Heavy hitter"; }
    @Override public String skillName() { return "Titan form"; }
    @Override public String skillDesc() { return "Grows huge and\nhits twice as\nhard this turn."; }

    @Override protected void applySkillEffect() {
        x -= 20;
        y -= 20;
        size = 70;
        attack *= 2;
    }

    @Override protected void removeSkillEffect() {
        x += 20;
        y += 20;
        size = 30;
        attack /= 2;
    }
}
