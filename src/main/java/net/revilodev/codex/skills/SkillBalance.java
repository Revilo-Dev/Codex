package net.revilodev.codex.skills;

public final class SkillBalance {
    private SkillBalance() {}

    public static final double SHARPNESS_MAX_DAMAGE = 5.0D;
    public static final double POWER_MAX_DAMAGE = 5.0D;
    public static final double CRIT_BONUS_MAX_PCT = 5.0D;
    public static final double FIRE_RES_MAX_PCT = 30.0D;
    public static final double BLAST_RES_MAX_PCT = 30.0D;
    public static final double PROJECTILE_RES_MAX_PCT = 30.0D;
    public static final double KNOCKBACK_RES_MAX_PCT = 50.0D;

    public static final double HEALTH_MAX_POINTS = 10.0D;
    public static final double REGEN_MAX_PCT = 100.0D;
    public static final double SWIFTNESS_MAX_PCT = 50.0D;
    public static final double DEFENSE_MAX_POINTS = 4.0D;
    public static final double SATURATION_MAX_PCT = 100.0D;

    public static final double EFFICIENCY_MAX_PCT = 100.0D;
    public static final double CHOPPING_MAX_PCT = 100.0D;
    public static final double FORAGING_MAX_PCT = 200.0D;
    public static final double FORTUNE_MAX_PCT = 200.0D;
    public static final double LOOTING_MAX_PCT = 100.0D;

    public static final double SHARPNESS_DAMAGE_PER_LEVEL = SHARPNESS_MAX_DAMAGE / SkillId.SHARPNESS.maxLevel();
    public static final double POWER_DAMAGE_PER_LEVEL = POWER_MAX_DAMAGE / SkillId.POWER.maxLevel();
    public static final double CRIT_BONUS_PCT_PER_LEVEL = CRIT_BONUS_MAX_PCT / SkillId.CRIT_BONUS.maxLevel();
    public static final double FIRE_RES_PCT_PER_LEVEL = FIRE_RES_MAX_PCT / SkillId.FIRE_RESISTANCE.maxLevel();
    public static final double BLAST_RES_PCT_PER_LEVEL = BLAST_RES_MAX_PCT / SkillId.BLAST_RESISTANCE.maxLevel();
    public static final double PROJECTILE_RES_PCT_PER_LEVEL = PROJECTILE_RES_MAX_PCT / SkillId.PROJECTILE_RESISTANCE.maxLevel();
    public static final double KNOCKBACK_RES_PCT_PER_LEVEL = KNOCKBACK_RES_MAX_PCT / SkillId.KNOCKBACK_RESISTANCE.maxLevel();

    public static final double HEALTH_POINTS_PER_LEVEL = HEALTH_MAX_POINTS / SkillId.HEALTH.maxLevel();
    public static final double REGEN_PCT_PER_LEVEL = REGEN_MAX_PCT / SkillId.REGENERATION.maxLevel();
    public static final double SWIFTNESS_PCT_PER_LEVEL = SWIFTNESS_MAX_PCT / SkillId.SWIFTNESS.maxLevel();
    public static final double DEFENSE_POINTS_PER_LEVEL = DEFENSE_MAX_POINTS / SkillId.DEFENSE.maxLevel();
    public static final double SATURATION_PCT_PER_LEVEL = SATURATION_MAX_PCT / SkillId.SATURATION.maxLevel();

    public static final double EFFICIENCY_PCT_PER_LEVEL = EFFICIENCY_MAX_PCT / SkillId.EFFICIENCY.maxLevel();
    public static final double CHOPPING_PCT_PER_LEVEL = CHOPPING_MAX_PCT / SkillId.CHOPPING.maxLevel();
    public static final double FORAGING_PCT_PER_LEVEL = FORAGING_MAX_PCT / SkillId.FORAGING.maxLevel();
    public static final double FORTUNE_PCT_PER_LEVEL = FORTUNE_MAX_PCT / SkillId.FORTUNE.maxLevel();
    public static final double LOOTING_PCT_PER_LEVEL = LOOTING_MAX_PCT / SkillId.LOOTING.maxLevel();

    public static final double LEAPING_PCT_PER_LEVEL = 2.0D;
    public static final float SATURATION_POINTS_AT_100_PCT = 1.0F;
    public static final float REGEN_HEAL_PER_SECOND_AT_100_PCT = 0.4F;
    public static final double LUCK_PER_PERCENT = 0.01D;
}
