import java.awt.Color;

public class FastEnemy extends Enemy {
    public FastEnemy(double x, double y, double speed) {
        super(x, y, speed * 1.5, 60, 40, 30, new Color(173, 216, 255), new Color (255, 255, 255));
        setIcon("fastenemy.png");
    }
}
