import java.awt.Color;
import java.awt.Graphics;

public class Enemy {
    private static final int SIZE = 40;
    private static final int DAMAGE = 20;
    private static final Color COLOR = new Color(255, 255, 255);

    private double x;
    private double y;
    private final double speed;
    private int hp;
    private final int maxHp;
    private final int size;
    private final int damage;
    private final Color color;
    private long lastHitTime = 0;
    private static final long FLASH_MS = 120;

    public Enemy(double x, double y, double speed, int startingHp, int size, int damage, Color color) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.hp = startingHp;
        this.maxHp = startingHp;
        this.size = size;
        this.damage = damage;
        this.color = color;
    }

    public void moveToward(double targetX, double targetY) {
        double dx = targetX - (x + size / 2.0);
        double dy = targetY - (y + size / 2.0);
        double distance = Math.hypot(dx, dy);

        if (distance < 1) return;

        x += dx / distance * speed;
        y += dy / distance * speed;
    }
    
    public boolean isDead() {
        return hp <= 0;
    }

    public boolean isInPuddle(double puddleX, double puddleY, double radius) {
        double dx = (x + size / 2.0) - puddleX;
        double dy = (y + size / 2.0) - puddleY;
        return Math.hypot(dx, dy) <= radius;
    }
    public boolean isTouchingPlayer(double playerX, double playerY, double playerSize) {
        double dx = (x + size / 2.0) - (playerX + playerSize / 2.0);
        double dy = (y + size / 2.0) - (playerY + playerSize / 2.0);
        return Math.hypot(dx, dy) <= size / 2.0 + playerSize / 2.0;
    }
    public void takeDamage(int damage) {
        hp = hp - damage;
        lastHitTime = System.currentTimeMillis();
    }

    public void draw(Graphics graphics) {
        if (System.currentTimeMillis() - lastHitTime < FLASH_MS) {
            graphics.setColor(Color.RED);
            graphics.fillOval((int) x, (int) y, size, size);
        }
        else {
            graphics.setColor(color);
            graphics.fillOval((int) x, (int) y, size, size);
        }
        // health bar: grey background, green fill on top
        int barWidth = size + 20;
        int barLeft = (int) x - 10;

        graphics.setColor(Color.RED);
        graphics.fillRect(barLeft, (int) y - 10, barWidth, 5);

        graphics.setColor(new Color(60, 180, 75));
        graphics.fillRect(barLeft, (int) y - 10, (int) (barWidth * Math.max(hp, 0) / (double) maxHp), 5);
    }
}

    