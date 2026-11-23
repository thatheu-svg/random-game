import java.awt.Color;

public class TankEnemy extends Enemy {
    public TankEnemy(double x, double y, double speed, int hp, int damage) {
        super(x, y, speed * 0.8, (int) (hp * 1.6), 60, (int) (damage * 1.2), new Color(150, 150, 150), new Color(255, 255, 255));
        setImages("images/tankenemy.png");
    }
}