package net.revilodev.codex.skills;

public final class SkillBalance {
    private SkillBalance() {}

    public static double strengthDamage(int level) {
        return level * SkillConfig.strengthDamagePerLevel();
    }

    public static double powerDamage(int level) {
        return level * SkillConfig.powerDamagePerLevel();
    }

    public static double critPowerDamage(int level) {
        return level * SkillConfig.critPowerDamagePerLevel();
    }

    public static double hasteBreakSpeed(int level) {
        return level * SkillConfig.hasteBreakSpeedPerLevel();
    }

    public static double resistance(int level) {
        return clamp(level * SkillConfig.resistancePerLevel(), 0.0D, 0.95D);
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

    public static double agilitySpeed(int level) {
        return level * SkillConfig.agilitySpeedPerLevel();
    }

    public static double leapingBonus(int level) {
        return level * SkillConfig.leapingPerLevel();
    }

    public static float regenHeartsPerSecond(int level) {
        return (float) (level * SkillConfig.regenHeartsPerLevel());
    }

    public static double vitalityHearts(int level) {
        return level * SkillConfig.vitalityHeartsPerLevel();
    }

    public static double lifeLeach(int level) {
        return clamp(level * SkillConfig.lifeLeachPerLevel(), 0.0D, 1.0D);
    }

    public static int cleanseImmunities(int level) {
        return Math.max(0, level * SkillConfig.cleanseImmunitiesPerLevel());
    }

    public static double luck(int level) {
        return level * SkillConfig.luckPerLevel();
    }

    public static double lootingChance(int level) {
        return clamp(level * SkillConfig.lootingChancePerLevel(), 0.0D, 1.0D);
    }

    public static int fortuneBonus(int level) {
        return Math.max(0, level * SkillConfig.fortuneBonusPerLevel());
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
