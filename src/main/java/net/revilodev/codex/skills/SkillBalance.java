package net.revilodev.codex.skills;

public final class SkillBalance {
    private SkillBalance() {}

    public static double powerDamage(int level) {
        return level * SkillConfig.powerDamagePerLevel();
    }

    public static double critPowerMultiplier(int level) {
        return level * SkillConfig.critPowerPerLevel();
    }

    public static double hasteAttackSpeed(int level) {
        return level * SkillConfig.hastePerLevel();
    }

    public static double fireResistance(int level) {
        return clamp(level * SkillConfig.fireResistancePerLevel(), 0.0D, 0.95D);
    }

    public static double projectileResistance(int level) {
        return clamp(level * SkillConfig.projectileResistancePerLevel(), 0.0D, 0.95D);
    }

    public static double knockbackResistance(int level) {
        return clamp(level * SkillConfig.knockbackResistancePerLevel(), 0.0D, 1.0D);
    }

    public static double leapingBonus(int level) {
        return level * SkillConfig.leapingPerLevel();
    }

    public static float regenHeartsPerSecond(int level) {
        return (float) (level * SkillConfig.regenHeartsPerLevel());
    }

    public static double healthBoostHearts(int level) {
        return level * SkillConfig.healthBoostHeartsPerLevel();
    }

    public static double cleanseChance(int level) {
        return clamp(level * SkillConfig.cleanseChancePerLevel(), 0.0D, 1.0D);
    }

    public static double lootingChance(int level) {
        return clamp(level * SkillConfig.lootingChancePerLevel(), 0.0D, 1.0D);
    }

    public static double fortuneChance(int level) {
        return clamp(level * SkillConfig.fortuneChancePerLevel(), 0.0D, 1.0D);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
