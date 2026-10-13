import java.awt.Rectangle;
import java.util.ArrayList;

/**
 * จำลองฟิสิกส์ล่วงหน้าเพื่อวาดเส้นเล็ง ใช้กติกาเดียวกับ Chicken
 * (updatePhysics → handleObstacles → handlePortals ตามลำดับเดียวกับเกมจริง)
 *
 * ผลลัพธ์แบ่งเป็นหลายเส้น (segment) เพราะเมื่อวาร์ปเส้นจะขาดจากกัน
 */
class AimPredictor {
    private static final int MAX_STEPS = 300;

    static class Result {
        /** เส้นทางแต่ละช่วง (เริ่มช่วงใหม่ทุกครั้งที่วาร์ป) แต่ละช่วงมีอย่างน้อย 1 จุด */
        final ArrayList<ArrayList<double[]>> segments = new ArrayList<>();
        /** ทิศที่ไก่เคลื่อนที่ในก้าวสุดท้าย ใช้หมุนหัวลูกศร */
        double endAngle;
        /** หลุมที่ไก่โผล่ออกมาในแต่ละช่วง ลำดับตรงกับ segments[1..] */
        final ArrayList<Portal> exitPortals = new ArrayList<>();
    }

    static Result predict(HeroChicken hero, double vx, double vy, Rectangle arena,
                          ArrayList<Obstacle> obstacles, ArrayList<Portal> portals) {
        Result result = new Result();
        ArrayList<double[]> current = new ArrayList<>();
        result.segments.add(current);

        double x = hero.x, y = hero.y;
        int size = hero.size;
        // ต้องเริ่มจากสถานะล็อกหลุมของไก่จริง ไม่งั้นไก่ที่จอดทับหลุมปลายทางอยู่
        // จะถูกทำนายว่าโดนวาร์ปกลับทันที ทั้งที่ในเกมจริงไม่โดน
        Portal lastWarped = hero.lastWarpedPortal;

        double moveVx = vx, moveVy = vy;
        result.endAngle = Math.atan2(vy, vx);
        current.add(new double[]{x + size / 2.0, y + size / 2.0});

        for (int step = 0; step < MAX_STEPS; step++) {
            moveVx = vx;
            moveVy = vy;
            x += vx;
            y += vy;
            vx *= 0.94;
            vy *= 0.94;
            if (Math.abs(vx) < 0.2) vx = 0;
            if (Math.abs(vy) < 0.2) vy = 0;

            // ขอบสนาม
            if (x <= arena.x)                       { x = arena.x;                        vx *= -1; }
            if (x + size >= arena.x + arena.width)  { x = arena.x + arena.width - size;   vx *= -1; }
            if (y <= arena.y)                       { y = arena.y;                        vy *= -1; }
            if (y + size >= arena.y + arena.height) { y = arena.y + arena.height - size;  vy *= -1; }

            // สิ่งกีดขวาง
            for (Obstacle obs : obstacles) {
                double dx = (x + size / 2.0) - obs.centerX();
                double dy = (y + size / 2.0) - obs.centerY();
                double dist = Math.hypot(dx, dy);
                double minDist = (size + obs.size) / 2.0;
                if (dist >= minDist) continue;

                if (dist == 0) { dx = 1; dy = 0; dist = 1; }
                double nx = dx / dist, ny = dy / dist;
                double dot = vx * nx + vy * ny;
                if (dot < 0) {
                    vx = vx - 2 * dot * nx;
                    vy = vy - 2 * dot * ny;
                }
                double overlap = minDist - dist;
                x += nx * overlap;
                y += ny * overlap;
            }

            // หลุมวาร์ป
            if (lastWarped != null && !overlaps(x, y, size, lastWarped)) lastWarped = null;
            for (Portal p : portals) {
                if (p == lastWarped) continue;
                if (p.linkedPortal != null && overlaps(x, y, size, p)) {
                    current.add(new double[]{x + size / 2.0, y + size / 2.0}); // ให้เส้นเข้าไปถึงปากหลุม
                    x = p.linkedPortal.x + (p.linkedPortal.size - size) / 2.0;
                    y = p.linkedPortal.y + (p.linkedPortal.size - size) / 2.0;
                    lastWarped = p.linkedPortal;
                    result.exitPortals.add(p.linkedPortal);
                    current = new ArrayList<>();                                // เริ่มเส้นใหม่ที่ปลายทาง
                    result.segments.add(current);
                    break;
                }
            }

            current.add(new double[]{x + size / 2.0, y + size / 2.0});
            if (vx == 0 && vy == 0) break;
        }

        if (Math.hypot(moveVx, moveVy) > 0) result.endAngle = Math.atan2(moveVy, moveVx);
        return result;
    }

    private static boolean overlaps(double x, double y, int size, Portal p) {
        double dx = (x + size / 2.0) - p.centerX();
        double dy = (y + size / 2.0) - p.centerY();
        return Math.hypot(dx, dy) < (size + p.size) / 2.0;
    }
}
