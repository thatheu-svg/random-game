import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

// The aura ability unlocked at the kill milestone: it follows the player, slows enemies inside it,
// and hits them on a schedule (harder for each earlier hit on the same enemy).
public class Aura {
    private static final int FRAMES_PER_SECOND = 60;
    private static final int RADIUS = 250;
    private static final int DURATION_FRAMES = 5 * FRAMES_PER_SECOND;
    private static final int FRAMES_PER_HIT = 30;         // frames between aura damage pulses
    private static final int BASE_DAMAGE = 10;
    private static final double DAMAGE_PER_HIT = 0.2;     // extra +20% of base damage per earlier hit on that enemy
    private static final int PULSE_FRAMES = 3 * FRAMES_PER_SECOND;   // one pulse ring grows from the player to the aura's edge in this long

    private boolean unlocked = false;
    private boolean active = false;
    private int framesLeft = 0;
    private int cooldown = 0;                             // counts DOWN to 0
    private int pulseFrames = 0;

    public boolean isUnlocked() {
        return unlocked;
    }

    public void unlock() {
        unlocked = true;
    }

    // Starts the aura if it is unlocked and off cooldown
    public void cast() {
        if (!unlocked || active || cooldown > 0) return;
        active = true;
        framesLeft = DURATION_FRAMES;
        pulseFrames = 0;
    }

    // Counts the aura down while it is on, then counts its cooldown down
    public void updateTimers(int cooldownFrames) {
        if (active) {
            pulseFrames = (pulseFrames + 1) % PULSE_FRAMES;   // wraps back to 0, so the pulse restarts
            framesLeft--;
            if (framesLeft <= 0) {
                active = false;
                cooldown = cooldownFrames;
            }
        } else if (cooldown > 0) {
            cooldown--;
        }
    }

    // Enemies inside the aura are slowed and hit on a schedule.
    // Enemies outside it go back to normal speed and lose their hit count.
    public void affectEnemies(List<Enemy> enemies, double playerX, double playerY, double playerSize,
                              int frameCount, double slowMultiplier) {
        for (Enemy enemy : enemies) {
            if (active && enemy.isInAura(playerX, playerY, playerSize, RADIUS)) {
                enemy.setSpeedMultiplier(slowMultiplier);
                if (frameCount % FRAMES_PER_HIT == 0) {
                    enemy.addAuraHit();
                    double scaled = BASE_DAMAGE * (1 + enemy.getAuraHits() * DAMAGE_PER_HIT);
                    enemy.takeDamage((int) scaled);
                }
            } else {
                enemy.setSpeedMultiplier(1.0);
                enemy.resetAuraHits();
            }
        }
    }

    public void reset() {
        unlocked = false;
        active = false;
        framesLeft = 0;
        cooldown = 0;
        pulseFrames = 0;
    }

    // Translucent circle around the player while the aura is on
    public void draw(Graphics graphics, double playerX, double playerY, double playerSize) {
        if (!active) return;
        Graphics2D g2 = (Graphics2D) graphics;
        int centerX = (int) (playerX + playerSize / 2.0);
        int centerY = (int) (playerY + playerSize / 2.0);

        g2.setColor(new Color(160, 80, 255, 60));
        g2.fillOval(centerX - RADIUS, centerY - RADIUS, RADIUS * 2, RADIUS * 2);
        g2.setColor(new Color(190, 120, 255, 200));
        g2.setStroke(new BasicStroke(3));
        g2.drawOval(centerX - RADIUS, centerY - RADIUS, RADIUS * 2, RADIUS * 2);

        // thin blue pulse: grows from the player out to the aura's edge, fading as it goes
        double progress = pulseFrames / (double) PULSE_FRAMES;
        int pulseRadius = (int) (RADIUS * progress);
        int alpha = (int) (255 * (1.0 - progress));
        g2.setColor(new Color(90, 170, 255, Math.max(alpha, 0)));
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(centerX - pulseRadius, centerY - pulseRadius, pulseRadius * 2, pulseRadius * 2);
        g2.setStroke(new BasicStroke(1));
    }

    // Status text under the kill counter
    public void drawStatus(Graphics graphics) {
        if (!unlocked) return;
        graphics.setFont(new Font("Arial", Font.BOLD, 24));
        if (active) {
            graphics.setColor(new Color(190, 120, 255));
            graphics.drawString("Aura: " + (framesLeft / FRAMES_PER_SECOND + 1) + "s left", 6, 102);
        } else if (cooldown > 0) {
            graphics.setColor(Color.GRAY);
            graphics.drawString("Aura recharging: " + (cooldown / FRAMES_PER_SECOND + 1) + "s", 6, 102);
        } else {
            graphics.setColor(Color.YELLOW);
            graphics.drawString("Aura ready: press E", 6, 102);
        }
    }
}