package net.revilodev.codex.skills;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SkillConfig {
    private SkillConfig() {}

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.IntValue POINTS_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue STRENGTH_DAMAGE_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue POWER_DAMAGE_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue CRIT_POWER_DAMAGE_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue HASTE_BREAK_SPEED_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue RESISTANCE_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue FIRE_RESIST_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue PROJECTILE_RESIST_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue KNOCKBACK_RESIST_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue AGILITY_SPEED_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue LEAPING_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue REGEN_HEARTS_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue VITALITY_HEARTS_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue LIFE_LEACH_PER_LEVEL;
    private static final ModConfigSpec.IntValue CLEANSE_IMMUNITIES_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue LUCK_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue LOOTING_CHANCE_PER_LEVEL;
    private static final ModConfigSpec.IntValue FORTUNE_BONUS_PER_LEVEL;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("progression");
        POINTS_PER_LEVEL = builder.defineInRange("pointsPerLevel", 1, 0, 10);
        builder.pop();

        builder.push("scaling");
        STRENGTH_DAMAGE_PER_LEVEL = builder.defineInRange("strengthDamagePerLevel", 1.0D, 0.0D, 20.0D);
        POWER_DAMAGE_PER_LEVEL = builder.defineInRange("powerDamagePerLevel", 4.0D, 0.0D, 20.0D);
        CRIT_POWER_DAMAGE_PER_LEVEL = builder.defineInRange("critPowerDamagePerLevel", 2.0D, 0.0D, 20.0D);
        HASTE_BREAK_SPEED_PER_LEVEL = builder.defineInRange("hasteBreakSpeedPerLevel", 1.0D, 0.0D, 20.0D);
        RESISTANCE_PER_LEVEL = builder.defineInRange("resistancePerLevel", 0.05D, 0.0D, 1.0D);
        FIRE_RESIST_PER_LEVEL = builder.defineInRange("fireResistancePerLevel", 0.05D, 0.0D, 1.0D);
        PROJECTILE_RESIST_PER_LEVEL = builder.defineInRange("projectileResistancePerLevel", 0.05D, 0.0D, 1.0D);
        KNOCKBACK_RESIST_PER_LEVEL = builder.defineInRange("knockbackResistancePerLevel", 0.05D, 0.0D, 1.0D);
        AGILITY_SPEED_PER_LEVEL = builder.defineInRange("agilitySpeedPerLevel", 0.05D, 0.0D, 2.0D);
        LEAPING_PER_LEVEL = builder.defineInRange("leapingBonusPerLevel", 0.05D, 0.0D, 2.0D);
        REGEN_HEARTS_PER_LEVEL = builder.defineInRange("regenHeartsPerSecondPerLevel", 0.05D, 0.0D, 2.0D);
        VITALITY_HEARTS_PER_LEVEL = builder.defineInRange("vitalityHeartsPerLevel", 1.0D, 0.0D, 10.0D);
        LIFE_LEACH_PER_LEVEL = builder.defineInRange("lifeLeachPerLevel", 0.05D, 0.0D, 1.0D);
        CLEANSE_IMMUNITIES_PER_LEVEL = builder.defineInRange("cleanseImmunitiesPerLevel", 1, 0, 10);
        LUCK_PER_LEVEL = builder.defineInRange("luckPerLevel", 1.0D, 0.0D, 10.0D);
        LOOTING_CHANCE_PER_LEVEL = builder.defineInRange("lootingExtraDropChancePerLevel", 0.20D, 0.0D, 1.0D);
        FORTUNE_BONUS_PER_LEVEL = builder.defineInRange("fortuneBonusPerLevel", 1, 0, 10);
        builder.pop();

        SPEC = builder.build();
    }

    public static int pointsPerLevel() { return POINTS_PER_LEVEL.get(); }
    public static double strengthDamagePerLevel() { return STRENGTH_DAMAGE_PER_LEVEL.get(); }
    public static double powerDamagePerLevel() { return POWER_DAMAGE_PER_LEVEL.get(); }
    public static double critPowerDamagePerLevel() { return CRIT_POWER_DAMAGE_PER_LEVEL.get(); }
    public static double hasteBreakSpeedPerLevel() { return HASTE_BREAK_SPEED_PER_LEVEL.get(); }
    public static double resistancePerLevel() { return RESISTANCE_PER_LEVEL.get(); }
    public static double fireResistancePerLevel() { return FIRE_RESIST_PER_LEVEL.get(); }
    public static double projectileResistancePerLevel() { return PROJECTILE_RESIST_PER_LEVEL.get(); }
    public static double knockbackResistancePerLevel() { return KNOCKBACK_RESIST_PER_LEVEL.get(); }
    public static double agilitySpeedPerLevel() { return AGILITY_SPEED_PER_LEVEL.get(); }
    public static double leapingPerLevel() { return LEAPING_PER_LEVEL.get(); }
    public static double regenHeartsPerLevel() { return REGEN_HEARTS_PER_LEVEL.get(); }
    public static double vitalityHeartsPerLevel() { return VITALITY_HEARTS_PER_LEVEL.get(); }
    public static double lifeLeachPerLevel() { return LIFE_LEACH_PER_LEVEL.get(); }
    public static int cleanseImmunitiesPerLevel() { return CLEANSE_IMMUNITIES_PER_LEVEL.get(); }
    public static double luckPerLevel() { return LUCK_PER_LEVEL.get(); }
    public static double lootingChancePerLevel() { return LOOTING_CHANCE_PER_LEVEL.get(); }
    public static int fortuneBonusPerLevel() { return FORTUNE_BONUS_PER_LEVEL.get(); }
}
