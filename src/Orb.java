// An XP orb dropped by a dead enemy. It drifts toward the player when close enough.
// Player (not the orb) decides what collecting it does.
public class Orb {
    double orbX;
    double orbY;
    double speed;
    int radius;
    int xp;
    boolean collected;

    Orb(double x, double y, double speed, int radius, int xp, boolean collected) {
        this.orbX = x;
        this.orbY = y;
        this.speed = speed;
        this.radius = radius;
        this.xp = xp;
        this.collected = collected;
    }

    // Moves toward the target if it is within pullRange
    public void moveToward(double targetX, double targetY, double pullRange) {
        double dx = targetX - orbX;
        double dy = targetY - orbY;
        double distance = Math.hypot(dx, dy);

        if (distance < 1) return;
        if (distance > pullRange) return;

        double step = Math.min(speed, distance);   // don't overshoot the player
        orbX += dx / distance * step;
        orbY += dy / distance * step;
    }

    public boolean isTouchingPlayer(double playerCenterX, double playerCenterY, double playerSize) {
        double dx = orbX - playerCenterX;
        double dy = orbY - playerCenterY;
        return Math.hypot(dx, dy) <= radius / 2.0 + playerSize / 2.0;
    }
}