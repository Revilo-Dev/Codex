package net.revilodev.codex.abilities;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumMap;

public final class AbilityConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.IntValue POINT_INTERVAL_LEVELS;
    private static final ModConfigSpec.DoubleValue COOLDOWN_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue PRIMARY_SCALING_MULTIPLIER;
    private static final ModConfigSpec.BooleanValue HUD_ENABLED;
    private static final ModConfigSpec.BooleanValue HUD_TIMER_TEXT;
    private static final ModConfigSpec.IntValue MAX_SLOTS;
    private static final EnumMap<AbilityId, ModConfigSpec.BooleanValue> ENABLED = new EnumMap<>(AbilityId.class);

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("progression");
        POINT_INTERVAL_LEVELS = builder.defineInRange("abilityPointLevelInterval", 5, 1, 100);
        builder.pop();

        builder.push("general");
        COOLDOWN_MULTIPLIER = builder.defineInRange("cooldownMultiplier", 1.0D, 0.1D, 5.0D);
        PRIMARY_SCALING_MULTIPLIER = builder.defineInRange("primaryScalingMultiplier", 1.0D, 0.0D, 5.0D);
        HUD_ENABLED = builder.define("hudEnabled", true);
        HUD_TIMER_TEXT = builder.define("hudTimerText", true);
        MAX_SLOTS = builder.defineInRange("maxSlots", 5, 1, 5);
        builder.pop();

        builder.push("abilities");
        for (AbilityId id : AbilityId.values()) {
            ENABLED.put(id, builder.define(id.name().toLowerCase(java.util.Locale.ROOT), true));
        }
        builder.pop();

        SPEC = builder.build();
    }

    private AbilityConfig() {}

    public static int pointIntervalLevels() {
        return POINT_INTERVAL_LEVELS.get();
    }

    public static double cooldownMultiplier() {
        return COOLDOWN_MULTIPLIER.get();
    }

    public static double primaryScalingMultiplier() {
        return PRIMARY_SCALING_MULTIPLIER.get();
    }

    public static boolean hudEnabled() {
        return HUD_ENABLED.get();
    }

    public static boolean hudTimerText() {
        return HUD_TIMER_TEXT.get();
    }

    public static int maxSlots() {
        return MAX_SLOTS.get();
    }

    public static boolean enabled(AbilityId id) {
        ModConfigSpec.BooleanValue value = ENABLED.get(id);
        return value != null && value.get();
    }
}
