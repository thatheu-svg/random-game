public final class Upgrades {
    public static class BiggerPuddles implements Upgrade {
        private static final int MAX_BUYS = 2;
        private int bought = 0;
        private final Stats stats;

        public BiggerPuddles(Stats stats) {
            this.stats = stats;
        }
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
            stats.radius += 10;
            bought++;
        }

        @Override
        public boolean isAvailable() {
            return bought < MAX_BUYS;
        }
    }

    public static class MoveFaster implements Upgrade {
        private static final int MAX_BUYS = 3;
        private int bought = 0;
        private final Stats stats;

        public MoveFaster(Stats stats) {
            this.stats = stats;
        }
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
            stats.moveAmount += 2;
            bought++;
        }

        @Override
        public boolean isAvailable() {
            return bought < MAX_BUYS;
        }
    }

    public static class MoreDamage implements Upgrade {
        private int moreDamageNextTime = 0;
        private final Stats stats;

        public MoreDamage(Stats stats) {
            this.stats = stats;
        }
        @Override
        public String getName() {
            return "More damage";
        }

        @Override
        public String getDescription() {
            return "Puddles deal +5 damage. Each purchase adds +5 more than the last.";
        }

        @Override
        public void apply() {
            stats.damage += 5 + moreDamageNextTime;
            moreDamageNextTime += 5;
        }
    }

    public static class MorePlayerHp implements Upgrade {
        private int moreHpNextTime = 0;
        private final Stats stats;

        public MorePlayerHp(Stats stats) {
            this.stats = stats;
        }
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
            stats.playerMaxHp += 20 + moreHpNextTime;
            moreHpNextTime += 10;
        }
    }

    public static class LessSuperChargeUsed implements Upgrade {
        private final Stats stats;

        public LessSuperChargeUsed(Stats stats) {
            this.stats = stats;
        }
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
            stats.chargeDrainedPerFrame *= 0.9;
        }
    }

    public static class HoldMoreSuper implements Upgrade {
        private final Stats stats;

        public HoldMoreSuper(Stats stats) {
            this.stats = stats;
        }
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
            stats.superChargeNeeded *= 1.1;
        }
    }

    public static class HealYourselfMore implements Upgrade {
        private final Stats stats;

        public HealYourselfMore(Stats stats) {
            this.stats = stats;
        }
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
            stats.hpHealed *= 1.1;
        }
    }

    public static class Aftershock implements Upgrade {
        private static final int MAX_BUYS = 1;
        private int bought = 0;
        private final Stats stats;

        public Aftershock(Stats stats) {
            this.stats = stats;
        }
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
            stats.aftershockActive = true;
            bought++;
        }

        @Override
        public boolean isAvailable() {
            return bought < MAX_BUYS;
        }
    }

    public static class Thorns implements Upgrade {
        private static final int MAX_BUYS = 1;
        private int bought = 0;
        private final Stats stats;

        public Thorns(Stats stats) {
            this.stats = stats;
        }
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
            stats.thornsActive = true;
            bought++;
        }

        @Override
        public boolean isAvailable() {
            return bought < MAX_BUYS;
        }
    }
    public static class SecondWind implements Upgrade {
        private static final int MAX_BUYS = 1;
        private int bought = 0;
        private final Stats stats;

        public SecondWind(Stats stats) {
            this.stats = stats;
        }
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
            stats.secondWindActive = true;
            bought++;
        }

        @Override
        public boolean isAvailable() {
            return bought < MAX_BUYS;
        }
    }

    public static class FasterThrows implements Upgrade {
        private static final int MAX_BUYS = 3;
        private int bought = 0;
        private final Stats stats;

        public FasterThrows(Stats stats) {
            this.stats = stats;
        }
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
            stats.placeCooldownFrames = Math.max(8, (int) (stats.placeCooldownFrames * 0.8));
            bought++;
        }

        @Override
        public boolean isAvailable() {
            return bought < MAX_BUYS;
        }
    }

    public static class LongArm implements Upgrade {
        private static final int MAX_BUYS = 3;
        private int bought = 0;
        private final Stats stats;

        public LongArm(Stats stats) {
            this.stats = stats;
        }
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
            stats.maxRange += 80;
            bought++;
        }

        @Override
        public boolean isAvailable() {
            return bought < MAX_BUYS;
        }
    }

    public static class Magnet implements Upgrade {
        private static final int MAX_BUYS = 3;
        private int bought = 0;
        private final Stats stats;

        public Magnet(Stats stats) {
            this.stats = stats;
        }
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
            stats.orbPullRange += 100;
            bought++;
        }

        @Override
        public boolean isAvailable() {
            return bought < MAX_BUYS;
        }
    }

    public static class HealingOrbs implements Upgrade {
        private static final int MAX_BUYS = 3;
        private int bought = 0;
        private final Stats stats;

        public HealingOrbs(Stats stats) {
            this.stats = stats;
        }
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
            stats.orbHeal += 5;
            bought++;
        }

        @Override
        public boolean isAvailable() {
            return bought < MAX_BUYS;
        }
    }

    public static class AuraMoreOften implements Upgrade {
        private static final int MAX_BUYS = 5;
        private int bought = 0;
        private final Stats stats;

        public AuraMoreOften(Stats stats) {
            this.stats = stats;
        }
        @Override
        public String getName() {
            return "Shorter Aura Cooldown";
        }

        @Override
        public String getDescription() {
            return "Aura comes off cooldown 3 seconds faster";
        }

        @Override
        public void apply() {
            stats.auraCooldownFrames -= 180;
            bought++;
        }

        @Override
        public boolean isAvailable() {
            return bought < MAX_BUYS;
        }
    }

    public static class AuraSlowsMore implements Upgrade {
        private static final int MAX_BUYS = 2;
        private int bought = 0;
        private final Stats stats;

        public AuraSlowsMore(Stats stats) {
            this.stats = stats;
        }

        @Override
        public String getName() {
            return "Aura Slows More";
        }

        @Override
        public String getDescription() {
            return "Aura slows enemies 20% more";
        }

        @Override
        public void apply() {
            stats.auraSlowMultiplier *= 0.8;
            bought++;
        }

        @Override
        public boolean isAvailable() {
            return bought < MAX_BUYS;
        }
    }
}
