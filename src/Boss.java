import java.awt.Color;

public class Boss extends Enemy {
    public Boss(double x, double y, double speed, int hp, int damage) {
        super(x, y, speed * 0.4, hp * 5, 80, (int) (damage * 2), new Color(0, 0, 0), new Color (255, 255, 255));
        setImages("images/miniboss.png");
        
    }
}
