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

    public static double dashDistance(int rank, PlayerSkills skills) {
        return 0.9D + (rank * 0.22D) + primaryLevel(skills, SkillId.AGILITY) * 0.045D * AbilityConfig.primaryScalingMultiplier();
    }

    public static double leapVertical(int rank, PlayerSkills skills) {
        return 0.56D + (rank * 0.04D) + primaryLevel(skills, SkillId.AGILITY) * 0.012D * AbilityConfig.primaryScalingMultiplier();
    }

    public static double leapForward(int rank, PlayerSkills skills) {
        return 0.7D + (rank * 0.14D) + primaryLevel(skills, SkillId.AGILITY) * 0.03D * AbilityConfig.primaryScalingMultiplier();
    }

    public static float healAmount(int rank, PlayerSkills skills) {
        return (float) (3.0D + rank * 1.2D + primaryLevel(skills, SkillId.VITALITY) * 0.35D * AbilityConfig.primaryScalingMultiplier());
    }

    public static float cleanseHeal(int rank, PlayerSkills skills) {
        return (float) (1.0D + rank * 0.5D + primaryLevel(skills, SkillId.VITALITY) * 0.15D * AbilityConfig.primaryScalingMultiplier());
    }

    public static int guardDurationTicks(int rank, PlayerSkills skills) {
        return 60 + rank * 20 + (int) Math.round(primaryLevel(skills, SkillId.RESISTANCE) * 4.0D * AbilityConfig.primaryScalingMultiplier());
    }

    public static int guardAmplifier(int rank, PlayerSkills skills) {
        return primaryLevel(skills, SkillId.RESISTANCE) >= 8 || rank >= 4 ? 1 : 0;
    }

    public static int warcryDurationTicks(int rank, PlayerSkills skills) {
        return 80 + rank * 20 + (int) Math.round(primaryLevel(skills, SkillId.STRENGTH) * 5.0D * AbilityConfig.primaryScalingMultiplier());
    }

    public static int warcryStrengthAmp(int rank, PlayerSkills skills) {
        return rank >= 4 || primaryLevel(skills, SkillId.STRENGTH) >= 7 ? 1 : 0;
    }

    public static float executionDamage(int rank, PlayerSkills skills) {
        return (float) (5.0D + rank * 1.8D + primaryLevel(skills, SkillId.STRENGTH) * 0.55D * AbilityConfig.primaryScalingMultiplier());
    }

    public static float executionMissingHealthBonus(int rank) {
        return 1.10F + (rank * 0.08F);
    }

    public static float cleaveDamage(int rank, PlayerSkills skills) {
        return (float) (4.0D + rank * 1.25D + primaryLevel(skills, SkillId.STRENGTH) * 0.45D * AbilityConfig.primaryScalingMultiplier());
    }

    public static double cleaveRadius(int rank) {
        return 2.4D + rank * 0.22D;
    }

    public static float overpowerDamage(int rank, PlayerSkills skills) {
        return (float) (6.0D + rank * 1.65D + primaryLevel(skills, SkillId.STRENGTH) * 0.55D * AbilityConfig.primaryScalingMultiplier());
    }

    public static double overpowerKnockback(int rank, PlayerSkills skills) {
        return 0.5D + rank * 0.16D + primaryLevel(skills, SkillId.STRENGTH) * 0.03D * AbilityConfig.primaryScalingMultiplier();
    }

    public static double magnetismRadius(int rank, PlayerSkills skills) {
        return 3.5D + rank * 0.45D + primaryLevel(skills, SkillId.LUCK) * 0.08D * AbilityConfig.primaryScalingMultiplier();
    }

    public static int magnetismDurationTicks(int rank, PlayerSkills skills) {
        double out = 60 + rank * 24 + primaryLevel(skills, SkillId.LUCK) * 6.0D * AbilityConfig.primaryScalingMultiplier();
        return Math.max(20, Mth.floor(out));
    }

    public static String summary(AbilityId id, int rank, PlayerSkills skills) {
        return switch (id) {
            case DASH -> "Distance +" + fmt(dashDistance(rank, skills));
            case LEAP -> "Leap " + fmt(leapVertical(rank, skills)) + " up / " + fmt(leapForward(rank, skills)) + " forward";
            case HEAL -> "Restore " + fmt(healAmount(rank, skills) / 2.0F) + " hearts";
            case CLEANSE -> "Remove debuffs and heal " + fmt(cleanseHeal(rank, skills) / 2.0F) + " hearts";
            case GUARD -> "Resist for " + fmt(guardDurationTicks(rank, skills) / 20.0D) + "s";
            case WARCRY -> "Buff for " + fmt(warcryDurationTicks(rank, skills) / 20.0D) + "s";
            case EXECUTION -> "Hit for " + fmt(executionDamage(rank, skills)) + " dmg";
            case CLEAVE -> "Arc hit " + fmt(cleaveRadius(rank)) + "m";
            case OVERPOWER -> "Smash for " + fmt(overpowerDamage(rank, skills)) + " dmg";
            case SCAVENGER -> fmt(magnetismDurationTicks(rank, skills) / 20.0D) + "s pull / " + fmt(magnetismRadius(rank, skills)) + "m";
        };
    }

    private static int primaryLevel(PlayerSkills skills, SkillId id) {
        return skills == null || id == null ? 0 : skills.level(id);
    }

    private static String fmt(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-9) return Integer.toString((int) Math.rint(value));
        String out = String.format(Locale.ROOT, "%.2f", value);
        while (out.contains(".") && (out.endsWith("0") || out.endsWith("."))) out = out.substring(0, out.length() - 1);
        return out;
    }
}
