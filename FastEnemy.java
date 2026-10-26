import java.awt.Color;

public class FastEnemy extends Enemy {
    public FastEnemy(double x, double y, double speed) {
        super(x, y, speed * 1.5, 60, 35, 30, new Color(173, 216, 230));
    }
}
