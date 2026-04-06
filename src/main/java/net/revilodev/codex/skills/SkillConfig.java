package net.revilodev.codex.skills;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumMap;

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
    private static final ModConfigSpec.DoubleValue CLEANSE_IMMUNITIES_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue LUCK_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue LOOTING_CHANCE_PER_LEVEL;
    private static final ModConfigSpec.IntValue FORTUNE_BONUS_PER_LEVEL;
    private static final ModConfigSpec.BooleanValue SPAWN_WITH_SKILLS_BOOK;
    private static final EnumMap<SkillId, ModConfigSpec.IntValue> MAX_LEVELS = new EnumMap<>(SkillId.class);

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("progression");
        POINTS_PER_LEVEL = builder.defineInRange("pointsPerLevel", 1, 0, 10);
        builder.pop();

        builder.push("items");
        SPAWN_WITH_SKILLS_BOOK = builder.define("spawnWithSkillsBook", true);
        builder.pop();

        builder.push("maxLevels");
        for (SkillId id : SkillId.values()) {
            MAX_LEVELS.put(id, builder.defineInRange(id.name().toLowerCase(java.util.Locale.ROOT), id.defaultMaxLevel(), 1, 100));
        }
        builder.pop();

        builder.push("scaling");
        STRENGTH_DAMAGE_PER_LEVEL = builder.defineInRange("strengthDamagePerLevel", 0.5D, 0.0D, 20.0D);
        POWER_DAMAGE_PER_LEVEL = builder.defineInRange("powerDamagePerLevel", 1.0D, 0.0D, 20.0D);
        CRIT_POWER_DAMAGE_PER_LEVEL = builder.defineInRange("critPowerDamagePerLevel", 0.1D, 0.0D, 20.0D);
        HASTE_BREAK_SPEED_PER_LEVEL = builder.defineInRange("hasteBreakSpeedPerLevel", 2.0D, 0.0D, 20.0D);
        RESISTANCE_PER_LEVEL = builder.defineInRange("resistancePerLevel", 0.05D, 0.0D, 1.0D);
        FIRE_RESIST_PER_LEVEL = builder.defineInRange("fireResistancePerLevel", 0.05D, 0.0D, 1.0D);
        PROJECTILE_RESIST_PER_LEVEL = builder.defineInRange("projectileResistancePerLevel", 0.05D, 0.0D, 1.0D);
        KNOCKBACK_RESIST_PER_LEVEL = builder.defineInRange("knockbackResistancePerLevel", 0.05D, 0.0D, 1.0D);
        AGILITY_SPEED_PER_LEVEL = builder.defineInRange("agilitySpeedPerLevel", 0.10D, 0.0D, 2.0D);
        LEAPING_PER_LEVEL = builder.defineInRange("leapingBonusPerLevel", 0.20D, 0.0D, 2.0D);
        REGEN_HEARTS_PER_LEVEL = builder.defineInRange("regenHeartsPerSecondPerLevel", 0.05D, 0.0D, 2.0D);
        VITALITY_HEARTS_PER_LEVEL = builder.defineInRange("vitalityHeartsPerLevel", 1.0D, 0.0D, 10.0D);
        LIFE_LEACH_PER_LEVEL = builder.defineInRange("lifeLeachPerLevel", 0.01D, 0.0D, 1.0D);
        CLEANSE_IMMUNITIES_PER_LEVEL = builder.defineInRange("cleanseReductionPerLevel", 0.05D, 0.0D, 1.0D);
        LUCK_PER_LEVEL = builder.defineInRange("luckPerLevel", 1.0D, 0.0D, 10.0D);
        LOOTING_CHANCE_PER_LEVEL = builder.defineInRange("lootingExtraDropChancePerLevel", 0.02D, 0.0D, 1.0D);
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
    public static double cleanseImmunitiesPerLevel() { return CLEANSE_IMMUNITIES_PER_LEVEL.get(); }
    public static double luckPerLevel() { return LUCK_PER_LEVEL.get(); }
    public static double lootingChancePerLevel() { return LOOTING_CHANCE_PER_LEVEL.get(); }
    public static int fortuneBonusPerLevel() { return FORTUNE_BONUS_PER_LEVEL.get(); }
    public static boolean spawnWithSkillsBook() { return SPAWN_WITH_SKILLS_BOOK.get(); }
    public static int maxLevel(SkillId id) {
        ModConfigSpec.IntValue value = MAX_LEVELS.get(id);
        return value != null ? value.get() : (id == null ? 1 : id.defaultMaxLevel());
    }
}
