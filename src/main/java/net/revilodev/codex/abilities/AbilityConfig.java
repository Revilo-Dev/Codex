package net.revilodev.codex.abilities;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumMap;

public final class AbilityConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.IntValue POINT_INTERVAL_LEVELS;
    private static final ModConfigSpec.BooleanValue ENABLE_ABILITIES;
    private static final ModConfigSpec.DoubleValue COOLDOWN_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue PRIMARY_SCALING_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue ABILITY_POWER_LEVEL_1;
    private static final ModConfigSpec.DoubleValue ABILITY_POWER_LEVEL_2;
    private static final ModConfigSpec.DoubleValue ABILITY_POWER_LEVEL_3;
    private static final ModConfigSpec.BooleanValue ENABLE_HUD;
    private static final ModConfigSpec.ConfigValue<String> HUD_POSITION;
    private static final ModConfigSpec.BooleanValue HUD_TIMER_TEXT;
    private static final ModConfigSpec.IntValue MAX_SLOTS;
    private static final EnumMap<AbilityId, ModConfigSpec.IntValue> MAX_RANKS = new EnumMap<>(AbilityId.class);
    private static final EnumMap<AbilityId, ModConfigSpec.BooleanValue> ENABLED = new EnumMap<>(AbilityId.class);
    private static final EnumMap<AbilityId, ModConfigSpec.IntValue> COOLDOWNS = new EnumMap<>(AbilityId.class);
    private static final EnumMap<AbilityId, ModConfigSpec.DoubleValue> DAMAGE = new EnumMap<>(AbilityId.class);
    private static final EnumMap<AbilityId, ModConfigSpec.DoubleValue> RADIUS = new EnumMap<>(AbilityId.class);
    private static final EnumMap<AbilityId, ModConfigSpec.IntValue> DURATION = new EnumMap<>(AbilityId.class);

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("progression");
        POINT_INTERVAL_LEVELS = builder.defineInRange("abilityPointLevelInterval", 5, 1, 100);
        builder.pop();

        builder.push("general");
        ENABLE_ABILITIES = builder.define("enableAbilities", true);
        COOLDOWN_MULTIPLIER = builder.defineInRange("cooldownMultiplier", 1.0D, 0.1D, 5.0D);
        PRIMARY_SCALING_MULTIPLIER = builder.defineInRange("primaryScalingMultiplier", 1.0D, 0.0D, 5.0D);
        ABILITY_POWER_LEVEL_1 = builder.defineInRange("abilityPowerEnchantmentLevel1", 0.05D, 0.0D, 2.0D);
        ABILITY_POWER_LEVEL_2 = builder.defineInRange("abilityPowerEnchantmentLevel2", 0.10D, 0.0D, 2.0D);
        ABILITY_POWER_LEVEL_3 = builder.defineInRange("abilityPowerEnchantmentLevel3", 0.15D, 0.0D, 2.0D);
        ENABLE_HUD = builder.define("enableHud", true);
        HUD_POSITION = builder.define("hudPosition", "bottom-left", value -> {
            if (!(value instanceof String raw)) return false;
            return HudPosition.fromConfig(raw) != null;
        });
        HUD_TIMER_TEXT = builder.define("hudTimerText", true);
        MAX_SLOTS = builder.defineInRange("maxSlots", 5, 1, 5);
        builder.pop();

        builder.push("abilities");
        for (AbilityId id : AbilityId.values()) {
            ENABLED.put(id, builder.define(id.name().toLowerCase(java.util.Locale.ROOT), true));
        }
        builder.pop();

        builder.push("maxRanks");
        for (AbilityId id : AbilityId.values()) {
            MAX_RANKS.put(id, builder.defineInRange(id.name().toLowerCase(java.util.Locale.ROOT), id.defaultMaxRank(), 1, 100));
        }
        builder.pop();

        builder.push("values");
        for (AbilityId id : AbilityId.values()) {
            String key = id.name().toLowerCase(java.util.Locale.ROOT);
            COOLDOWNS.put(id, builder.defineInRange(key + "CooldownTicks", id.baseCooldownTicks(), 0, 72000));
            DAMAGE.put(id, builder.defineInRange(key + "Damage", 6.0D, 0.0D, 1000.0D));
            RADIUS.put(id, builder.defineInRange(key + "Radius", 3.0D, 0.0D, 128.0D));
            DURATION.put(id, builder.defineInRange(key + "DurationTicks", 100, 0, 72000));
        }
        builder.pop();

        SPEC = builder.build();
    }

    private AbilityConfig() {}

    public static int pointIntervalLevels() {
        return POINT_INTERVAL_LEVELS.get();
    }

    public static boolean abilitiesEnabled() {
        return ENABLE_ABILITIES.get();
    }

    public static double cooldownMultiplier() {
        return COOLDOWN_MULTIPLIER.get();
    }

    public static double primaryScalingMultiplier() {
        return PRIMARY_SCALING_MULTIPLIER.get();
    }

    public static boolean hudEnabled() {
        return abilitiesEnabled() && ENABLE_HUD.get();
    }

    public static HudPosition hudPosition() {
        HudPosition parsed = HudPosition.fromConfig(HUD_POSITION.get());
        return parsed == null ? HudPosition.BOTTOM_LEFT : parsed;
    }

    public static boolean hudTimerText() {
        return HUD_TIMER_TEXT.get();
    }

    public static int maxSlots() {
        return MAX_SLOTS.get();
    }

    public static int configuredCooldown(AbilityId id) {
        ModConfigSpec.IntValue value = COOLDOWNS.get(id);
        return value == null ? (id == null ? 0 : id.baseCooldownTicks()) : value.get();
    }

    public static double damage(AbilityId id) {
        ModConfigSpec.DoubleValue value = DAMAGE.get(id);
        return value == null ? 6.0D : value.get();
    }

    public static double radius(AbilityId id) {
        ModConfigSpec.DoubleValue value = RADIUS.get(id);
        return value == null ? 3.0D : value.get();
    }

    public static int durationTicks(AbilityId id) {
        ModConfigSpec.IntValue value = DURATION.get(id);
        return value == null ? 100 : value.get();
    }

    public static double abilityPowerEnchantScale(int level) {
        return switch (level) {
            case 1 -> ABILITY_POWER_LEVEL_1.get();
            case 2 -> ABILITY_POWER_LEVEL_2.get();
            case 3 -> ABILITY_POWER_LEVEL_3.get();
            default -> 0.0D;
        };
    }

    public static boolean enabled(AbilityId id) {
        if (!abilitiesEnabled()) return false;
        ModConfigSpec.BooleanValue value = ENABLED.get(id);
        return value != null && value.get();
    }

    public static int maxRank(AbilityId id) {
        ModConfigSpec.IntValue value = MAX_RANKS.get(id);
        return value != null ? value.get() : (id == null ? 1 : id.defaultMaxRank());
    }

    public enum HudPosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT;

        public static HudPosition fromConfig(String value) {
            if (value == null) return null;
            String normalized = value.trim()
                    .toLowerCase(java.util.Locale.ROOT)
                    .replace('_', '-')
                    .replace(',', '-');
            return switch (normalized) {
                case "top-left" -> TOP_LEFT;
                case "top-right" -> TOP_RIGHT;
                case "bottom-left" -> BOTTOM_LEFT;
                case "bottom-right" -> BOTTOM_RIGHT;
                default -> null;
            };
        }
    }
}
