import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.*;

public class Player {

    // Program entry point: builds the window and puts the game panel inside it
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Berry Blast (man idk)");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setContentPane(new PlayerPanel());
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }

    // The drawing surface inside the window. It also runs the whole game.
    private static class PlayerPanel extends JPanel {

        // Constants

        // Screen and timing (60 frames is 1 second)
        private static final int PANEL_WIDTH = 1920;
        private static final int PANEL_HEIGHT = 1080;
        private static final int FRAME_DELAY_MS = 16;
        private static final int FRAMES_PER_SECOND = 60;

        // Player
        private static final int PLAYER_SIZE = 48;
        private static final int MOVE_AMOUNT = 8;
        private static final Color PLAYER_COLOR = new Color(128, 130, 177);  // used if the sprite fails to load
        private static final int PLAYER_MAX_HP = 100;
        private static final int CONTACT_DAMAGE = 40;        // damage per enemy touch (high for testing)
        private static final int HIT_COOLDOWN_FRAMES = 60;   // invincibility after being hit
        private static final int HEAL_COOLDOWN_FRAMES = 60;
        private static final int HP_HEALED = 20;

        // Puddles
        private static final int RADIUS_FIRST = 48;
        private static final int DAMAGE_FIRST = 20;
        private static final int MAX_AGE = 300;              // puddle lifetime in frames
        private static final int FRAMES_PER_HIT = 30;        // frames between damage pulses
        private static final int PLACE_COOLDOWN_FRAMES = 30; // wait between placing puddles
        private static final int MAX_RANGE = 500;            // max distance from player where a puddle can be placed
        private static final int MAX_DELAY_FRAMES = 60;
        private static final int MIN_DELAY_FRAMES = 10;

        // Enemy
        private static final int MAX_ENEMIES = 20;
        private static final int START_SPAWN_GAP = 300;      // 5 seconds between spawns at the start
        private static final int FASTEST_SPAWN_GAP = FRAMES_PER_SECOND;  // 1 second at max difficulty
        private static final int FRAMES_TO_MAX_DIFFICULTY = 18000;       // 5 minutes
        private static final double START_ENEMY_SPEED = 2;
        private static final double MAX_ENEMY_SPEED = 5;
        private static final int START_HP_ENEMY = 100;
        private static final int KILL_COUNT_START = 0;

        // Things in the world
        private final List<Enemy> enemies = new ArrayList<>();
        private final List<Puddle> puddles = new ArrayList<>();
        private final Set<Integer> pressedKeys = new HashSet<>();
        private final Random random = new Random();

        // Images (null if the file couldn't be loaded)
        private final BufferedImage playerSprite = loadSprite("player.png");
        private final BufferedImage puddleSprite = loadSprite("galaxy_puddle.png");

        // Player state
        private int playerX = PANEL_WIDTH / 2;
        private int playerY = PANEL_HEIGHT / 2;
        private int playerHp = PLAYER_MAX_HP;
        private int killCount = KILL_COUNT_START;

        // Timers (all counted in frames)
        private int hitCooldown = HIT_COOLDOWN_FRAMES;  // counts UP to HIT_COOLDOWN_FRAMES
        private int placeCooldown = 0;                  // counts DOWN to 0
        private int healCooldown = HEAL_COOLDOWN_FRAMES;
        private int frameCount = 0;
        private int framesUntilSpawn = START_SPAWN_GAP;

        // puddle stats
        private static class Puddle {
            double x;
            double y;
            int radius;
            int damage;
            int age;
            int delay;

            Puddle(double x, double y, int radius, int damage, int age, int delay) {
                this.x = x;
                this.y = y;
                this.radius = radius;
                this.damage = damage;
                this.age = age;
                this.delay = delay;
            }

            public boolean isExpired() {
                return age >= MAX_AGE;
            }
            public boolean isTouchingPlayer(double playerCenterX, double playerCenterY, double playerSize) {
            return Math.hypot(x - playerCenterX, y - playerCenterY) <= radius + playerSize / 2.0;
            }
        }

        // ==================== SETUP ====================

        PlayerPanel() {
            setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
            setBackground(Color.BLACK);

            // checks for WASD
            for (int keyCode : new int[] {KeyEvent.VK_W, KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D, KeyEvent.VK_R}) {
                bindKey(keyCode, true);
                bindKey(keyCode, false);
            }

            // Left click places a puddle
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    double playerCenterX = playerX + PLAYER_SIZE / 2.0;
                    double playerCenterY = playerY + PLAYER_SIZE / 2.0;

                    double targetX = event.getX();
                    double targetY = event.getY();
                    double dx = targetX - playerCenterX;
                    double dy = targetY - playerCenterY;
                    double distance = Math.hypot(dx, dy);
                    
                    double fraction = Math.min(distance, MAX_RANGE) / MAX_RANGE;
                    int delay = MIN_DELAY_FRAMES + (int) (fraction * (MAX_DELAY_FRAMES - MIN_DELAY_FRAMES));
                    if (isGameOver()) return;
                    if (placeCooldown > 0) return;
                    if (distance > MAX_RANGE) {
                        targetX = playerCenterX + dx * (MAX_RANGE / distance);
                        targetY = playerCenterY + dy * (MAX_RANGE / distance);
                    }


                    Puddle puddle = new Puddle(
                        targetX, targetY, RADIUS_FIRST, DAMAGE_FIRST, 0, delay);
                    puddles.add(puddle);
                    placeCooldown = PLACE_COOLDOWN_FRAMES;
                }
            });

            // Start the game loop
            new Timer(FRAME_DELAY_MS, e -> updateGame()).start();
        }

        // Loads an image, or returns null (and prints the path) if it fails
        private static BufferedImage loadSprite(String path) {
            File file = new File(path);
            try {
                return ImageIO.read(file);
            } catch (IOException e) {
                System.err.println("Could not load sprite: " + file.getAbsolutePath());
                return null;
            }
        }

        // controls

        // Connects one key (press or release) to setKeyPressed
        private void bindKey(int keyCode, boolean pressed) {
            String name = keyCode + (pressed ? "Press" : "Release");
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCode, 0, !pressed), name);
            getActionMap().put(name, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    setKeyPressed(keyCode, pressed);
                }
            });
        }

        // Records which keys are held, and restarts the game if R is pressed while dead
        private void setKeyPressed(int keyCode, boolean pressed) {
            if (pressed) {
                pressedKeys.add(keyCode);
            } else {
                pressedKeys.remove(keyCode);
            }
            updateMovement();

            if (pressed && keyCode == KeyEvent.VK_R && isGameOver()) {
                restartGame();
            }
        }

        // ==================== GAME LOOP ====================

        // Runs every FRAME_DELAY_MS: cooldowns, spawning, movement, damage, cleanup, redraw
        private void updateGame() {
            if (isGameOver()) return;

            frameCount++;
            if (hitCooldown < HIT_COOLDOWN_FRAMES) hitCooldown++;
            if (placeCooldown > 0) placeCooldown--;
            if (healCooldown < HEAL_COOLDOWN_FRAMES) healCooldown++;

            handleSpawning();
            updateMovement();

            // Enemies chase the player's center and hurt on contact
            double playerCenterX = playerX + PLAYER_SIZE / 2.0;
            double playerCenterY = playerY + PLAYER_SIZE / 2.0;
            for (Enemy enemy : enemies) {
                enemy.moveToward(playerCenterX, playerCenterY);
                if (enemy.isTouchingPlayer(playerX, playerY, PLAYER_SIZE) && hitCooldown >= HIT_COOLDOWN_FRAMES) {
                    damagePlayer(CONTACT_DAMAGE);
                }
            }

            // Puddles age, and pulse damage on a schedule
            for (Puddle puddle : puddles) {
                if (puddle.delay > 0) {
                    puddle.delay--;
                    if (puddle.delay == 0) {
                        damageEnemiesIn(puddle);  // the landing hit
                    }
                } else {
                    puddle.age++;
                    if (puddle.age % FRAMES_PER_HIT == 0) {
                        damageEnemiesIn(puddle);
                    }
                }
                if (puddle.isTouchingPlayer(playerCenterX, playerCenterY, PLAYER_SIZE) && healCooldown == HEAL_COOLDOWN_FRAMES && puddle.delay == 0 && playerHp < PLAYER_MAX_HP) {
                    healPlayer(HP_HEALED);
                    }
                }

            // Remove dead enemies and expired puddles
            int enemiesBefore = enemies.size();
            enemies.removeIf(Enemy::isDead);
            killCount += enemiesBefore - enemies.size();
            puddles.removeIf(Puddle::isExpired);
            repaint();
        }

        private boolean isGameOver() {
            return playerHp <= 0;
        }

        // ==================== PLAYER ====================

        // Moves the player based on held keys, and keeps them on screen
        private void updateMovement() {
            if (isGameOver()) return;

            int dx = 0;
            int dy = 0;

            if (pressedKeys.contains(KeyEvent.VK_W)) dy -= MOVE_AMOUNT;
            if (pressedKeys.contains(KeyEvent.VK_S)) dy += MOVE_AMOUNT;
            if (pressedKeys.contains(KeyEvent.VK_A)) dx -= MOVE_AMOUNT;
            if (pressedKeys.contains(KeyEvent.VK_D)) dx += MOVE_AMOUNT;

            playerX = Math.max(0, Math.min(playerX + dx, getWidth() - PLAYER_SIZE));
            playerY = Math.max(0, Math.min(playerY + dy, getHeight() - PLAYER_SIZE));
        }

        // Lowers the player's HP and starts the invincibility window
        private void damagePlayer(int amount) {
            playerHp = playerHp - amount;
            hitCooldown = 0;
        }

        private void healPlayer(int amount) {
            playerHp = playerHp + amount;
            playerHp = Math.min(playerHp, PLAYER_MAX_HP);
            healCooldown = 0;
        }

        // Puts everything back to its starting values
        private void restartGame() {
            playerHp = PLAYER_MAX_HP;
            playerX = PANEL_WIDTH / 2;
            playerY = PANEL_HEIGHT / 2;
            frameCount = 0;
            framesUntilSpawn = START_SPAWN_GAP;
            hitCooldown = HIT_COOLDOWN_FRAMES;
            placeCooldown = 0;
            healCooldown = HEAL_COOLDOWN_FRAMES;
            killCount = KILL_COUNT_START;
            enemies.clear();
            puddles.clear();
        }

        // puddles

        // Damages every enemy currently inside the given puddle
        private void damageEnemiesIn(Puddle puddle) {
            for (Enemy enemy : enemies) {
                if (enemy.isInPuddle(puddle.x, puddle.y, puddle.radius)) {
                    enemy.takeDamage(puddle.damage);
                }
            }
        }

        // enemy spawning

        // Counts down, then spawns a group scaled to the current difficulty
        private void handleSpawning() {
            framesUntilSpawn--;
            if (framesUntilSpawn > 0) return;

            // difficulty goes from 0.0 (start) to 1.0 (after FRAMES_TO_MAX_DIFFICULTY)
            double difficulty = Math.min(frameCount / (double) FRAMES_TO_MAX_DIFFICULTY, 1.0);

            // next spawn comes sooner as difficulty rises
            framesUntilSpawn = (int) (START_SPAWN_GAP - difficulty * (START_SPAWN_GAP - FASTEST_SPAWN_GAP));

            // groups of 1 at first, up to 4 later
            int groupSize = 1 + random.nextInt(1 + (int) (difficulty * 3));

            // base speed rises with difficulty, plus a little randomness per enemy
            double baseSpeed = START_ENEMY_SPEED + difficulty * (MAX_ENEMY_SPEED - START_ENEMY_SPEED);

            // a group clusters near one random spot on the edge
            double[] spot = randomEdgePoint();
            for (int i = 0; i < groupSize && enemies.size() < MAX_ENEMIES; i++) {
                double speed = baseSpeed + random.nextDouble();
                double x = spot[0] + random.nextInt(60) - 30;
                double y = spot[1] + random.nextInt(60) - 30;
                int size = 40;
                int damage = CONTACT_DAMAGE;
                Color color = new Color(255,255,255);
                enemies.add(new Enemy(x, y, speed, START_HP_ENEMY, size, damage, color));
            }
        }

        // Returns {x, y} for a random point just outside one of the four edges
        private double[] randomEdgePoint() {
            int w = getWidth();
            int h = getHeight();

            return switch (random.nextInt(4)) {
                case 0 -> new double[] {random.nextInt(w), -40};   // top
                case 1 -> new double[] {random.nextInt(w), h};     // bottom
                case 2 -> new double[] {-40, random.nextInt(h)};   // left
                default -> new double[] {w, random.nextInt(h)};    // right
            };
        }

        // ==================== DRAWING ====================

        // Draws everything, back to front: puddles, player, enemies, UI
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            drawPuddles(graphics);
            drawPlayer(graphics);
            for (Enemy enemy : enemies) {
                enemy.draw(graphics);
            }
            drawPlayerHealthBar(graphics);
            drawKillCount(graphics);

            if (isGameOver()) {
                drawGameOver(graphics);
            }
        }

        // Puddles are centered on their position, so the corner is one radius up and left
        private void drawPuddles(Graphics graphics) {
            for (Puddle puddle : puddles) {
                int left = (int) (puddle.x - puddle.radius);
                int top = (int) (puddle.y - puddle.radius);
                int diameter = puddle.radius * 2;

                if (puddle.delay > 0) {
                    // still in the air: draw a warning outline
                    graphics.setColor(new Color(120, 180, 255));
                    graphics.drawOval(left, top, diameter, diameter);
                } else if (puddleSprite != null) {
                    graphics.drawImage(puddleSprite, left, top, diameter, diameter, null);
                } else {
                    graphics.setColor(new Color(60, 120, 255, 120));
                    graphics.fillOval(left, top, diameter, diameter);
                }
            }
        }

        private void drawPlayer(Graphics graphics) {
            if (playerSprite != null) {
                graphics.drawImage(playerSprite, playerX, playerY, PLAYER_SIZE, PLAYER_SIZE, null);
            } else {
                graphics.setColor(PLAYER_COLOR);
                graphics.fillOval(playerX, playerY, PLAYER_SIZE, PLAYER_SIZE);
            }
        }

        // Red bar for missing health, green bar on top for remaining health
        private void drawPlayerHealthBar(Graphics graphics) {
            int barWidth = PLAYER_SIZE + 20;
            int barLeft = playerX - 10;

            graphics.setColor(Color.RED);
            graphics.fillRect(barLeft, playerY - 10, barWidth, 5);

            graphics.setColor(new Color(60, 180, 75));
            graphics.fillRect(barLeft, playerY - 10,
                (int) (barWidth * Math.max(playerHp, 0) / (double) PLAYER_MAX_HP), 5);
        }

        private void drawKillCount(Graphics graphics) {
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("Arial", Font.BOLD, 24));
            graphics.drawString("Kill Count:" + killCount, 6, 30);
        }

        private void drawGameOver(Graphics graphics) {
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("Arial", Font.BOLD, 48));
            graphics.drawString("It's genuinely so over...", getWidth() / 2 - 200, getHeight() / 2);
        }
    }
}