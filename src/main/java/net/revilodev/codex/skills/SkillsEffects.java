package net.revilodev.codex.skills;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.revilodev.codex.CodexMod;

public final class SkillsEffects {
    private SkillsEffects() {}

    private static final ResourceLocation MOD_MAX_HEALTH = ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skill_max_health");
    private static final ResourceLocation MOD_ARMOR = ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skill_armor");
    private static final ResourceLocation MOD_SPEED = ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skill_speed");
    private static final ResourceLocation MOD_KNOCKBACK_RES = ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skill_knockback_res");

    public static void applyServerTickEffects(ServerPlayer player, PlayerSkills skills) {
        applyModifier(player, Attributes.MAX_HEALTH, MOD_MAX_HEALTH, skills.level(SkillId.HEALTH) * 1.0D, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, Attributes.ARMOR, MOD_ARMOR, skills.level(SkillId.DEFENSE) * 0.5D, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, Attributes.MOVEMENT_SPEED, MOD_SPEED, skills.level(SkillId.SWIFTNESS) * 0.004D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        applyModifier(player, Attributes.KNOCKBACK_RESISTANCE, MOD_KNOCKBACK_RES, Math.min(1.0D, skills.level(SkillId.KNOCKBACK_RESISTANCE) * 0.02D), AttributeModifier.Operation.ADD_VALUE);

        int regen = skills.level(SkillId.REGENERATION);
        if (regen > 0) addIfStronger(player, new MobEffectInstance(MobEffects.REGENERATION, 220, ampSteps(regen, 10), true, false, false));

        int jump = skills.level(SkillId.LEAPING);
        if (jump > 0) addIfStronger(player, new MobEffectInstance(MobEffects.JUMP, 220, ampSteps(jump, 10), true, false, false));

        int eff = skills.level(SkillId.EFFICIENCY);
        if (eff > 0) addIfStronger(player, new MobEffectInstance(MobEffects.DIG_SPEED, 220, ampSteps(eff, 10), true, false, false));

        int fort = skills.level(SkillId.FORTUNE);
        if (fort > 0) addIfStronger(player, new MobEffectInstance(MobEffects.LUCK, 220, ampSteps(fort, 15), true, false, false));

        int sat = skills.level(SkillId.SATURATION);
        if (sat > 0 && player.tickCount % 40 == 0) {
            if (player.getFoodData().getFoodLevel() < 20) {
                player.getFoodData().eat(0, Math.min(6.0F, sat * 0.1F));
            }
        }

        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static int ampSteps(int level, int perAmp) {
        int a = (level - 1) / perAmp;
        return Math.max(0, Math.min(4, a));
    }

    private static void addIfStronger(ServerPlayer player, MobEffectInstance inst) {
        MobEffectInstance cur = player.getEffect(inst.getEffect());
        if (cur == null || cur.getAmplifier() < inst.getAmplifier() || cur.getDuration() < 40) {
            player.addEffect(inst);
        }
    }

    private static void applyModifier(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount != 0.0D) inst.addTransientModifier(new AttributeModifier(id, amount, op));
    }
}
