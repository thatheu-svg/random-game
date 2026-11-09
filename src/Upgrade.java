import java.awt.image.BufferedImage;

public interface Upgrade {
    String getName();
    void apply();

    default String getDescription() { return ""; }
    default BufferedImage getImage() { return null; }   // return a loaded sprite later; null shows a placeholder
    default boolean isAvailable() { return true; }
}