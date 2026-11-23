import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class DeadEnemy {
    public static final int MAX_DURATION = 30;

    private double x;
    private double y;
    private int size;
    int duration;
    private BufferedImage deathAnimationOne;
    private BufferedImage deathAnimationTwo;
    private BufferedImage deathAnimationThree;

    public DeadEnemy(double deathX, double deathY, int size, int duration) {
        this.x = deathX;
        this.y = deathY;
        this.size = size;
        this.duration = duration;
        this.deathAnimationOne = Enemy.loadImages("images/smoke1.png");
        this.deathAnimationTwo = Enemy.loadImages("images/smoke2.png");
        this.deathAnimationThree = Enemy.loadImages("images/smoke3.png");
    }

    public boolean durationExpired() {
        return duration >= 30;
    }

    public void tickDuration() {
        duration++;
    }

    public void drawDeathAnimation(Graphics graphics) {
        double box = size;
        double scale = Math.min(box / deathAnimationOne.getWidth(), box / deathAnimationOne.getHeight());
        int width = (int) (deathAnimationOne.getWidth() * scale);
        int height = (int) (deathAnimationOne.getHeight() * scale);
        int left = (int) (x + size / 2.0 - width / 2.0);
        int top = (int) (y + size / 2.0 - height / 2.0);
        
        if (duration >= 20) {
            graphics.drawImage(deathAnimationThree, left, top, width, height, null);
        }

        else if (duration >= 10) {
            graphics.drawImage(deathAnimationTwo, left, top, width, height, null);
        }

        else if (duration >= 0) {
            graphics.drawImage(deathAnimationOne, left, top, width, height, null);
        }
    }
}