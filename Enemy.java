import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class Enemy {

    private static final long FLASH_MS = 120;
    private static final double ICON_FRACTION = 0.6;   // icon size as a fraction of the circle's inner diameter

    // Each icon file is loaded once and shared by every enemy that uses it
    private static final Map<String, BufferedImage> iconCache = new HashMap<>();

    private double x;
    private double y;
    private final double speed;
    private int hp;
    private final int maxHp;
    private final int size;
    final int damage;
    private final Color color;
    private final Color outlineColor;
    private BufferedImage icon;          // null means no icon
    private long lastHitTime = 0;

    public Enemy(double x, double y, double speed, int startingHp, int size, int damage, Color color, Color outlineColor) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.hp = startingHp;
        this.maxHp = startingHp;
        this.size = size;
        this.damage = damage;
        this.color = color;
        this.outlineColor = outlineColor;
    }

    // Subclasses call this from their constructor to pick an icon
    protected final void setIcon(String path) {
        this.icon = loadIcon(path);
    }

    private static BufferedImage loadIcon(String path) {
        if (iconCache.containsKey(path)) {
            return iconCache.get(path);
        }
        BufferedImage image = null;
        File file = new File(path);
        try {
            image = ImageIO.read(file);
        } catch (IOException e) {
            System.err.println("Could not load icon: " + file.getAbsolutePath());
        }
        iconCache.put(path, image);   // remembers failures too, so the error prints once
        return image;
    }

    public int getDamage() {
        return damage;
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
        } else {
            graphics.setColor(outlineColor);
            graphics.fillOval((int) x, (int) y, size, size);

            graphics.setColor(color);
            graphics.fillOval((int) (x + 2.5), (int) (y + 2.5), size - 5, size - 5);
        }

        drawIcon(graphics);
        drawHealthBar(graphics);
    }

    // Draws the icon centered in the circle, scaled to fit without changing its proportions
    private void drawIcon(Graphics graphics) {
        if (icon == null) return;

        double box = (size - 5) * ICON_FRACTION;
        double scale = Math.min(box / icon.getWidth(), box / icon.getHeight());
        int width = (int) (icon.getWidth() * scale);
        int height = (int) (icon.getHeight() * scale);
        int left = (int) (x + size / 2.0 - width / 2.0);
        int top = (int) (y + size / 2.0 - height / 2.0);

        graphics.drawImage(icon, left, top, width, height, null);
    }

    // Red bar for missing health, green bar on top for remaining health, with the numbers above
    private void drawHealthBar(Graphics graphics) {
        int barWidth = size + 20;
        int barLeft = (int) x - 10;

        graphics.setColor(Color.RED);
        graphics.fillRect(barLeft, (int) y - 10, barWidth, 5);

        graphics.setColor(new Color(60, 180, 75));
        graphics.fillRect(barLeft, (int) y - 10, (int) (barWidth * Math.max(hp, 0) / (double) maxHp), 5);

        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Arial", Font.BOLD, 10));
        graphics.drawString(" " + Math.max(hp, 0) + " / " + maxHp, barLeft + 8, (int) y - 11);
    }
}