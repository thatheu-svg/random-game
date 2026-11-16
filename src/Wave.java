import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

// The expanding ring that bursts out from the player at the kill milestone.
// Purely visual: Player decides what the wave kills.
public class Wave {
    private static final int SPEED = 40;   // pixels the ring grows per frame

    private boolean active = false;
    private double radius = 0;
    private double centerX = 0;
    private double centerY = 0;

    public void start(double centerX, double centerY) {
        this.centerX = centerX;
        this.centerY = centerY;
        radius = 0;
        active = true;
    }

    // Grows the ring until it is past the whole screen
    public void update(int panelWidth, int panelHeight) {
        if (!active) return;
        radius += SPEED;
        if (radius > Math.hypot(panelWidth, panelHeight)) {
            active = false;
        }
    }

    public void reset() {
        active = false;
        radius = 0;
    }

    public void draw(Graphics graphics) {
        if (!active) return;
        Graphics2D g2 = (Graphics2D) graphics;
        int r = (int) radius;

        g2.setColor(new Color(255, 255, 255, 220));
        g2.setStroke(new BasicStroke(10));
        g2.drawOval((int) centerX - r, (int) centerY - r, r * 2, r * 2);
        g2.setColor(new Color(120, 200, 255, 150));
        g2.setStroke(new BasicStroke(24));
        g2.drawOval((int) centerX - r, (int) centerY - r, r * 2, r * 2);
        g2.setStroke(new BasicStroke(1));
    }
}