import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

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

    // Puddles are centered on their position, so the corner is one radius up and left
    public void draw(Graphics graphics, BufferedImage sprite) {
        int left = (int) (x - radius);
        int top = (int) (y - radius);
        int diameter = radius * 2;

        if (delay > 0) {
            // still in the air: draw a warning outline
            graphics.setColor(new Color(120, 180, 255));
            graphics.drawOval(left, top, diameter, diameter);
        } else if (sprite != null) {
            graphics.drawImage(sprite, left, top, diameter, diameter, null);
        } else {
            graphics.setColor(new Color(60, 120, 255, 120));
            graphics.fillOval(left, top, diameter, diameter);
        }
    }
}