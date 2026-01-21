package net.revilodev.codex.skills.logic;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.skills.SkillCategory;
import net.revilodev.codex.skills.SkillId;

public final class SkillLogic {
    private SkillLogic() {}

    private static final ResourceLocation MOD_MAX_HEALTH = ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skill_max_health");
    private static final ResourceLocation MOD_ARMOR = ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skill_armor");
    private static final ResourceLocation MOD_SPEED = ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skill_speed");
    private static final ResourceLocation MOD_KB_RES = ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skill_knockback_res");
    private static final ResourceLocation MOD_LUCK = ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skill_luck");

    public static boolean tryUpgrade(PlayerSkills skills, SkillId id) {
        return skills.tryUpgrade(id);
    }

    public static boolean tryDowngrade(PlayerSkills skills, SkillId id) {
        return skills.tryDowngrade(id);
    }

    public static boolean awardCombatKill(PlayerSkills skills) {
        skills.addPoints(SkillCategory.COMBAT, 1);
        return true;
    }

    public static boolean awardUtilityBlock(PlayerSkills skills) {
        skills.addPoints(SkillCategory.UTILITY, 1);
        return true;
    }

    public static boolean awardSurvivalEvaded(PlayerSkills skills, float evaded) {
        if (evaded <= 0.0F) return false;
        int pts = (int) (evaded / 2.0F);
        if (pts <= 0) return false;
        skills.addPoints(SkillCategory.SURVIVAL, pts);
        return true;
    }

    public static float applyIncomingReductions(ServerPlayer target, PlayerSkills skills, DamageSource src, float amount) {
        float out = amount;

        int fire = skills.level(SkillId.FIRE_RESISTANCE);
        if (fire > 0 && src.is(DamageTypeTags.IS_FIRE)) out *= (float) Math.max(0.0D, 1.0D - fire * 0.02D);

        int blast = skills.level(SkillId.BLAST_RESISTANCE);
        if (blast > 0 && src.is(DamageTypeTags.IS_EXPLOSION)) out *= (float) Math.max(0.0D, 1.0D - blast * 0.02D);

        int proj = skills.level(SkillId.PROJECTILE_RESISTANCE);
        if (proj > 0 && src.is(DamageTypeTags.IS_PROJECTILE)) out *= (float) Math.max(0.0D, 1.0D - proj * 0.02D);

        return out;
    }

    public static void applyAllEffects(ServerPlayer player, PlayerSkills skills) {
        int hp = skills.level(SkillId.HEALTH);
        int armor = skills.level(SkillId.DEFENSE);
        int speed = skills.level(SkillId.SWIFTNESS);
        int kb = skills.level(SkillId.KNOCKBACK_RESISTANCE);
        int foraging = skills.level(SkillId.FORAGING);
        int fortune = skills.level(SkillId.FORTUNE);

        applyModifier(player, Attributes.MAX_HEALTH, MOD_MAX_HEALTH, hp * 2.0D, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, Attributes.ARMOR, MOD_ARMOR, armor * 1.0D, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, Attributes.MOVEMENT_SPEED, MOD_SPEED, speed * 0.004D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        applyModifier(player, Attributes.KNOCKBACK_RESISTANCE, MOD_KB_RES, Math.min(1.0D, kb * 0.02D), AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, Attributes.LUCK, MOD_LUCK, (foraging * 0.25D) + (fortune * 0.50D), AttributeModifier.Operation.ADD_VALUE);

        int regen = skills.level(SkillId.REGENERATION);
        if (regen > 0) addIfStronger(player, new MobEffectInstance(MobEffects.REGENERATION, 220, ampSteps(regen, 10), true, false, false));

        int jump = skills.level(SkillId.LEAPING);
        if (jump > 0) addIfStronger(player, new MobEffectInstance(MobEffects.JUMP, 220, ampSteps(jump, 10), true, false, false));

        int eff = skills.level(SkillId.EFFICIENCY);
        if (eff > 0) addIfStronger(player, new MobEffectInstance(MobEffects.DIG_SPEED, 220, ampSteps(eff, 10), true, false, false));

        int sat = skills.level(SkillId.SATURATION);
        if (sat > 0 && player.tickCount % 40 == 0) {
            if (player.getFoodData().getFoodLevel() < 20) player.getFoodData().eat(0, Math.min(6.0F, sat * 0.1F));
        }

        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static int ampSteps(int level, int perAmp) {
        int a = (level - 1) / perAmp;
        return Math.max(0, Math.min(4, a));
    }

    private static void addIfStronger(ServerPlayer player, MobEffectInstance inst) {
        MobEffectInstance cur = player.getEffect(inst.getEffect());
        if (cur == null || cur.getAmplifier() < inst.getAmplifier() || cur.getDuration() < 40) player.addEffect(inst);
    }

    private static void applyModifier(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount != 0.0D) inst.addTransientModifier(new AttributeModifier(id, amount, op));
    }
}
