// A damaging puddle on the ground. Placed by clicking, or dropped as a trail during the super.
public class Puddle {
    static final int MAX_AGE = 300;   // puddle lifetime in frames

    double x;
    double y;
    int radius;
    int damage;
    int age;
    int delay;
    boolean madeBySuper = false;

    Puddle(double x, double y, int radius, int damage, int age, int delay) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.damage = damage;
        this.age = age;
        this.delay = delay;
    }

    public boolean isExpired() {
        return age >= MAX_AGE;
    }

    public boolean isTouchingPlayer(double playerCenterX, double playerCenterY, double playerSize) {
        return Math.hypot(x - playerCenterX, y - playerCenterY) <= radius + playerSize / 2.0;
    }

    public boolean madeBySuper() {
        return madeBySuper;
    }
}