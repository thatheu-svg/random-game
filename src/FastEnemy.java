import java.awt.Color;

public class FastEnemy extends Enemy {
    public FastEnemy(double x, double y, double speed, int hp, int damage) {
        super(x, y, speed * 1.5, (int) (hp * 0.6), 40, damage, new Color(173, 216, 255), new Color (255, 255, 255));
        setImages("images/fastenemy.png");
        
    }
}
