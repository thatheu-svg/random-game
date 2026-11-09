import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
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
import java.awt.event.MouseMotionAdapter;
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


// please don't forget to upload files to github alongside uploading the zip as a release
// also update readme every time
// make images for upgrade menu
// TODO (milestone wave + aura):
// 1. Milestone: when killCount hits a threshold (e.g. 100, then a later one), trigger the wave.
// 2. Wave: kill all enemies, set framesUntilSpawn to ~900 (15 s), state stays PLAYING.
// 3. Wave animation: expanding ring from the player's center (radius grows each frame, draws until it passes the screen edge).
// 4. Unlock: waveCleared = true -> aura ability becomes available.
// 5. Aura: follows the player, slows enemies inside it, damage scales with hits on the same enemy.


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
        private int moveAmount = MOVE_AMOUNT;
        private static final Color PLAYER_COLOR = new Color(128, 130, 177);  // used if the sprite fails to load
        private int playerMaxHp = 100;
        private int moreHpNextTime = 0;
        private static final int HIT_COOLDOWN_FRAMES = 60;   // invincibility after being hit
        private static final int HEAL_COOLDOWN_FRAMES = 60;
        private static final int HP_HEALED = 20;
        private int hpHealed = HP_HEALED;
        private int speedDuringSuper = moveAmount * 2;
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
        private int theta = 0;
        private static final int INCREASE_IN_THETA = 12;
        // Milestone wave + aura
        private static final int AURA_KILL_MILESTONE = 1;        // kills needed to trigger the wave and unlock the aura
        private boolean hasAuraAbility = false;
        private static final int WAVE_SPEED = 40;                  // pixels the wave ring grows per frame
        private static final int WAVE_SPAWN_PAUSE_FRAMES = 5 * FRAMES_PER_SECOND;   // no spawns for 15 seconds after the wave
        private boolean waveActive = false;
        private double waveRadius = 0;
        private double waveCenterX = 0;
        private double waveCenterY = 0;
        private static final int AURA_RADIUS = 300;
        private int auraRadius = AURA_RADIUS;
        private static final int AURA_DURATION_FRAMES = 6 * FRAMES_PER_SECOND;
        private static final int AURA_COOLDOWN_FRAMES = 15 * FRAMES_PER_SECOND;
        private static final int AURA_FRAMES_PER_HIT = 30;         // frames between aura damage pulses
        private static final int AURA_BASE_DAMAGE = 10;
        private static final double AURA_DAMAGE_PER_HIT = 0.5;     // extra +50% of base damage per earlier hit on that enemy
        private static final double AURA_SLOW_MULTIPLIER = 0.5;    // enemies inside the aura move at half speed
        private boolean auraActive = false;
        private int auraFramesLeft = 0;
        private int auraCooldown = 0;                              // counts DOWN to 0
        private static final int AURA_PULSE_FRAMES = 3 * FRAMES_PER_SECOND;   // one pulse ring grows from the player to the aura's edge in this long
        private int auraPulseFrames = 0;


        // Puddles
        private static final int RADIUS_FIRST = 48;
        private int radius = RADIUS_FIRST;
        private static final int DAMAGE_FIRST = 20;
        private int damage = DAMAGE_FIRST;
        private int moreDamageNextTime = 0;
        private static final int FRAMES_PER_HIT = 30;        // frames between damage pulses
        private static final int PLACE_COOLDOWN_FRAMES = 30; // wait between placing puddles
        private static final int MAX_RANGE = 500;            // max distance from player where a puddle can be placed
        private int maxRange = MAX_RANGE;
        private int placeCooldownFrames = PLACE_COOLDOWN_FRAMES;
        private static final int MAX_DELAY_FRAMES = 60;
        private static final int MIN_DELAY_FRAMES = 10;
        private static final double SUPER_PUDDLE_DAMAGE_MULTI = 0.5;
        private static final double SUPER_PUDDLE_RADIUS_MULTI = 0.8;

        // Upgrades
        private static final int BASE_XP_TO_UPGRADE = 3;  // kills needed before the extra from moreKillsToNextUpgrade
        private static final int BIGGER_PUDDLES_MAX_BUYS = 2;
        private int biggerPuddlesBought = 0;
        private static final int MOVE_FASTER_MAX_BUYS = 3;
        private int moveFastersBought = 0;
        private boolean aftershockActive = false;
        private static final int AFTERSHOCK_MAX_BUYS = 1;
        private int aftershocksBought = 0;
        private boolean thornsActive = false;
        private static final int THORNS_MAX_BUYS = 1;
        private int thornsBought = 0;
        private boolean secondWindActive = false;
        private static final int SECOND_WIND_MAX_BUYS = 1;
        private int secondWindBought = 0;
        private boolean secondWindProcced = false;
        private static final int FASTER_THROWS_MAX_BUYS = 3;
        private int fasterThrowsBought = 0;
        private static final int LONG_ARM_MAX_BUYS = 3;
        private int longArmBought = 0;
        private static final int ORB_PULL_RANGE = 400;
        private int orbPullRange = ORB_PULL_RANGE;
        private static final int MAGNET_MAX_BUYS = 3;
        private int magnetBought = 0;
        private int orbHeal = 0;                          // HP restored per orb collected
        private static final int HEALING_ORBS_MAX_BUYS = 3;
        private int healingOrbsBought = 0;

        // Enemy
        private static final int MAX_ENEMIES = 30;
        private static final int START_SPAWN_GAP = 300;      // 5 seconds between spawns at the start
        private static final int FASTEST_SPAWN_GAP = FRAMES_PER_SECOND;  // 1 second at max difficulty
        private static final int FRAMES_TO_MAX_DIFFICULTY = 36000;       // 10 minutes
        private double basicEnemySpeed = 3;
        private static final double MAX_BASIC_ENEMY_SPEED = 7;
        private static final int KILL_COUNT_START = 0;
        private int basicEnemyHp = 100;
        private int basicEnemySize = 50;
        private int basicEnemyDamage = 40;
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

        // Upgrade menu layout
        private static final int MENU_WIDTH = 960;
        private static final int MENU_HEIGHT = 540;
        private static final int MENU_PAD = 24;
        private static final int CARD_GAP = 20;
        private static final int MENU_TITLE_SPACE = 60;
        private static final int CARD_IMAGE_SIZE = 150;
        private int hoveredCard = -1;

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

        // SETUP

        PlayerPanel() {
            setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
            setBackground(Color.BLACK);

            // checks for keyboard controls
            for (int keyCode : new int[] {KeyEvent.VK_W, KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D, KeyEvent.VK_R, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_ENTER, KeyEvent.VK_P, KeyEvent.VK_SPACE, KeyEvent.VK_E}) {
                bindKey(keyCode, true);
                bindKey(keyCode, false);
            }

            // Left click places a puddle
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    if (state == GameState.UPGRADE_MENU) {
                        int card = cardAt(event.getX(), event.getY());
                        if (card >= 0) chooseUpgrade(card);
                        return;
                    }
                    double playerCenterX = playerX + PLAYER_SIZE / 2.0;
                    double playerCenterY = playerY + PLAYER_SIZE / 2.0;

                    double targetX = event.getX();
                    double targetY = event.getY();
                    double dx = targetX - playerCenterX;
                    double dy = targetY - playerCenterY;
                    double distance = Math.hypot(dx, dy);

                    double fraction = Math.min(distance, maxRange) / maxRange;
                    int delay = MIN_DELAY_FRAMES + (int) (fraction * (MAX_DELAY_FRAMES - MIN_DELAY_FRAMES));
                    if (state != GameState.PLAYING) return;
                    if (isUsingSuper == true) return;
                    if (placeCooldown > 0) return;
                    if (distance > maxRange) {
                        targetX = playerCenterX + dx * (maxRange / distance);
                        targetY = playerCenterY + dy * (maxRange / distance);
                    }
                    
                    Puddle puddle = new Puddle(
                        targetX, targetY, radius, damage, 0, delay);
                    puddles.add(puddle);
                    placeCooldown = placeCooldownFrames;
                    }
            });

            // Hover highlight for the upgrade cards
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent event) {
                    if (state != GameState.UPGRADE_MENU) return;
                    int card = cardAt(event.getX(), event.getY());
                    if (card != hoveredCard) {
                        hoveredCard = card;
                        repaint();
                    }
                }
            });

            allUpgrades.add(new BiggerPuddles());
            allUpgrades.add(new MoveFaster());
            allUpgrades.add(new MoreDamage());
            allUpgrades.add(new MorePlayerHp());
            allUpgrades.add(new LessSuperChargeUsed());
            allUpgrades.add(new HoldMoreSuper());
            allUpgrades.add(new HealYourselfMore());
            allUpgrades.add(new Aftershock());
            allUpgrades.add(new Thorns());
            allUpgrades.add(new SecondWind());
            allUpgrades.add(new FasterThrows());
            allUpgrades.add(new LongArm());
            allUpgrades.add(new Magnet());
            allUpgrades.add(new HealingOrbs());

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

            // E casts the aura once it has been unlocked
            if (pressed && keyCode == KeyEvent.VK_E && state == GameState.PLAYING) {
                castAura();
            }

            // Enter opens the upgrade menu once enough XP is collected
            if (pressed && keyCode == KeyEvent.VK_ENTER && xpToUpgrade <= 0 && state == GameState.PLAYING) {
                List<Upgrade> shuffled = new ArrayList<>();
                for (Upgrade upgrade : allUpgrades) {
                    if (upgrade.isAvailable()) {
                        shuffled.add(upgrade);
                    }
                }

                if (!shuffled.isEmpty()) {
                    Collections.shuffle(shuffled, random);
                    menuChoices.clear();
                    menuChoices.addAll(shuffled.subList(0, Math.min(3, shuffled.size())));
                    state = GameState.UPGRADE_MENU;
                    repaint();
                }
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
            hoveredCard = -1;
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
            updateAuraTimers();
            auraAffectsEnemies();
            updateWave();
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
                    if (thornsActive) {
                        enemy.takeDamage(damage);
                    }
                }
            }

            for (Orb orb : orbs) {
                orb.moveToward(playerCenterX, playerCenterY, orbPullRange);
                if (orb.isTouchingPlayer(playerCenterX, playerCenterY, PLAYER_SIZE)) {
                    collectOrb(orb);
                }
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
            checkMilestoneWave();
            for (Puddle puddle : puddles) {
                if (aftershockActive && puddle.madeBySuper() == false && puddle.isExpired()) {
                    int aftershockDamage = puddle.damage;
                    int aftershockRadius = puddle.radius;
                    puddle.radius = (int) (puddle.radius * 1.5);
                    puddle.damage = (int) (puddle.damage * 0.5);
                    damageEnemiesIn(puddle);
                    puddle.radius = aftershockRadius;
                    puddle.damage = aftershockDamage;
                }
            }
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

            if (pressedKeys.contains(KeyEvent.VK_W)) dy -= moveAmount;
            if (pressedKeys.contains(KeyEvent.VK_S)) dy += moveAmount;
            if (pressedKeys.contains(KeyEvent.VK_A)) dx -= moveAmount;
            if (pressedKeys.contains(KeyEvent.VK_D)) dx += moveAmount;

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
        // Collecting an orb gives XP (and healing, if the upgrade is owned)
        private void collectOrb(Orb orb) {
            orb.collected = true;
            xpToUpgrade -= orb.xp;
            moreXpToNextUpgrade += orb.xp / 2.0;
            playerHp = Math.min(playerHp + orbHeal, playerMaxHp);
        }

        // Lowers the player's HP and starts the invincibility window
        private void damagePlayer(int amount) {
            playerHp = playerHp - amount;
            hitCooldown = 0;
            if (playerHp <= 0 && secondWindActive && !secondWindProcced) {
                playerHp = playerMaxHp / 2;
                hitCooldown = -240;
                secondWindProcced = true;
            }
            if (playerHp <= 0) {
                state = GameState.GAME_OVER;
            }
        }

        private void healPlayer(int amount) {
            playerHp = playerHp + amount;
            playerHp = Math.min(playerHp, playerMaxHp);
            healCooldown = 0;
        }

        // At the kill milestone: a wave kills every enemy on screen, spawning pauses, and the aura unlocks
        private void checkMilestoneWave() {
            if (hasAuraAbility || killCount < AURA_KILL_MILESTONE) return;

            hasAuraAbility = true;
            for (Enemy enemy : enemies) {
                enemy.takeDamage(100000);   // they die and are cleaned up (orbs, kills) next frame
            }
            framesUntilSpawn = WAVE_SPAWN_PAUSE_FRAMES;
            waveActive = true;
            waveRadius = 0;
            waveCenterX = playerX + PLAYER_SIZE / 2.0;
            waveCenterY = playerY + PLAYER_SIZE / 2.0;
        }

        // Grows the wave ring until it is past the whole screen
        private void updateWave() {
            if (!waveActive) return;
            waveRadius += WAVE_SPEED;
            if (waveRadius > Math.hypot(getWidth(), getHeight())) {
                waveActive = false;
            }
        }

        // Starts the aura if it is unlocked and off cooldown
        private void castAura() {
            if (!hasAuraAbility || auraActive || auraCooldown > 0) return;
            auraActive = true;
            auraFramesLeft = AURA_DURATION_FRAMES;
            auraPulseFrames = 0;
        }

        // Counts the aura down while it is on, then counts its cooldown down
        private void updateAuraTimers() {
            if (auraActive) {
                auraPulseFrames = (auraPulseFrames + 1) % AURA_PULSE_FRAMES;   // wraps back to 0, so the pulse restarts
                auraFramesLeft--;
                if (auraFramesLeft <= 0) {
                    auraActive = false;
                    auraCooldown = AURA_COOLDOWN_FRAMES;
                }
            } else if (auraCooldown > 0) {
                auraCooldown--;
            }
        }

        // Enemies inside the aura are slowed and hit on a schedule (more damage for each earlier hit on them).
        // Enemies outside it go back to normal speed and lose their hit count.
        private void auraAffectsEnemies() {
            for (Enemy enemy : enemies) {
                if (auraActive && enemy.isInAura(playerX, playerY, PLAYER_SIZE, auraRadius)) {
                    enemy.setSpeedMultiplier(AURA_SLOW_MULTIPLIER);
                    if (frameCount % AURA_FRAMES_PER_HIT == 0) {
                        enemy.addAuraHit();
                        double scaled = AURA_BASE_DAMAGE * (1 + enemy.getAuraHits() * AURA_DAMAGE_PER_HIT);
                        enemy.takeDamage((int) scaled);
                    }
                } else {
                    enemy.setSpeedMultiplier(1.0);
                    enemy.resetAuraHits();
                }
            }
        }

        // Puts everything back to its starting values
        private void restartGame() {
            playerMaxHp = 100;
            moreHpNextTime = 0;
            moveAmount = MOVE_AMOUNT;
            speedDuringSuper = MOVE_AMOUNT * 2;
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
            moreDamageNextTime = 0;
            hpHealed = HP_HEALED;
            moreXpToNextUpgrade = 0;
            xpToUpgrade = BASE_XP_TO_UPGRADE;
            startingXpToUpgrade = xpToUpgrade;
            state = GameState.PLAYING;
            biggerPuddlesBought = 0;
            moveFastersBought = 0;
            superChargeNeeded = SUPER_CHARGE_NEEDED_START;
            superCharged = SUPER_CHARGED_START;
            chargeDrainedPerFrame = CHARGE_DRAINED_PER_FRAME;
            hits = 0;
            hitGainCooldown = HIT_GAIN_COOLDOWN;
            isUsingSuper = false;
            aftershockActive = false;
            thornsActive = false;
            superDistanceSincePuddle = 0;
            aftershocksBought = 0;
            thornsBought = 0;
            secondWindActive = false;
            secondWindBought = 0;
            secondWindProcced = false;
            maxRange = MAX_RANGE;
            placeCooldownFrames = PLACE_COOLDOWN_FRAMES;
            fasterThrowsBought = 0;
            longArmBought = 0;
            orbPullRange = ORB_PULL_RANGE;
            magnetBought = 0;
            orbHeal = 0;
            healingOrbsBought = 0;
            theta = 0;
            hasAuraAbility = false;
            waveActive = false;
            waveRadius = 0;
            auraRadius = AURA_RADIUS;
            auraActive = false;
            auraFramesLeft = 0;
            auraCooldown = 0;
            auraPulseFrames = 0;
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
        // private void layTrail(double oldCx, double oldCy, double newCx, double newCy) {
        //     double traveled = Math.hypot(newCx - oldCx, newCy - oldCy);
        //     if (traveled == 0) return;
        //     double dirX = (newCx - oldCx) / traveled;
        //     double dirY = (newCy - oldCy) / traveled;
        //     Puddle puddle = new Puddle(px, targetY, radius, damage, 0, delay);
        //     puddle.madeBySuper = true;

        //     superDistanceSincePuddle += traveled;
        //     while (superDistanceSincePuddle >= radius) {
        //         superDistanceSincePuddle -= radius;
        //         double px = newCx - dirX * superDistanceSincePuddle;
        //         double py = newCy - dirY * superDistanceSincePuddle;
        //         puddles.add(new Puddle(px, py, (int) (radius * SUPER_PUDDLE_RADIUS_MULTI), (int) (damage * SUPER_PUDDLE_DAMAGE_MULTI), 0, 1));
        //     }
        // }

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

                Puddle trailPuddle = new Puddle(px, py,
                    (int) (radius * SUPER_PUDDLE_RADIUS_MULTI),
                    (int) (damage * SUPER_PUDDLE_DAMAGE_MULTI),
                    0, 1);                      // delay 1 so the landing hit fires
                trailPuddle.madeBySuper = true;  // mark this puddle as coming from the super
                puddles.add(trailPuddle);
            }
        }

        // upgrades

        private class BiggerPuddles implements Upgrade {
            @Override
            public String getName() {
                return "Bigger Puddles";
            }

            @Override
            public String getDescription() {
                return "Puddles get +10 radius. Can be bought twice.";
            }

            @Override
            public void apply() {
                radius += 10;
                biggerPuddlesBought++;
            }

            @Override
            public boolean isAvailable() {
                return biggerPuddlesBought < BIGGER_PUDDLES_MAX_BUYS;
            }
        }

        private class MoveFaster implements Upgrade {
            @Override
            public String getName() {
                return "Fleet Feet";
            }

            @Override
            public String getDescription() {
                return "+2 movement speed. Can be bought 3 times.";
            }

            @Override
            public void apply() {
                moveAmount += 2;
                moveFastersBought++;
            }

            @Override
            public boolean isAvailable() {
                return moveFastersBought < MOVE_FASTER_MAX_BUYS;
            }
        }

        private class MoreDamage implements Upgrade {
            @Override
            public String getName() {
                return "More Damage";
            }

            @Override
            public String getDescription() {
                return "Puddles deal +5 damage. Each purchase adds +5 more than the last.";
            }

            @Override
            public void apply() {
                damage += 5 + moreDamageNextTime;
                moreDamageNextTime += 5;
            }
        }

        private class MorePlayerHp implements Upgrade {
            @Override
            public String getName() {
                return "Tougher Berry";
            }

            @Override
            public String getDescription() {
                return "+20 max HP. Each purchase adds +10 more than the last.";
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
                return "Efficient Super";
            }

            @Override
            public String getDescription() {
                return "Your super drains 10% less charge.";
            }

            @Override
            public void apply() {
                chargeDrainedPerFrame *= 0.9;
            }
        }

        private class HoldMoreSuper implements Upgrade {
            @Override
            public String getName() {
                return "Bigger Super Tank";
            }

            @Override
            public String getDescription() {
                return "Your super meter holds 10% more charge.";
            }

            @Override
            public void apply() {
                superChargeNeeded *= 1.1;
            }
        }

        private class HealYourselfMore implements Upgrade {
            @Override
            public String getName() {
                return "Puddle Healing";
            }

            @Override
            public String getDescription() {
                return "Standing in puddles heals 10% more.";
            }

            @Override
            public void apply() {
                hpHealed *= 1.1;
            }
        }

        private class Aftershock implements Upgrade {
            @Override 
            public String getName() {
                return "Aftershock";
            }

            @Override
            public String getDescription() {
                return "Puddles explode when they expire, dealing half damage in a wider area.";
            }

            @Override 
            public void apply() {
                aftershockActive = true;
                aftershocksBought++;
            }

            @Override
            public boolean isAvailable() {
                return aftershocksBought < AFTERSHOCK_MAX_BUYS;
            }
        }

        private class Thorns implements Upgrade {
            @Override
            public String getName() {
                return "Thorns";
            }

            @Override
            public String getDescription() {
                return "Enemies that hit you take damage equal to your puddle damage.";
            }

            @Override 
            public void apply() {
                thornsActive = true;
                thornsBought++;
            }

            @Override
            public boolean isAvailable() {
                return thornsBought < THORNS_MAX_BUYS;
            }
        }
        private class SecondWind implements Upgrade {
            @Override
            public String getName() {
                return "Second Wind";
            }

            @Override
            public String getDescription() {
                return "Once per run, survive a lethal hit at half HP and become immune for 5 seconds.";
            }

            @Override 
            public void apply() {
                secondWindActive = true;
                secondWindBought++;
            }

            @Override
            public boolean isAvailable() {
                return secondWindBought < SECOND_WIND_MAX_BUYS;
            }
        }

        private class FasterThrows implements Upgrade {
            @Override
            public String getName() {
                return "Faster Throws";
            }

            @Override
            public String getDescription() {
                return "Puddles can be placed 20% faster. Can be bought 3 times.";
            }

            @Override
            public void apply() {
                placeCooldownFrames = Math.max(8, (int) (placeCooldownFrames * 0.8));
                fasterThrowsBought++;
            }

            @Override
            public boolean isAvailable() {
                return fasterThrowsBought < FASTER_THROWS_MAX_BUYS;
            }
        }

        private class LongArm implements Upgrade {
            @Override
            public String getName() {
                return "Long Arm";
            }

            @Override
            public String getDescription() {
                return "Puddles can be placed +80 pixels farther away. Can be bought 3 times.";
            }

            @Override
            public void apply() {
                maxRange += 80;
                longArmBought++;
            }

            @Override
            public boolean isAvailable() {
                return longArmBought < LONG_ARM_MAX_BUYS;
            }
        }

        private class Magnet implements Upgrade {
            @Override
            public String getName() {
                return "Magnet";
            }

            @Override
            public String getDescription() {
                return "XP orbs are pulled toward you from +100 pixels farther away. Can be bought 3 times.";
            }

            @Override
            public void apply() {
                orbPullRange += 100;
                magnetBought++;
            }

            @Override
            public boolean isAvailable() {
                return magnetBought < MAGNET_MAX_BUYS;
            }
        }

        private class HealingOrbs implements Upgrade {
            @Override
            public String getName() {
                return "Healing Orbs";
            }

            @Override
            public String getDescription() {
                return "Collecting an XP orb heals +5 HP. Can be bought 3 times.";
            }

            @Override
            public void apply() {
                orbHeal += 5;
                healingOrbsBought++;
            }

            @Override
            public boolean isAvailable() {
                return healingOrbsBought < HEALING_ORBS_MAX_BUYS;
            }
        }

        // enemy spawning

        // Counts down, then spawns a group scaled to the current difficulty
        private void handleSpawning() {
            framesUntilSpawn--;
            if (framesUntilSpawn > 0) return;

            // difficulty goes from 0.0 (start) to 1.0 (after FRAMES_TO_MAX_DIFFICULTY)
            double difficulty = Math.min(frameCount / (double) FRAMES_TO_MAX_DIFFICULTY, 1.0);
            double scaling = (1.0 + (frameCount / 3600) * 0.15);

            // next spawn comes sooner as difficulty rises
            framesUntilSpawn = (int) (START_SPAWN_GAP - difficulty * (START_SPAWN_GAP - FASTEST_SPAWN_GAP));

            // groups of 1 at first, up to 4 later
            int groupSize = 1 + random.nextInt(1 + (int) (difficulty * 3));

            // base speed rises with difficulty, plus a little randomness per enemy
            double baseSpeed = basicEnemySpeed + difficulty * (MAX_BASIC_ENEMY_SPEED - basicEnemySpeed);

            // a group clusters near one random spot on the edge
            double[] spot = randomEdgePoint();
            for (int i = 0; i < groupSize && enemies.size() < MAX_ENEMIES; i++) {
                double speed = baseSpeed + random.nextDouble();
                double x = spot[0] + random.nextInt(60) - 30;
                double y = spot[1] + random.nextInt(60) - 30;
                double roll = random.nextDouble();
                Enemy enemy;
                if (roll < difficulty * 0.3) {
                    enemy = new TankEnemy(x, y, speed, (int) (basicEnemyHp * 1.6 * scaling), (int) (basicEnemyDamage * 1.25 * Math.max(scaling / 1.5, 1)));
                } else if (roll < difficulty * 0.3 + 0.2) {
                    enemy = new FastEnemy(x, y, speed, (int) (basicEnemyHp * 0.6 * Math.max(scaling / 1.5, 1)), (int) (basicEnemyDamage * 0.75 * Math.max(scaling / 1.2, 1)));
                } else {
                    enemy = new Enemy(x, y, speed, (int) (basicEnemyHp * Math.max(scaling / 1.5, 1)),
                            basicEnemySize, (int) (basicEnemyDamage * scaling), BASIC_ENEMY_COLOR, BASIC_ENEMY_OUTLINE_COLOR);
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
        // DRAWING

        // Draws everything, back to front: puddles, player, enemies, UI
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            drawPuddles(graphics);
            drawAura(graphics);
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
            drawAuraStatus(graphics);
            drawWave(graphics);

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

        // Translucent circle around the player while the aura is on
        private void drawAura(Graphics graphics) {
            if (!auraActive) return;
            Graphics2D g2 = (Graphics2D) graphics;
            int centerX = (int) (playerX + PLAYER_SIZE / 2.0);
            int centerY = (int) (playerY + PLAYER_SIZE / 2.0);

            g2.setColor(new Color(160, 80, 255, 60));
            g2.fillOval(centerX - auraRadius, centerY - auraRadius, auraRadius * 2, auraRadius * 2);
            g2.setColor(new Color(190, 120, 255, 200));
            g2.setStroke(new BasicStroke(3));
            g2.drawOval(centerX - auraRadius, centerY - auraRadius, auraRadius * 2, auraRadius * 2);

            // thin blue pulse: grows from the player out to the aura's edge, fading as it goes
            double progress = auraPulseFrames / (double) AURA_PULSE_FRAMES;
            int pulseRadius = (int) (auraRadius * progress);
            int alpha = (int) (255 * (1.0 - progress));
            g2.setColor(new Color(90, 170, 255, Math.max(alpha, 0)));
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(centerX - pulseRadius, centerY - pulseRadius, pulseRadius * 2, pulseRadius * 2);
            g2.setStroke(new BasicStroke(1));
        }

        // Expanding ring from where the milestone wave fired
        private void drawWave(Graphics graphics) {
            if (!waveActive) return;
            Graphics2D g2 = (Graphics2D) graphics;
            int r = (int) waveRadius;

            g2.setColor(new Color(255, 255, 255, 220));
            g2.setStroke(new BasicStroke(10));
            g2.drawOval((int) waveCenterX - r, (int) waveCenterY - r, r * 2, r * 2);
            g2.setColor(new Color(120, 200, 255, 150));
            g2.setStroke(new BasicStroke(24));
            g2.drawOval((int) waveCenterX - r, (int) waveCenterY - r, r * 2, r * 2);
            g2.setStroke(new BasicStroke(1));
        }

        // Aura status text under the kill counter
        private void drawAuraStatus(Graphics graphics) {
            if (!hasAuraAbility) return;
            graphics.setFont(new Font("Arial", Font.BOLD, 24));
            if (auraActive) {
                graphics.setColor(new Color(190, 120, 255));
                graphics.drawString("Aura: " + (auraFramesLeft / FRAMES_PER_SECOND + 1) + "s left", 6, 102);
            } else if (auraCooldown > 0) {
                graphics.setColor(Color.GRAY);
                graphics.drawString("Aura recharging: " + (auraCooldown / FRAMES_PER_SECOND + 1) + "s", 6, 102);
            } else {
                graphics.setColor(Color.YELLOW);
                graphics.drawString("Aura ready: press E", 6, 102);
            }
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

        // Where card i sits on screen (used for both drawing and mouse clicks)
        private Rectangle cardBounds(int i) {
            int menuLeft = (getWidth() - MENU_WIDTH) / 2;
            int menuTop = (getHeight() - MENU_HEIGHT) / 2;
            int cardWidth = (MENU_WIDTH - 2 * MENU_PAD - 2 * CARD_GAP) / 3;
            int cardHeight = MENU_HEIGHT - 2 * MENU_PAD - MENU_TITLE_SPACE;
            return new Rectangle(menuLeft + MENU_PAD + i * (cardWidth + CARD_GAP),
                    menuTop + MENU_PAD + MENU_TITLE_SPACE, cardWidth, cardHeight);
        }

        // Which card is under (x, y), or -1 if none
        private int cardAt(int x, int y) {
            for (int i = 0; i < menuChoices.size(); i++) {
                if (cardBounds(i).contains(x, y)) return i;
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

        // The upgrade menu: three cards, each with an image, a name, and a description
        public void drawMenu(Graphics graphics) {
            int left = (getWidth() - MENU_WIDTH) / 2;
            int top = (getHeight() - MENU_HEIGHT) / 2;

            graphics.setColor(Color.GRAY);
            graphics.fillRect(left, top, MENU_WIDTH, MENU_HEIGHT);

            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("Arial", Font.BOLD, 32));
            graphics.drawString("Choose an upgrade", left + MENU_PAD, top + MENU_PAD + 32);

            for (int i = 0; i < menuChoices.size(); i++) {
                Upgrade upgrade = menuChoices.get(i);
                Rectangle card = cardBounds(i);
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
}