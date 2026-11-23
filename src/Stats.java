public class Stats {
    private static final int FRAMES_PER_SECOND = 60;
    private static final int RADIUS_FIRST = 60;
    int radius;
    private static final int MOVE_AMOUNT = 8;
    int moveAmount;
    private static final int DAMAGE_FIRST = 20;
    int damage;
    private static final int PLAYER_MAX_HP = 100;
    int playerMaxHp;
    int playerUpgradedMaxHp;
    private static final int CHARGE_DRAINED_PER_FRAME = 20;
    int chargeDrainedPerFrame;
    private static final int SUPER_CHARGE_NEEDED_START = 1600;
    int superChargeNeeded;
    private static final int HP_HEALED = 10;
    int hpHealed;
    boolean aftershockActive;
    boolean thornsActive;
    boolean secondWindActive;
    private static final int PLACE_COOLDOWN_FRAMES = 30;
    int placeCooldownFrames;
    private static final int MAX_RANGE = 500;
    int maxRange;
    private static final int ORB_PULL_RANGE = 400;
    int orbPullRange;
    private static final int ORB_HEAL_START = 5;
    int orbHeal;
    private static final int AURA_COOLDOWN_FRAMES = 25 * FRAMES_PER_SECOND;
    int auraCooldownFrames;
    private static final double AURA_SLOW_MULTIPLIER = 0.75;
    double auraSlowMultiplier;
    private static final double PUDDLE_SLOW_MULTIPLIER = 0.75;
    double puddleSlowMultiplier;
    public Stats() { reset(); }

    public void reset() {
        radius = RADIUS_FIRST;
        moveAmount = MOVE_AMOUNT;
        damage = DAMAGE_FIRST;
        playerMaxHp = PLAYER_MAX_HP;
        playerUpgradedMaxHp = PLAYER_MAX_HP;
        chargeDrainedPerFrame = CHARGE_DRAINED_PER_FRAME;
        superChargeNeeded = SUPER_CHARGE_NEEDED_START;
        hpHealed = HP_HEALED;
        aftershockActive = false;
        thornsActive = false;
        secondWindActive = false;
        placeCooldownFrames = PLACE_COOLDOWN_FRAMES;
        maxRange = MAX_RANGE;
        orbPullRange = ORB_PULL_RANGE;
        orbHeal = ORB_HEAL_START;
        auraCooldownFrames = AURA_COOLDOWN_FRAMES;
        auraSlowMultiplier = AURA_SLOW_MULTIPLIER;
        puddleSlowMultiplier = PUDDLE_SLOW_MULTIPLIER;
    }
}
