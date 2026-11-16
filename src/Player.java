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
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.*;


// please don't forget to upload files to github alongside uploading the zip as a release
// also update readme every time
// make images for upgrade menu (Currently in progress)
// art made for bigger puddles, movefaster, more damage and more hp
// begin work on making the images actually show up in the upgrade menu, probably should add something to upgrade.java to add the thing that will
// individually give each upgrade the ability to call an image. Should preload it somewhere

// Ideas for making this work: use the getimage() thing that already exists in upgrade.java and put them into each upgrade class in upgrades.java



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
        private final Stats stats = new Stats();
        private static final int PLAYER_SIZE = 48;
        private static final Color PLAYER_COLOR = new Color(128, 130, 177);  // used if the sprite fails to load
        private static final int HIT_COOLDOWN_FRAMES = 60;   // invincibility after being hit
        private static final int HEAL_COOLDOWN_FRAMES = 60;
        private int speedDuringSuper = stats.moveAmount * 2;
        private static final int SUPER_CHARGED_START = 0;
        private int superCharged = SUPER_CHARGED_START;
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
        private static final int AURA_KILL_MILESTONE = 500;        // kills needed to trigger the wave and unlock the aura
        private static final int WAVE_SPAWN_PAUSE_FRAMES = 5 * FRAMES_PER_SECOND;   // no spawns for 5 seconds after the wave
        private final Wave wave = new Wave();
        private final Aura aura = new Aura();


        // Puddles
        private static final int FRAMES_PER_HIT = 30;        // frames between damage pulses
        private static final int MAX_DELAY_FRAMES = 60;
        private static final int MIN_DELAY_FRAMES = 10;
        private static final double SUPER_PUDDLE_DAMAGE_MULTI = 0.5;
        private static final double SUPER_PUDDLE_RADIUS_MULTI = 0.8;

        // Upgrades
        private static final int BASE_XP_TO_UPGRADE = 3;  // kills needed before the extra from moreKillsToNextUpgrade
        private boolean secondWindProcced = false;
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
        private final List<DeadEnemy> deadEnemies = new ArrayList<>();
        private final List<Puddle> puddles = new ArrayList<>();
        private final List<Orb> orbs = new ArrayList<>();
        private final List<Upgrade> allUpgrades = new ArrayList<>();
        private final Set<Integer> pressedKeys = new HashSet<>(); 
        private final Random random = new Random();

        // Images (null if the file couldn't be loaded)
        private final BufferedImage playerSprite = loadSprite("player.png");
        private final BufferedImage puddleSprite = loadSprite("galaxy_puddle.png");

        // The upgrade menu (draws its own cards and finds which one is clicked)
        private final UpgradeMenu menu = new UpgradeMenu();

        // Player state
        private double playerX = PANEL_WIDTH / 2;
        private double playerY = PANEL_HEIGHT / 2;
        private int playerHp = stats.playerMaxHp;
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
                        int card = menu.cardAt(event.getX(), event.getY(), getWidth(), getHeight());
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

                    double fraction = Math.min(distance, stats.maxRange) / stats.maxRange;
                    int delay = MIN_DELAY_FRAMES + (int) (fraction * (MAX_DELAY_FRAMES - MIN_DELAY_FRAMES));
                    if (state != GameState.PLAYING) return;
                    if (isUsingSuper == true) return;
                    if (placeCooldown > 0) return;
                    if (distance > stats.maxRange) {
                        targetX = playerCenterX + dx * (stats.maxRange / distance);
                        targetY = playerCenterY + dy * (stats.maxRange / distance);
                    }
                    
                    Puddle puddle = new Puddle(
                        targetX, targetY, stats.radius, stats.damage, 0, delay);
                    puddles.add(puddle);
                    placeCooldown = stats.placeCooldownFrames;
                    }
            });

            // Hover highlight for the upgrade cards
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent event) {
                    if (state != GameState.UPGRADE_MENU) return;
                    if (menu.updateHover(event.getX(), event.getY(), getWidth(), getHeight())) {
                        repaint();
                    }
                }
            });

            // Start the game loop
            buildUpgrades();
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
                aura.cast();
            }

            // Enter opens the upgrade menu once enough XP is collected
            if (pressed && keyCode == KeyEvent.VK_ENTER && xpToUpgrade <= 0 && state == GameState.PLAYING) {
                if (menu.open(allUpgrades, random)) {
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
            speedDuringSuper = stats.moveAmount * 2;

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

            superCharged = Math.max(superCharged - stats.chargeDrainedPerFrame, 0);
        }
        // Applies the chosen upgrade, closes the menu, and sets the next kill requirement
        private void chooseUpgrade(int index) {
            Upgrade chosen = menu.get(index);
            if (chosen == null) return;
            chosen.apply();
            menu.clearHover();
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
            for (DeadEnemy deadEnemy : deadEnemies) {
                deadEnemy.tickDuration();
            }
            handleSpawning();
            double startCx = playerX + PLAYER_SIZE / 2.0;
            double startCy = playerY + PLAYER_SIZE / 2.0;
            updateMovement();
            updateSuper();
            aura.updateTimers(stats.auraCooldownFrames);
            aura.affectEnemies(enemies, playerX, playerY, PLAYER_SIZE, frameCount, stats.auraSlowMultiplier);
            wave.update(getWidth(), getHeight());
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
                    if (stats.thornsActive) {
                        enemy.takeDamage(stats.damage);
                    }
                }
            }

            for (Orb orb : orbs) {
                orb.moveToward(playerCenterX, playerCenterY, stats.orbPullRange);
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
                        && playerHp < stats.playerMaxHp) {
                    healPlayer(stats.hpHealed);
                }
            }
            // Remove dead enemies and expired puddles, also counts kills for now
            int enemiesBefore = enemies.size();
            for (Enemy enemy : enemies) {
                if (enemy.isDead()) {
                    int deathX = enemy.getPosX();
                    int deathY = enemy.getPosY();
                    int duration = 0;
                        DeadEnemy deadEnemy = new DeadEnemy(deathX, deathY, enemy.getSize(), duration);
                        deadEnemies.add(deadEnemy);
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
                if (stats.aftershockActive && puddle.madeBySuper() == false && puddle.isExpired()) {
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
            deadEnemies.removeIf(DeadEnemy::durationExpired);
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

            if (pressedKeys.contains(KeyEvent.VK_W)) dy -= stats.moveAmount;
            if (pressedKeys.contains(KeyEvent.VK_S)) dy += stats.moveAmount;
            if (pressedKeys.contains(KeyEvent.VK_A)) dx -= stats.moveAmount;
            if (pressedKeys.contains(KeyEvent.VK_D)) dx += stats.moveAmount;

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
            playerHp = Math.min(playerHp + stats.orbHeal, stats.playerMaxHp);
        }

        // Lowers the player's HP and starts the invincibility window
        private void damagePlayer(int amount) {
            playerHp = playerHp - amount;
            hitCooldown = 0;
            if (playerHp <= 0 && stats.secondWindActive && !secondWindProcced) {
                playerHp = stats.playerMaxHp / 2;
                hitCooldown = -240;
                secondWindProcced = true;
            }
            if (playerHp <= 0) {
                state = GameState.GAME_OVER;
            }
        }

        private void healPlayer(int amount) {
            playerHp = playerHp + amount;
            playerHp = Math.min(playerHp, stats.playerMaxHp);
            healCooldown = 0;
        }

        // At the kill milestone: a wave kills every enemy on screen, spawning pauses, and the aura unlocks
        private void checkMilestoneWave() {
            if (aura.isUnlocked() || killCount < AURA_KILL_MILESTONE) return;

            aura.unlock();
            for (Enemy enemy : enemies) {
                enemy.takeDamage(100000);   // they die and are cleaned up (orbs, kills) next frame
            }
            framesUntilSpawn = WAVE_SPAWN_PAUSE_FRAMES;
            wave.start(playerX + PLAYER_SIZE / 2.0, playerY + PLAYER_SIZE / 2.0);
        }

        private void buildUpgrades() {
            allUpgrades.clear();
            allUpgrades.add(new Upgrades.BiggerPuddles(stats));
            allUpgrades.add(new Upgrades.MoveFaster(stats));
            allUpgrades.add(new Upgrades.MoreDamage(stats));
            allUpgrades.add(new Upgrades.MorePlayerHp(stats));
            allUpgrades.add(new Upgrades.LessSuperChargeUsed(stats));
            allUpgrades.add(new Upgrades.HoldMoreSuper(stats));
            allUpgrades.add(new Upgrades.HealYourselfMore(stats));
            allUpgrades.add(new Upgrades.Aftershock(stats));
            allUpgrades.add(new Upgrades.Thorns(stats));
            allUpgrades.add(new Upgrades.SecondWind(stats));
            allUpgrades.add(new Upgrades.FasterThrows(stats));
            allUpgrades.add(new Upgrades.LongArm(stats));
            allUpgrades.add(new Upgrades.Magnet(stats));
            allUpgrades.add(new Upgrades.HealingOrbs(stats));
            allUpgrades.add(new Upgrades.AuraMoreOften(stats));
            allUpgrades.add(new Upgrades.AuraSlowsMore(stats));
        }

        // Puts everything back to its starting values
        private void restartGame() {
            stats.reset();
            speedDuringSuper = stats.moveAmount * 2;
            playerHp = stats.playerMaxHp;
            playerX = PANEL_WIDTH / 2;
            playerY = PANEL_HEIGHT / 2;
            frameCount = 0;
            framesUntilSpawn = START_SPAWN_GAP;
            hitCooldown = HIT_COOLDOWN_FRAMES;
            placeCooldown = 0;
            healCooldown = HEAL_COOLDOWN_FRAMES;
            killCount = KILL_COUNT_START;
            moreXpToNextUpgrade = 0;
            xpToUpgrade = BASE_XP_TO_UPGRADE;
            startingXpToUpgrade = xpToUpgrade;
            state = GameState.PLAYING;
            superCharged = SUPER_CHARGED_START;
            hits = 0;
            hitGainCooldown = HIT_GAIN_COOLDOWN;
            isUsingSuper = false;
            superDistanceSincePuddle = 0;
            secondWindProcced = false;
            theta = 0;
            wave.reset();
            aura.reset();
            buildUpgrades();
            enemies.clear();
            puddles.clear();
            deadEnemies.clear();
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
                        superCharged = Math.min(stats.superChargeNeeded, superCharged + SUPER_CHARGE_PER_HIT);
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
            while (superDistanceSincePuddle >= stats.radius) {
                superDistanceSincePuddle -= stats.radius;
                double px = newCx - dirX * superDistanceSincePuddle;
                double py = newCy - dirY * superDistanceSincePuddle;

                Puddle trailPuddle = new Puddle(px, py,
                    (int) (stats.radius * SUPER_PUDDLE_RADIUS_MULTI),
                    (int) (stats.damage * SUPER_PUDDLE_DAMAGE_MULTI),
                    0, 1);                      // delay 1 so the landing hit fires
                trailPuddle.madeBySuper = true;  // mark this puddle as coming from the super
                puddles.add(trailPuddle);
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
            aura.draw(graphics, playerX, playerY, PLAYER_SIZE);
            drawOrb(graphics);
            drawPlayer(graphics);
            drawSuperRing(graphics);
            for (Enemy enemy : enemies) {
                enemy.draw(graphics);
            }
            for (DeadEnemy deadEnemy : deadEnemies) {
                deadEnemy.drawDeathAnimation(graphics);
            }
            drawPlayerHealthBar(graphics);
            drawXpBar(graphics);
            drawKillCount(graphics);
            drawHits(graphics);
            aura.drawStatus(graphics);
            wave.draw(graphics);

            switch (state) {
                case PAUSED -> drawGamePaused(graphics);
                case UPGRADE_MENU -> menu.draw(graphics, getWidth(), getHeight());
                case GAME_OVER -> drawGameOver(graphics);
                case PLAYING -> {}
            }
        }

        private void drawPuddles(Graphics graphics) {
            for (Puddle puddle : puddles) {
                puddle.draw(graphics, puddleSprite);
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
                orb.draw(graphics);
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
                (int) (barWidth * Math.max(playerHp, 0) / (double) stats.playerMaxHp), 5);

            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("Arial", Font.BOLD, 10));
            graphics.drawString(" " + Math.max(playerHp, 0) + " / " + stats.playerMaxHp, barLeft + 9, (int) playerY - 11);
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
        int sweep = (int) (360.0 * superCharged / stats.superChargeNeeded);

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

    }
}