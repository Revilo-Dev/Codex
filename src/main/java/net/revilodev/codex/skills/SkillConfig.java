package net.revilodev.codex.skills;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SkillConfig {
    private SkillConfig() {}

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.IntValue POINTS_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue POWER_DAMAGE_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue CRIT_POWER_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue HASTE_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue FIRE_RESIST_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue PROJECTILE_RESIST_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue KNOCKBACK_RESIST_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue LEAPING_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue REGEN_HEARTS_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue HEALTH_BOOST_HEARTS_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue CLEANSE_CHANCE_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue LOOTING_CHANCE_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue FORTUNE_CHANCE_PER_LEVEL;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("progression");
        POINTS_PER_LEVEL = builder.defineInRange("pointsPerLevel", 1, 0, 10);
        builder.pop();

        builder.push("scaling");
        POWER_DAMAGE_PER_LEVEL = builder.defineInRange("powerDamagePerLevel", 0.25D, 0.0D, 10.0D);
        CRIT_POWER_PER_LEVEL = builder.defineInRange("critPowerMultiplierPerLevel", 0.05D, 0.0D, 2.0D);
        HASTE_PER_LEVEL = builder.defineInRange("hasteAttackSpeedPerLevel", 0.03D, 0.0D, 2.0D);
        FIRE_RESIST_PER_LEVEL = builder.defineInRange("fireResistancePerLevel", 0.03D, 0.0D, 1.0D);
        PROJECTILE_RESIST_PER_LEVEL = builder.defineInRange("projectileResistancePerLevel", 0.03D, 0.0D, 1.0D);
        KNOCKBACK_RESIST_PER_LEVEL = builder.defineInRange("knockbackResistancePerLevel", 0.05D, 0.0D, 1.0D);
        LEAPING_PER_LEVEL = builder.defineInRange("leapingBonusPerLevel", 0.12D, 0.0D, 2.0D);
        REGEN_HEARTS_PER_LEVEL = builder.defineInRange("regenHeartsPerSecondPerLevel", 0.05D, 0.0D, 2.0D);
        HEALTH_BOOST_HEARTS_PER_LEVEL = builder.defineInRange("healthBoostHeartsPerLevel", 0.5D, 0.0D, 10.0D);
        CLEANSE_CHANCE_PER_LEVEL = builder.defineInRange("cleanseChancePerLevel", 0.02D, 0.0D, 1.0D);
        LOOTING_CHANCE_PER_LEVEL = builder.defineInRange("lootingExtraDropChancePerLevel", 0.04D, 0.0D, 1.0D);
        FORTUNE_CHANCE_PER_LEVEL = builder.defineInRange("fortuneExtraDropChancePerLevel", 0.04D, 0.0D, 1.0D);
        builder.pop();

        SPEC = builder.build();
    }

    public static int pointsPerLevel() { return POINTS_PER_LEVEL.get(); }
    public static double powerDamagePerLevel() { return POWER_DAMAGE_PER_LEVEL.get(); }
    public static double critPowerPerLevel() { return CRIT_POWER_PER_LEVEL.get(); }
    public static double hastePerLevel() { return HASTE_PER_LEVEL.get(); }
    public static double fireResistancePerLevel() { return FIRE_RESIST_PER_LEVEL.get(); }
    public static double projectileResistancePerLevel() { return PROJECTILE_RESIST_PER_LEVEL.get(); }
    public static double knockbackResistancePerLevel() { return KNOCKBACK_RESIST_PER_LEVEL.get(); }
    public static double leapingPerLevel() { return LEAPING_PER_LEVEL.get(); }
    public static double regenHeartsPerLevel() { return REGEN_HEARTS_PER_LEVEL.get(); }
    public static double healthBoostHeartsPerLevel() { return HEALTH_BOOST_HEARTS_PER_LEVEL.get(); }
    public static double cleanseChancePerLevel() { return CLEANSE_CHANCE_PER_LEVEL.get(); }
    public static double lootingChancePerLevel() { return LOOTING_CHANCE_PER_LEVEL.get(); }
    public static double fortuneChancePerLevel() { return FORTUNE_CHANCE_PER_LEVEL.get(); }
}
