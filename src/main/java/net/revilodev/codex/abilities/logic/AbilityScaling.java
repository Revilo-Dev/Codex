package net.revilodev.codex.abilities.logic;

import net.minecraft.util.Mth;
import net.revilodev.codex.abilities.AbilityConfig;
import net.revilodev.codex.abilities.AbilityId;
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.skills.SkillId;

import java.util.Locale;

public final class AbilityScaling {
    private AbilityScaling() {}

    public static int cooldownTicks(AbilityId id, int rank, PlayerSkills skills) {
        double cooldown = id.baseCooldownTicks();
        cooldown -= rank * 8.0D;
        cooldown -= primaryLevel(skills, id.scalingSkill()) * 2.0D * AbilityConfig.primaryScalingMultiplier();
        cooldown *= AbilityConfig.cooldownMultiplier();
        return Math.max(20, (int) Math.round(cooldown));
    }

    public static double dashDistance(int rank, PlayerSkills skills, double abilityPower) {
        return scaled(0.9D + (rank * 0.22D) + primaryLevel(skills, SkillId.AGILITY) * 0.045D * AbilityConfig.primaryScalingMultiplier(), abilityPower, 0.2D);
    }

    public static double leapVertical(int rank, PlayerSkills skills, double abilityPower) {
        return scaled(0.56D + (rank * 0.04D) + primaryLevel(skills, SkillId.AGILITY) * 0.012D * AbilityConfig.primaryScalingMultiplier(), abilityPower, 0.2D);
    }

    public static double leapForward(int rank, PlayerSkills skills, double abilityPower) {
        return scaled(0.7D + (rank * 0.14D) + primaryLevel(skills, SkillId.AGILITY) * 0.03D * AbilityConfig.primaryScalingMultiplier(), abilityPower, 0.2D);
    }

    public static float healAmount(int rank, PlayerSkills skills, double abilityPower) {
        return (float) scaled(3.0D + rank * 1.2D + primaryLevel(skills, SkillId.VITALITY) * 0.35D * AbilityConfig.primaryScalingMultiplier(), abilityPower, 0.8D);
    }

    public static float cleanseHeal(int rank, PlayerSkills skills, double abilityPower) {
        return (float) scaled(1.0D + rank * 0.5D + primaryLevel(skills, SkillId.VITALITY) * 0.15D * AbilityConfig.primaryScalingMultiplier(), abilityPower, 0.75D);
    }

    public static int guardDurationTicks(int rank, PlayerSkills skills, double abilityPower) {
        return scaledTicks(60 + rank * 20 + primaryLevel(skills, SkillId.RESISTANCE) * 4.0D * AbilityConfig.primaryScalingMultiplier(), abilityPower, 0.5D);
    }

    public static int guardAmplifier(int rank, PlayerSkills skills) {
        return primaryLevel(skills, SkillId.RESISTANCE) >= 8 || rank >= 4 ? 1 : 0;
    }

    public static int warcryDurationTicks(int rank, PlayerSkills skills, double abilityPower) {
        return scaledTicks(200, abilityPower, 0.5D);
    }

    public static int warcryStrengthAmp(int rank, PlayerSkills skills) {
        return Math.max(0, rank - 1);
    }

    public static float executionDamage(int rank, PlayerSkills skills, double abilityPower) {
        return (float) scaled(5.0D + rank * 1.8D + primaryLevel(skills, SkillId.STRENGTH) * 0.55D * AbilityConfig.primaryScalingMultiplier(), abilityPower, 1.0D);
    }

    public static float executionMissingHealthBonus(int rank) {
        return 1.10F + (rank * 0.08F);
    }

    public static float cleaveDamage(int rank, PlayerSkills skills, double abilityPower) {
        return (float) scaled(3.7D + rank * 1.2D + primaryLevel(skills, SkillId.STRENGTH) * 0.45D * AbilityConfig.primaryScalingMultiplier(), abilityPower, 1.0D);
    }

    public static double cleaveRadius(int rank, double abilityPower) {
        return scaled(2.4D + rank * 0.22D, abilityPower, 0.6D);
    }

    public static float overpowerDamage(int rank, PlayerSkills skills, double abilityPower) {
        return (float) scaled(6.0D + rank * 1.65D + primaryLevel(skills, SkillId.STRENGTH) * 0.55D * AbilityConfig.primaryScalingMultiplier(), abilityPower, 1.0D);
    }

    public static double overpowerKnockback(int rank, PlayerSkills skills, double abilityPower) {
        return scaled(0.5D + rank * 0.16D + primaryLevel(skills, SkillId.STRENGTH) * 0.03D * AbilityConfig.primaryScalingMultiplier(), abilityPower, 0.35D);
    }

    public static double toxicRadius(int rank, PlayerSkills skills, double abilityPower) {
        return scaled(2.1D + rank * 0.4D + primaryLevel(skills, SkillId.LUCK) * 0.06D * AbilityConfig.primaryScalingMultiplier(), abilityPower, 0.7D);
    }

    public static int toxicPoisonTicks(int rank, PlayerSkills skills, double abilityPower) {
        double out = 40 + rank * 24 + primaryLevel(skills, SkillId.LUCK) * 5.0D * AbilityConfig.primaryScalingMultiplier();
        return Math.max(20, scaledTicks(out, abilityPower, 0.7D));
    }

    public static int toxicAmplifier(int rank) {
        return rank >= 4 ? 1 : 0;
    }

    public static float toxicDamage(int rank, PlayerSkills skills, double abilityPower) {
        return (float) scaled(1.5D + rank * 0.75D + primaryLevel(skills, SkillId.LUCK) * 0.22D * AbilityConfig.primaryScalingMultiplier(), abilityPower, 0.9D);
    }

    public static String summary(AbilityId id, int rank, PlayerSkills skills) {
        return switch (id) {
            case DASH -> "Distance +" + fmt(dashDistance(rank, skills, 1.0D));
            case LEAP -> "Leap " + fmt(leapVertical(rank, skills, 1.0D)) + " up / " + fmt(leapForward(rank, skills, 1.0D)) + " forward";
            case LUNGE -> "Dash " + fmt(lungeDistance(rank, 0, skills, 1.0D)) + "m / " + fmt(lungeDamage(rank, 0, skills, 1.0D)) + "+ speed dmg";
            case HEAL -> "Restore " + fmt(healAmount(rank, skills, 1.0D) / 2.0F) + " hearts";
            case CLEANSE -> "Remove debuffs and heal " + fmt(cleanseHeal(rank, skills, 1.0D) / 2.0F) + " hearts";
            case GUARD -> "Resist for " + fmt(guardDurationTicks(rank, skills, 1.0D) / 20.0D) + "s";
            case WARCRY -> "Strength " + (warcryStrengthAmp(rank, skills) + 1) + " for " + fmt(warcryDurationTicks(rank, skills, 1.0D) / 20.0D) + "s";
            case EXECUTION -> "Hit for " + fmt(executionDamage(rank, skills, 1.0D)) + " dmg";
            case CLEAVE -> "Arc hit " + fmt(cleaveRadius(rank, 1.0D)) + "m";
            case OVERPOWER -> "Smash for " + fmt(overpowerDamage(rank, skills, 1.0D)) + " dmg";
            case BLAST -> "Burst " + fmt(blastRadius(rank, skills, 1.0D)) + "m / " + fmt(blastDamage(rank, skills, 1.0D)) + " dmg";
            case BLAZE -> fmt(blazeRadius(rank, skills, 1.0D)) + "m fire ring / " + fmt(blazeBurnSeconds(rank, 1.0D)) + "s burn";
            case GLACIER -> fmt(glacierDamage(rank, skills, 1.0D)) + " dmg / " + glacierProjectiles(rank) + " projectiles" + (rank >= AbilityId.GLACIER.maxRank() ? " / shrapnel burst" : "");
            case SMITE -> smiteTargets(rank) + " bolts / " + fmt(smiteDamage(rank, skills, 1.0D)) + " dmg";
            case SCAVENGER -> fmt(toxicRadius(rank, skills, 1.0D)) + "m burst / " + fmt(toxicDamage(rank, skills, 1.0D)) + " dmg / " + fmt(toxicPoisonTicks(rank, skills, 1.0D) / 20.0D) + "s poison";
        };
    }

    public static double lungeDistance(int rank, int dashRank, PlayerSkills skills, double abilityPower) {
        return scaled(2.4D + rank * 0.5D + dashRank * 0.22D + primaryLevel(skills, SkillId.AGILITY) * 0.08D * AbilityConfig.primaryScalingMultiplier(), abilityPower, 0.35D);
    }

    public static float lungeDamage(int rank, int dashRank, PlayerSkills skills, double abilityPower) {
        return (float) scaled(4.0D + rank + dashRank * 0.3D + primaryLevel(skills, SkillId.AGILITY) * 0.35D * AbilityConfig.primaryScalingMultiplier(), abilityPower, 1.0D);
    }

    public static float lungeSpeedDamageBonus(double horizontalSpeed) {
        return (float) Math.max(0.0D, horizontalSpeed * 6.0D);
    }

    public static double lungeKnockback(int rank, int dashRank, PlayerSkills skills, double abilityPower) {
        return scaled(0.5D + rank * 0.14D + dashRank * 0.05D + primaryLevel(skills, SkillId.AGILITY) * 0.03D * AbilityConfig.primaryScalingMultiplier(), abilityPower, 0.35D);
    }

    public static float blastDamage(int rank, PlayerSkills skills, double abilityPower) {
        return (float) scaled(3.8D + rank * 1.05D + primaryLevel(skills, SkillId.STRENGTH) * 0.36D * AbilityConfig.primaryScalingMultiplier(), abilityPower, 1.0D);
    }

    public static double blastRadius(int rank, PlayerSkills skills, double abilityPower) {
        return scaled(2.3D + rank * 0.28D + primaryLevel(skills, SkillId.STRENGTH) * 0.03D * AbilityConfig.primaryScalingMultiplier(), abilityPower, 0.7D);
    }

    public static double blazeRadius(int rank, PlayerSkills skills, double abilityPower) {
        return scaled(2.5D + rank + primaryLevel(skills, SkillId.RESISTANCE) * 0.08D * AbilityConfig.primaryScalingMultiplier(), abilityPower, 0.7D);
    }

    public static int blazeBurnSeconds(int rank, double abilityPower) {
        return Math.max(1, (int) Math.round(scaled(2 + rank, abilityPower, 0.75D)));
    }

    public static float glacierDamage(int rank, PlayerSkills skills, double abilityPower) {
        return (float) scaled(3.0D + rank + primaryLevel(skills, SkillId.LUCK) * 0.4D * AbilityConfig.primaryScalingMultiplier(), abilityPower, 1.0D);
    }

    public static int glacierProjectiles(int rank) {
        return Math.max(1, rank);
    }

    public static float smiteDamage(int rank, PlayerSkills skills, double abilityPower) {
        return (float) scaled(5.0D + rank * 1.2D + primaryLevel(skills, SkillId.RESISTANCE) * 0.45D * AbilityConfig.primaryScalingMultiplier(), abilityPower, 1.0D);
    }

    public static int smiteTargets(int rank) {
        return 1 + Math.max(0, rank - 1);
    }

    public static double smiteRadius(int rank, PlayerSkills skills, double abilityPower) {
        return scaled(6.0D + rank * 1.1D + primaryLevel(skills, SkillId.RESISTANCE) * 0.12D * AbilityConfig.primaryScalingMultiplier(), abilityPower, 0.7D);
    }

    private static int primaryLevel(PlayerSkills skills, SkillId id) {
        return skills == null || id == null ? 0 : skills.level(id);
    }

    private static double scaled(double value, double abilityPower, double sensitivity) {
        double bonus = Math.max(0.0D, abilityPower - 1.0D);
        return value * (1.0D + bonus * sensitivity);
    }

    private static int scaledTicks(double value, double abilityPower, double sensitivity) {
        return Math.max(1, Mth.floor(scaled(value, abilityPower, sensitivity)));
    }

    private static String fmt(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-9) return Integer.toString((int) Math.rint(value));
        String out = String.format(Locale.ROOT, "%.2f", value);
        while (out.contains(".") && (out.endsWith("0") || out.endsWith("."))) out = out.substring(0, out.length() - 1);
        return out;
    }
}
