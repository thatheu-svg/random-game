import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// The upgrade menu: picks three random upgrades, draws them as cards, and finds which card is under the mouse.
// It doesn't know about the panel, so the panel's width and height are passed in when needed.
public class UpgradeMenu {
    private static final int MENU_WIDTH = 960;
    private static final int MENU_HEIGHT = 540;
    private static final int MENU_PAD = 24;
    private static final int CARD_GAP = 20;
    private static final int MENU_TITLE_SPACE = 60;
    private static final int CARD_IMAGE_SIZE = 150;

    private final List<Upgrade> choices = new ArrayList<>();
    private int hoveredCard = -1;

    // Picks up to 3 random available upgrades. Returns false (and stays closed) if none are available.
    public boolean open(List<Upgrade> allUpgrades, Random random) {
        List<Upgrade> available = new ArrayList<>();
        for (Upgrade upgrade : allUpgrades) {
            if (upgrade.isAvailable()) {
                available.add(upgrade);
            }
        }
        if (available.isEmpty()) return false;

        Collections.shuffle(available, random);
        choices.clear();
        choices.addAll(available.subList(0, Math.min(3, available.size())));
        return true;
    }

    // The upgrade on card index, or null if there is no such card
    public Upgrade get(int index) {
        if (index < 0 || index >= choices.size()) return null;
        return choices.get(index);
    }

    public void clearHover() {
        hoveredCard = -1;
    }

    // Updates which card is hovered. Returns true if it changed (so the caller knows to repaint).
    public boolean updateHover(int mouseX, int mouseY, int panelWidth, int panelHeight) {
        int card = cardAt(mouseX, mouseY, panelWidth, panelHeight);
        if (card == hoveredCard) return false;
        hoveredCard = card;
        return true;
    }

    // Where card i sits on screen (used for both drawing and mouse clicks)
    private Rectangle cardBounds(int i, int panelWidth, int panelHeight) {
        int menuLeft = (panelWidth - MENU_WIDTH) / 2;
        int menuTop = (panelHeight - MENU_HEIGHT) / 2;
        int cardWidth = (MENU_WIDTH - 2 * MENU_PAD - 2 * CARD_GAP) / 3;
        int cardHeight = MENU_HEIGHT - 2 * MENU_PAD - MENU_TITLE_SPACE;
        return new Rectangle(menuLeft + MENU_PAD + i * (cardWidth + CARD_GAP),
                menuTop + MENU_PAD + MENU_TITLE_SPACE, cardWidth, cardHeight);
    }

    // Which card is under (x, y), or -1 if none
    public int cardAt(int x, int y, int panelWidth, int panelHeight) {
        for (int i = 0; i < choices.size(); i++) {
            if (cardBounds(i, panelWidth, panelHeight).contains(x, y)) return i;
        }
        return -1;
    }

    // Draws text wrapped to maxWidth, returns the y for the next line
    private int drawWrapped(Graphics graphics, String text, int x, int y, int maxWidth) {
        FontMetrics metrics = graphics.getFontMetrics();
        String line = "";
        for (String word : text.split(" ")) {
            String attempt = line.isEmpty() ? word : line + " " + word;
            if (metrics.stringWidth(attempt) > maxWidth && !line.isEmpty()) {
                graphics.drawString(line, x, y);
                y += metrics.getHeight();
                line = word;
            } else {
                line = attempt;
            }
        }
        if (!line.isEmpty()) {
            graphics.drawString(line, x, y);
            y += metrics.getHeight();
        }
        return y;
    }

    // Three cards, each with an image, a name, and a description
    public void draw(Graphics graphics, int panelWidth, int panelHeight) {
        int left = (panelWidth - MENU_WIDTH) / 2;
        int top = (panelHeight - MENU_HEIGHT) / 2;

        graphics.setColor(Color.GRAY);
        graphics.fillRect(left, top, MENU_WIDTH, MENU_HEIGHT);

        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Arial", Font.BOLD, 32));
        graphics.drawString("Choose an upgrade", left + MENU_PAD, top + MENU_PAD + 32);

        for (int i = 0; i < choices.size(); i++) {
            Upgrade upgrade = choices.get(i);
            Rectangle card = cardBounds(i, panelWidth, panelHeight);
            boolean hovered = (i == hoveredCard);

            graphics.setColor(hovered ? new Color(75, 75, 100) : new Color(50, 50, 60));
            graphics.fillRect(card.x, card.y, card.width, card.height);
            graphics.setColor(hovered ? Color.YELLOW : Color.WHITE);
            graphics.drawRect(card.x, card.y, card.width, card.height);

            // image (placeholder box until the upgrade supplies one)
            int imageLeft = card.x + (card.width - CARD_IMAGE_SIZE) / 2;
            int imageTop = card.y + 16;
            BufferedImage image = upgrade.getImage();
            if (image != null) {
                graphics.drawImage(image, imageLeft, imageTop, CARD_IMAGE_SIZE, CARD_IMAGE_SIZE, null);
            } else {
                graphics.setColor(new Color(30, 30, 38));
                graphics.fillRect(imageLeft, imageTop, CARD_IMAGE_SIZE, CARD_IMAGE_SIZE);
                graphics.setColor(Color.GRAY);
                graphics.drawRect(imageLeft, imageTop, CARD_IMAGE_SIZE, CARD_IMAGE_SIZE);
            }

            int textLeft = card.x + 16;
            int textWidth = card.width - 32;
            int y = imageTop + CARD_IMAGE_SIZE + 36;

            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("Arial", Font.BOLD, 24));
            y = drawWrapped(graphics, upgrade.getName(), textLeft, y, textWidth);

            graphics.setColor(new Color(220, 220, 220));
            graphics.setFont(new Font("Arial", Font.PLAIN, 18));
            drawWrapped(graphics, upgrade.getDescription(), textLeft, y + 6, textWidth);

            graphics.setColor(Color.YELLOW);
            graphics.setFont(new Font("Arial", Font.BOLD, 16));
            graphics.drawString("Click or press " + (i + 1), textLeft, card.y + card.height - 14);
        }
    }
}