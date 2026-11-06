import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
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

            // Center the window on the monitor the mouse is on
            PointerInfo pointer = MouseInfo.getPointerInfo();
            if (pointer != null) {
                Rectangle screen = pointer.getDevice().getDefaultConfiguration().getBounds();
                window.setLocation(screen.x + (screen.width - window.getWidth()) / 2,
                        screen.y + (screen.height - window.getHeight()) / 2);
            } else {
                window.setLocationRelativeTo(null);
            }
            window.setVisible(true);
        });
    }

    // The drawing surface inside the window. It also runs the whole game.
    private static class PlayerPanel extends JPanel {

        // Constants

        // Screen and timing (60 frames is 1 second)
        private static final int PANEL_WIDTH = 1920;
        private static final int PANEL_HEIGHT = 1040;
        private static final int FRAMES_PER_SECOND = 60;
        private static final double STEP_NANOS = 1_000_000_000.0 / FRAMES_PER_SECOND;
        private static final int TICK_CHECK_MS = 4;          // how often the timer checks the clock
        private static final int MAX_STEPS_PER_TICK = 5;     // most game steps run in one check

        // Player
        private static final int PLAYER_SIZE = 48;
        private static final int MOVE_AMOUNT = 8;
        private static final Color PLAYER_COLOR = new Color(128, 130, 177);  // used if the sprite fails to load
        private int playerMaxHp = 100;
        private int moreHpNextTime = 0;
        private static final int HIT_COOLDOWN_FRAMES = 60;   // invincibility after being hit
        private static final int HEAL_COOLDOWN_FRAMES = 60;
        private static final int HP_HEALED = 20;
        private int hpHealed = HP_HEALED;
        private static final int SPEED_DURING_SUPER = 16;
        private int speedDuringSuper = SPEED_DURING_SUPER;
        private static final int SUPER_CHARGED_START = 0;
        private static final int SUPER_CHARGE_NEEDED_START = 1600;
        private int superChargeNeeded = SUPER_CHARGE_NEEDED_START;
        private int superCharged = SUPER_CHARGED_START;
        private static final int CHARGE_DRAINED_PER_FRAME = 20;
        private int chargeDrainedPerFrame = CHARGE_DRAINED_PER_FRAME;
        private static final int SUPER_MIN_TO_START = 200;    // charge needed before the super can start
        private static final int SUPER_CHARGE_PER_HIT = 100;  // meter gained per hit
        private static final int HIT_GAIN_COOLDOWN = 20; // 60 frames or one second
        private int hitGainCooldown = HIT_GAIN_COOLDOWN;
        private int hits = 0;
        private double superDistanceSincePuddle = 0;
        private boolean isUsingSuper = false;
        private static final double WASD_DURING_SUPER = 0.25; // 1.0 when not supering
        private int theta = (int) Math.toRadians(0);
        private static final int INCREASE_IN_THETA = 12;


        // Puddles
        private static final int RADIUS_FIRST = 48;
        private int radius = RADIUS_FIRST;
        private static final int DAMAGE_FIRST = 20;
        private int damage = DAMAGE_FIRST;
        private static final int MAX_AGE = 300;              // puddle lifetime in frames
        private static final int FRAMES_PER_HIT = 30;        // frames between damage pulses
        private static final int PLACE_COOLDOWN_FRAMES = 30; // wait between placing puddles
        private static final int MAX_RANGE = 500;            // max distance from player where a puddle can be placed
        private static final int MAX_DELAY_FRAMES = 60;
        private static final int MIN_DELAY_FRAMES = 10;

        // Upgrades
        private static final int BASE_XP_TO_UPGRADE = 5;  // kills needed before the extra from moreKillsToNextUpgrade

        // Enemy
        private static final int MAX_ENEMIES = 30;
        private static final int START_SPAWN_GAP = 300;      // 5 seconds between spawns at the start
        private static final int FASTEST_SPAWN_GAP = FRAMES_PER_SECOND;  // 1 second at max difficulty
        private static final int FRAMES_TO_MAX_DIFFICULTY = 18000;       // 5 minutes
        private static final double BASIC_ENEMY_SPEED = 3;
        private static final double MAX_BASIC_ENEMY_SPEED = 7;
        private static final int KILL_COUNT_START = 0;
        private static final int BASIC_ENEMY_HP = 100;
        private static final int BASIC_ENEMY_SIZE = 50;
        private static final int BASIC_ENEMY_DAMAGE = 40;
        private static final Color BASIC_ENEMY_COLOR = (Color.WHITE);
        private static final Color BASIC_ENEMY_OUTLINE_COLOR = (Color.GRAY);

        // Things in the world
        private final List<Enemy> enemies = new ArrayList<>();
        private final List<Puddle> puddles = new ArrayList<>();
        private final List<Orb> orbs = new ArrayList<>();
        private final List<Upgrade> allUpgrades = new ArrayList<>();
        private final List<Upgrade> menuChoices = new ArrayList<>();
        private final Set<Integer> pressedKeys = new HashSet<>(); 
        private final Random random = new Random();

        // Images (null if the file couldn't be loaded)
        private final BufferedImage playerSprite = loadSprite("player.png");
        private final BufferedImage puddleSprite = loadSprite("galaxy_puddle.png");

        // Player state
        private double playerX = PANEL_WIDTH / 2;
        private double playerY = PANEL_HEIGHT / 2;
        private int playerHp = playerMaxHp;
        private int killCount = KILL_COUNT_START;
        private enum GameState {
            PLAYING, PAUSED, UPGRADE_MENU, GAME_OVER
        }
        private GameState state = GameState.PLAYING;
        // Timers (all counted in frames)
        private int hitCooldown = HIT_COOLDOWN_FRAMES;  // counts UP to HIT_COOLDOWN_FRAMES
        private int placeCooldown = 0;                  // counts DOWN to 0
        private int healCooldown = HEAL_COOLDOWN_FRAMES;
        private int frameCount = 0;
        private long lastTickNanos = System.nanoTime();
        private double accumulatorNanos = 0;
        private int framesUntilSpawn = START_SPAWN_GAP;

        // Menu and upgrade state
        private double moreXpToNextUpgrade = 0;
        private int xpToUpgrade = BASE_XP_TO_UPGRADE + (int) moreXpToNextUpgrade;
        private int startingXpToUpgrade = xpToUpgrade;

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

        // SETUP

        PlayerPanel() {
            setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
            setBackground(Color.BLACK);

            // checks for keyboard controls
            for (int keyCode : new int[] {KeyEvent.VK_W, KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D, KeyEvent.VK_R, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_ENTER, KeyEvent.VK_P, KeyEvent.VK_SPACE}) {
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
                    if (state != GameState.PLAYING) return;
                    if (isUsingSuper == true) return;
                    if (placeCooldown > 0) return;
                    if (distance > MAX_RANGE) {
                        targetX = playerCenterX + dx * (MAX_RANGE / distance);
                        targetY = playerCenterY + dy * (MAX_RANGE / distance);
                    }

                    Puddle puddle = new Puddle(
                        targetX, targetY, radius, damage, 0, delay);
                    puddles.add(puddle);
                    placeCooldown = PLACE_COOLDOWN_FRAMES;
                    }
            });

            allUpgrades.add(new BiggerPuddles());
            allUpgrades.add(new MoreDamage());
            allUpgrades.add(new MorePlayerHp());
            allUpgrades.add(new LessSuperChargeUsed());
            allUpgrades.add(new HoldMoreSuper());
            allUpgrades.add(new HealYourselfMore());

            // Start the game loop
            new Timer(TICK_CHECK_MS, e -> tick()).start();
        }

        // Loads an image, or returns null (and prints the path) if it fails
        private static BufferedImage loadSprite(String path) {
            try (InputStream in = PlayerPanel.class.getResourceAsStream("/" + path)) {
                if (in != null) {
                    return ImageIO.read(in);
                }
                File file = new File(path);   // fallback for running from the editor
                if (file.exists()) {
                    return ImageIO.read(file);
                }
            } catch (IOException e) {
                // falls through to the message below
            }
            System.err.println("Could not load image: " + path);
            return null;
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

        // Records which keys are held, and handles restart, pause, and the upgrade menu
        private void setKeyPressed(int keyCode, boolean pressed) {
            if (pressed) {
                pressedKeys.add(keyCode);
            } else {
                pressedKeys.remove(keyCode);
            }

            if (pressed && keyCode == KeyEvent.VK_R && isGameOver()) {
                restartGame();
            }

            // P pauses, but not while the upgrade menu is open or the game is over 
            if (pressed && keyCode == KeyEvent.VK_P) {
                if (state == GameState.PLAYING) {
                    state = GameState.PAUSED;
                    repaint();
                } else if (state == GameState.PAUSED) {
                    state = GameState.PLAYING;
                    repaint();
                }
            }

            // Enter opens the upgrade menu once enough kills are reached
            if (pressed && keyCode == KeyEvent.VK_ENTER && xpToUpgrade <= 0 && state == GameState.PLAYING) {
                List<Upgrade> shuffled = new ArrayList<>(allUpgrades);   // a copy, so the master list keeps its order
                Collections.shuffle(shuffled, random);
                menuChoices.clear();
                menuChoices.addAll(shuffled.subList(0, Math.min(3, shuffled.size())));
                state = GameState.UPGRADE_MENU;
                repaint();
            }

            // 1, 2, 3 pick an upgrade while the menu is open
            if (state == GameState.UPGRADE_MENU && pressed) {
                switch (keyCode) {
                    case KeyEvent.VK_1 -> chooseUpgrade(0);
                    case KeyEvent.VK_2 -> chooseUpgrade(1);
                    case KeyEvent.VK_3 -> chooseUpgrade(2);
                }
            }
        }

        private void updateSuper() {
            boolean held = pressedKeys.contains(KeyEvent.VK_SPACE);

            // starts when Space is held with enough charge, ends when released or empty
            if (!isUsingSuper && held && superCharged >= SUPER_MIN_TO_START) {
                isUsingSuper = true;
                superDistanceSincePuddle = 0;
            }
            if (isUsingSuper && (!held || superCharged <= 0)) {
                isUsingSuper = false;
            }
            if (!isUsingSuper) return;

            Point mouse = getMousePosition();     // window coordinates, null if the cursor is outside
            if (mouse == null) return;

            double dx = mouse.x - (playerX + PLAYER_SIZE / 2.0);
            double dy = mouse.y - (playerY + PLAYER_SIZE / 2.0);
            double distance = Math.hypot(dx, dy);
            if (distance < 1) return;             // already on the cursor

            double step = Math.min(speedDuringSuper, distance);   // don't overshoot the cursor
            playerX += dx / distance * step;
            playerY += dy / distance * step;
            playerX = Math.max(0, Math.min(playerX, getWidth() - PLAYER_SIZE));
            playerY = Math.max(0, Math.min(playerY, getHeight() - PLAYER_SIZE));

            superCharged = Math.max(superCharged - chargeDrainedPerFrame, 0);
        }
        // Applies the chosen upgrade, closes the menu, and sets the next kill requirement
        private void chooseUpgrade(int index) {
            if (index >= menuChoices.size()) return;
            menuChoices.get(index).apply();
            xpToUpgrade = BASE_XP_TO_UPGRADE + (int) moreXpToNextUpgrade;
            startingXpToUpgrade = BASE_XP_TO_UPGRADE + (int) moreXpToNextUpgrade;
            state = GameState.PLAYING;
            repaint();
        }

        // GAME LOOP

        // Runs once per game step (60 per second): cooldowns, spawning, movement, damage, cleanup, redraw
        private void updateGame() {
            if (state != GameState.PLAYING) return;

            frameCount++;
            if (hitCooldown < HIT_COOLDOWN_FRAMES) hitCooldown++;
            if (placeCooldown > 0) placeCooldown--;
            if (healCooldown < HEAL_COOLDOWN_FRAMES) healCooldown++;
            if (hitGainCooldown > 0) hitGainCooldown--;
            if (isUsingSuper) {
            theta = (theta + INCREASE_IN_THETA) % 360;
            } else {
                theta = 0;
            }
            handleSpawning();
            double startCx = playerX + PLAYER_SIZE / 2.0;
            double startCy = playerY + PLAYER_SIZE / 2.0;
            updateMovement();
            updateSuper();
            if (isUsingSuper) {
                layTrail(startCx, startCy, playerX + PLAYER_SIZE / 2.0, playerY + PLAYER_SIZE / 2.0);
            }

            // Enemies chase the player's center and hurt on contact
            double playerCenterX = playerX + PLAYER_SIZE / 2.0;
            double playerCenterY = playerY + PLAYER_SIZE / 2.0;
            for (Enemy enemy : enemies) {
                enemy.moveToward(playerCenterX, playerCenterY);
                if (enemy.isTouchingPlayer(playerX, playerY, PLAYER_SIZE) && hitCooldown >= HIT_COOLDOWN_FRAMES) {
                    damagePlayer(enemy.getDamage());
                }
            }

            for (Orb orb : orbs) {
                orb.moveToward(playerCenterX, playerCenterY);
                orb.givePlayerXp();
            }
            orbs.removeIf(orb -> orb.collected);
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
                if (puddle.isTouchingPlayer(playerCenterX, playerCenterY, PLAYER_SIZE)
                        && healCooldown == HEAL_COOLDOWN_FRAMES
                        && puddle.delay == 0
                        && playerHp < playerMaxHp) {
                    healPlayer(hpHealed);
                }
            }
            // Remove dead enemies and expired puddles, also counts kills for now
            int enemiesBefore = enemies.size();
            for (Enemy enemy : enemies) {
                if (enemy.isDead()) {
                for (int i = 1; i < 2; i++) {
                    Orb orb = new Orb(enemy.getPosX(), enemy.getPosY(), 30, 10, 1, false);
                    orbs.add(orb);
                    }
                }
            }
            enemies.removeIf(Enemy::isDead);
            int kills = enemiesBefore - enemies.size();
            killCount += kills;
            puddles.removeIf(Puddle::isExpired);
            repaint();
        }

        // Called often by the timer; runs as many game steps as real time requires
        private void tick() {
            long now = System.nanoTime();
            accumulatorNanos += now - lastTickNanos;
            lastTickNanos = now;

            int steps = 0;
            while (accumulatorNanos >= STEP_NANOS && steps < MAX_STEPS_PER_TICK) {
                updateGame();
                accumulatorNanos -= STEP_NANOS;
                steps++;
            }
            if (steps == MAX_STEPS_PER_TICK) {
                accumulatorNanos = 0;   // too far behind: drop the backlog instead of racing to catch up
            }
        }

        private boolean isGameOver() {
            return state == GameState.GAME_OVER;
        }

        // PLAYER

        // Moves the player based on held keys, and keeps them on screen
        private void updateMovement() {
            if (state != GameState.PLAYING) return;

            double dx = 0;
            double dy = 0;

            if (pressedKeys.contains(KeyEvent.VK_W)) dy -= MOVE_AMOUNT;
            if (pressedKeys.contains(KeyEvent.VK_S)) dy += MOVE_AMOUNT;
            if (pressedKeys.contains(KeyEvent.VK_A)) dx -= MOVE_AMOUNT;
            if (pressedKeys.contains(KeyEvent.VK_D)) dx += MOVE_AMOUNT;

            if (dx != 0 && dy != 0) {
                dx *= Math.sqrt(0.5);
                dy *= Math.sqrt(0.5);
            }

            if (isUsingSuper) {
            dx *= WASD_DURING_SUPER;
            dy *= WASD_DURING_SUPER;
            }

            playerX = Math.max(0, Math.min(playerX + dx, getWidth() - PLAYER_SIZE));
            playerY = Math.max(0, Math.min(playerY + dy, getHeight() - PLAYER_SIZE));
        }
        // Lowers the player's HP and starts the invincibility window
        private void damagePlayer(int amount) {
            playerHp = playerHp - amount;
            hitCooldown = 0;
            if (playerHp <= 0) {
                state = GameState.GAME_OVER;
            }
        }

        private void healPlayer(int amount) {
            playerHp = playerHp + amount;
            playerHp = Math.min(playerHp, playerMaxHp);
            healCooldown = 0;
        }

        // Puts everything back to its starting values
        private void restartGame() {
            playerMaxHp = 100;
            moreHpNextTime = 0;
            playerHp = playerMaxHp;
            playerX = PANEL_WIDTH / 2;
            playerY = PANEL_HEIGHT / 2;
            frameCount = 0;
            framesUntilSpawn = START_SPAWN_GAP;
            hitCooldown = HIT_COOLDOWN_FRAMES;
            placeCooldown = 0;
            healCooldown = HEAL_COOLDOWN_FRAMES;
            killCount = KILL_COUNT_START;
            radius = RADIUS_FIRST;
            damage = DAMAGE_FIRST;
            hpHealed = HP_HEALED;
            moreXpToNextUpgrade = 0;
            xpToUpgrade = BASE_XP_TO_UPGRADE;
            startingXpToUpgrade = xpToUpgrade;
            state = GameState.PLAYING;
            superChargeNeeded = SUPER_CHARGE_NEEDED_START;
            superCharged = SUPER_CHARGED_START;
            chargeDrainedPerFrame = CHARGE_DRAINED_PER_FRAME;
            hits = 0;
            hitGainCooldown = HIT_GAIN_COOLDOWN;
            isUsingSuper = false;
            superDistanceSincePuddle = 0;
            theta = 0;
            enemies.clear();
            puddles.clear();
            orbs.clear();
            // keep adding stuff that needs to be reset when you die
        }

        // puddles

        // Damages every enemy currently inside the given puddle
        private void damageEnemiesIn(Puddle puddle) {
            for (Enemy enemy : enemies) {
                if (enemy.isInPuddle(puddle.x, puddle.y, puddle.radius)) {
                    enemy.takeDamage(puddle.damage);
                    hits = hits + 1;
                    if (hitGainCooldown <= 0) {
                        superCharged = Math.min(superChargeNeeded, superCharged + SUPER_CHARGE_PER_HIT);
                        hitGainCooldown = HIT_GAIN_COOLDOWN;
                    }
                }
            }
        }

                // Drops puddles along the path from the old position to the new one, one radius apart
        private void layTrail(double oldCx, double oldCy, double newCx, double newCy) {
            double traveled = Math.hypot(newCx - oldCx, newCy - oldCy);
            if (traveled == 0) return;
            double dirX = (newCx - oldCx) / traveled;
            double dirY = (newCy - oldCy) / traveled;

            superDistanceSincePuddle += traveled;
            while (superDistanceSincePuddle >= radius) {
                superDistanceSincePuddle -= radius;
                double px = newCx - dirX * superDistanceSincePuddle;
                double py = newCy - dirY * superDistanceSincePuddle;
                puddles.add(new Puddle(px, py, radius, damage, 0, 1));   // delay 1 so the landing hit fires
            }
        }

        // upgrades

        private class BiggerPuddles implements Upgrade {
            @Override
            public String getName() {
                return "Bigger puddles (+10 radius)";
            }

            @Override
            public void apply() {
                radius += 10;
            }
        }

        private class MoreDamage implements Upgrade {
            @Override
            public String getName() {
                return "Puddles do more damage (+10%)";
            }

            @Override
            public void apply() {
                damage *= 1.1;
            }
        }

        private class MorePlayerHp implements Upgrade {
            @Override
            public String getName() {
                return "Player has more health (+20)";
            }

            @Override
            public void apply() {
                playerMaxHp += 20 + moreHpNextTime;
                moreHpNextTime += 10;
            }
        }

        private class LessSuperChargeUsed implements Upgrade {
            @Override
            public String getName() {
                return "Supering uses less charge (*0.9)";
            }

            @Override
            public void apply() {
                chargeDrainedPerFrame *= 0.9;
            }
        }

        private class HoldMoreSuper implements Upgrade {
            @Override
            public String getName() {
                return "Hold more Super charge at once (*1.1)";
            }

            @Override
            public void apply() {
                superChargeNeeded *= 1.1;
            }
        }

        private class HealYourselfMore implements Upgrade {
            @Override
            public String getName() {
                return "Standing in puddles heals more (+10%)";
            }

            @Override
            public void apply() {
                hpHealed *= 1.1;
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
            double baseSpeed = BASIC_ENEMY_SPEED + difficulty * (MAX_BASIC_ENEMY_SPEED - BASIC_ENEMY_SPEED);

            // a group clusters near one random spot on the edge
            double[] spot = randomEdgePoint();
            for (int i = 0; i < groupSize && enemies.size() < MAX_ENEMIES; i++) {
                double speed = baseSpeed + random.nextDouble();
                double x = spot[0] + random.nextInt(60) - 30;
                double y = spot[1] + random.nextInt(60) - 30;
                double roll = random.nextDouble();
                Enemy enemy;
                if (roll < difficulty * 0.3) {
                    enemy = new TankEnemy(x, y, speed);
                } else if (roll < difficulty * 0.3 + 0.2) {
                    enemy = new FastEnemy(x, y, speed);
                } else {
                    enemy = new Enemy(x, y, speed, BASIC_ENEMY_HP,
                            BASIC_ENEMY_SIZE, BASIC_ENEMY_DAMAGE, BASIC_ENEMY_COLOR, BASIC_ENEMY_OUTLINE_COLOR);
                }
                enemies.add(enemy);
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
        // ORBS
        private class Orb {
            double orbX;
            double orbY;
            double speed;
            int radius;
            int xp;
            boolean collected;

            Orb(double x, double y, double speed, int radius, int xp, boolean collected) {
                this.orbX = x;
                this.orbY = y;
                this.speed = speed;
                this.radius = radius;
                this.xp = xp;
                this.collected = collected;
            }

            public void moveToward(double targetX, double targetY) {
            double dx = targetX - orbX;
            double dy = targetY - orbY;
            double distance = Math.hypot(dx, dy);

            if (distance < 1) return;
            if (distance > 400) return;
            if (isTouchingPlayer()) return;

            orbX += dx / distance * speed;
            orbY += dy / distance * speed;
            }
            public boolean isTouchingPlayer() {
            double dx = orbX - (playerX + PLAYER_SIZE / 2.0);
            double dy = orbY - (playerY + PLAYER_SIZE / 2.0);
            return Math.hypot(dx, dy) <= radius / 2.0 + PLAYER_SIZE / 2.0;
            }

            public void givePlayerXp() {
                if (isTouchingPlayer()) {
                collected = true;
                xpToUpgrade -= xp;
                moreXpToNextUpgrade += xp / 2.0;  // take this out of loop when you come back diddyson
                }
            }
        }

        // DRAWING

        // Draws everything, back to front: puddles, player, enemies, UI
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            drawPuddles(graphics);
            drawOrb(graphics);
            drawPlayer(graphics);
            drawSuperRing(graphics);
            for (Enemy enemy : enemies) {
                enemy.draw(graphics);
            }
            drawPlayerHealthBar(graphics);
            drawXpBar(graphics);
            drawKillCount(graphics);
            drawHits(graphics);

            switch (state) {
                case PAUSED -> drawGamePaused(graphics);
                case UPGRADE_MENU -> drawMenu(graphics);
                case GAME_OVER -> drawGameOver(graphics);
                case PLAYING -> {}
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
                Graphics2D g2 = (Graphics2D) graphics;
                AffineTransform notSpinning = g2.getTransform();
                g2.rotate(Math.toRadians(theta), playerX + PLAYER_SIZE / 2.0, playerY + PLAYER_SIZE / 2.0);
                graphics.drawImage(playerSprite, (int) playerX, (int) playerY, PLAYER_SIZE, PLAYER_SIZE, null);
                g2.setTransform(notSpinning);
            } else {
                graphics.setColor(PLAYER_COLOR);
                graphics.fillOval((int) playerX, (int) playerY, PLAYER_SIZE, PLAYER_SIZE);
            }
        }

        private void drawOrb(Graphics graphics) {
            for (Orb orb : orbs) {
                int diameter = orb.radius * 2;
                int left = (int) (orb.orbX - orb.radius);
                int top = (int) (orb.orbY - orb.radius);
                graphics.setColor(Color.CYAN);
                graphics.fillOval(left, top, diameter, diameter);
            }
        }

        // Red bar for missing health, green bar on top for remaining health
        private void drawPlayerHealthBar(Graphics graphics) {
            int barWidth = PLAYER_SIZE + 20;
            int barLeft = (int) playerX - 10;

            graphics.setColor(Color.RED);
            graphics.fillRect(barLeft, (int) playerY - 10, barWidth, 5);

            graphics.setColor(new Color(60, 180, 75));
            graphics.fillRect(barLeft, (int) playerY - 10,
                (int) (barWidth * Math.max(playerHp, 0) / (double) playerMaxHp), 5);

            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("Arial", Font.BOLD, 10));
            graphics.drawString(" " + Math.max(playerHp, 0) + " / " + playerMaxHp, barLeft + 9, (int) playerY - 11);
        }

        // Kill count, kills needed for the next upgrade, and the Enter prompt when one is ready
        private void drawKillCount(Graphics graphics) {
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("Arial", Font.BOLD, 24));
            graphics.drawString("Kill Count:" + killCount, 6, 30);
            graphics.drawString("Xp to Upgrade:" + Math.max(xpToUpgrade, 0), 6, 54);

            if (xpToUpgrade <= 0 && state != GameState.UPGRADE_MENU) {
                graphics.setColor(Color.YELLOW);
                graphics.drawString("Upgrade ready: press Enter", 6, 78);
            }
        }

        private void drawXpBar(Graphics graphics) {
            graphics.setColor(Color.GRAY);
            graphics.fillRect((int) (PANEL_WIDTH / 2.0 - 250), (int) (PANEL_HEIGHT - 104), 500, 44);

            graphics.setColor(Color.WHITE);
            graphics.fillRect((int) (PANEL_WIDTH / 2.0 - 246), (int) (PANEL_HEIGHT - 99), 490, 33);

            double fraction = 1.0 - Math.max(xpToUpgrade, 0) / (double) startingXpToUpgrade;
            graphics.setColor(Color.YELLOW);
            graphics.fillRect((int) (PANEL_WIDTH / 2.0 - 246), (int) (PANEL_HEIGHT - 99), (int) (490 * fraction), 33);
        }

        private void drawHits(Graphics graphics) {
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("Arial", Font.BOLD, 24));
            graphics.drawString("Hits:" + hits, getWidth()/2, getHeight()/2 - 100);
        }

        private void drawSuperRing(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics;
        int pad = 6;
        int size = PLAYER_SIZE + pad * 2;
        int sweep = (int) (360.0 * superCharged / superChargeNeeded);

        g2.setColor(Color.YELLOW);
        g2.setStroke(new BasicStroke(4));
        g2.drawArc((int) playerX - pad, (int) playerY - pad, size, size, 90, -sweep);   // clockwise from the top
        g2.setStroke(new BasicStroke(1));   // restore so later drawing isn't thick
        }

        private void drawGamePaused(Graphics graphics) {
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("Arial", Font.BOLD, 48));
            graphics.drawString("PAUSED", getWidth() / 2 - 100, getHeight() / 2);
        }

        private void drawGameOver(Graphics graphics) {
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("Arial", Font.BOLD, 48));
            graphics.drawString("It's genuinely so over...", getWidth() / 2 - 200, getHeight() / 2);
        }

        // The upgrade menu, centered on the panel
        public void drawMenu(Graphics graphics) {
            int menuWidth = 960;
            int menuHeight = 540;
            int left = (getWidth() - menuWidth) / 2;
            int top = (getHeight() - menuHeight) / 2;

            graphics.setColor(Color.GRAY);
            graphics.fillRect(left, top, menuWidth, menuHeight);

            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("Arial", Font.BOLD, 32));
            for (int i = 0; i < menuChoices.size(); i++) {
            graphics.drawString((i + 1) + ": " + menuChoices.get(i).getName(), left + 40, top + 80 + i * 60);
            }
        }
    }
}