package net.revilodev.codex.abilities.logic;

import net.revilodev.codex.abilities.AbilityConfig;
import net.revilodev.codex.abilities.AbilityId;
import net.revilodev.codex.skills.PlayerSkills;

import java.util.Locale;

public final class AbilityScaling {
    private AbilityScaling() {}

    public static int cooldownTicks(AbilityId id, int rank, PlayerSkills skills) {
        double out = AbilityConfig.configuredCooldown(id);
        out -= (Math.max(1, rank) - 1) * 8.0D;
        out *= AbilityConfig.cooldownMultiplier();
        return Math.max(10, (int) Math.round(out));
    }

    public static float damage(AbilityId id, int coreRank, double abilityPower) {
        double base = AbilityConfig.damage(id) * (1.0D + (Math.max(1, coreRank) - 1) * 0.15D * AbilityConfig.primaryScalingMultiplier());
        return (float) (base * Math.max(0.0D, abilityPower));
    }

    public static double radius(AbilityId id, int coreRank, double abilityPower) {
        double base = AbilityConfig.radius(id) * (1.0D + (Math.max(1, coreRank) - 1) * 0.1D * AbilityConfig.primaryScalingMultiplier());
        return base * (0.85D + (Math.max(0.0D, abilityPower) * 0.15D));
    }

    public static int durationTicks(AbilityId id, int coreRank, double abilityPower) {
        double base = AbilityConfig.durationTicks(id) * (1.0D + (Math.max(1, coreRank) - 1) * 0.1D * AbilityConfig.primaryScalingMultiplier());
        return Math.max(1, (int) Math.round(base * (0.85D + (Math.max(0.0D, abilityPower) * 0.15D))));
    }

    public static String summary(AbilityId id, int rank, PlayerSkills skills) {
        return "Damage " + fmt(damage(id, rank, 1.0D)) + " | Radius " + fmt(radius(id, rank, 1.0D)) + " | Duration " + fmt(durationTicks(id, rank, 1.0D) / 20.0D) + "s";
    }

    private static String fmt(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-9) return Integer.toString((int) Math.rint(value));
        String out = String.format(Locale.ROOT, "%.2f", value);
        while (out.contains(".") && (out.endsWith("0") || out.endsWith("."))) out = out.substring(0, out.length() - 1);
        return out;
    }
}
