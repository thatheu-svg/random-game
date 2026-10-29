import java.awt.Color;

public class TankEnemy extends Enemy {
    public TankEnemy(double x, double y, double speed) {
        super(x, y, speed * 0.8, 160, 60, 50, new Color(150, 150, 150), new Color(255, 255, 255));
        setIcon("tankenemy.png");
    }
}