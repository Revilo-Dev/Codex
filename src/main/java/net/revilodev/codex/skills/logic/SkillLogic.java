package net.revilodev.codex.skills.logic;

import com.revilo.levelup.api.LevelUpApi;
import com.revilo.levelup.api.LevelUpSources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.skills.SkillBalance;
import net.revilodev.codex.skills.SkillId;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SkillLogic {
    private SkillLogic() {}

    private static final ResourceLocation MOD_MAX_HEALTH = ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skill_health_boost");
    private static final ResourceLocation MOD_MOVEMENT_SPEED = ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skill_agility");
    private static final ResourceLocation MOD_KB_RES = ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skill_knockback_res");
    private static final ResourceLocation MOD_LUCK = ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skill_luck");
    private static final ResourceLocation SOURCE_SURVIVAL_PREVENTED = ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "survival_prevented");
    private static final Map<UUID, Streak> COMBAT_STREAK = new HashMap<>();
    private static final int COMBAT_WINDOW_TICKS = 200;

    public static boolean tryUpgrade(ServerPlayer player, PlayerSkills skills, SkillId id) {
        if (player == null || id == null) return false;
        int cur = skills.level(id);
        if (cur >= id.maxLevel()) return false;
        if (!skills.canUnlock(id)) return false;
        int requiredLevel = requiredLevelForNextRank(id, cur);
        if (!LevelUpApi.meetsLevelRequirement(player, requiredLevel)) return false;
        return skills.tryUpgrade(id);
    }

    public static boolean tryDowngrade(PlayerSkills skills, SkillId id) {
        return skills.tryDowngrade(id);
    }

    public static void clearStreaks(UUID id) {
        if (id != null) COMBAT_STREAK.remove(id);
    }

    public static boolean awardCombatKill(ServerPlayer killer, LivingEntity victim) {
        if (killer == null || victim == null) return false;
        int xp = combatKillXp(victim);
        float mult = streakMultiplier(COMBAT_STREAK, killer.getUUID(), killer.tickCount, COMBAT_WINDOW_TICKS, 0.06F, 1.6F);
        int out = clampInt(Math.round(xp * mult), 1, 120);
        LevelUpApi.awardXp(killer, out, LevelUpSources.MOB_KILL);
        return true;
    }

    public static boolean awardSurvivalPrevented(ServerPlayer player, float preventedBySkills) {
        if (player == null) return false;
        int xp = survivalPreventedXp(preventedBySkills);
        if (xp <= 0) return false;
        LevelUpApi.awardXp(player, xp, SOURCE_SURVIVAL_PREVENTED);
        return true;
    }

    public static int requiredLevelForNextRank(SkillId id, int currentSkillLevel) {
        return Math.max(1, currentSkillLevel + 1);
    }

    public static int combatKillXp(LivingEntity victim) {
        int hp = (int) Math.ceil(Math.max(1.0F, victim.getMaxHealth()));
        int armor = Math.max(0, victim.getArmorValue());
        float catMult;
        MobCategory cat = victim.getType().getCategory();
        if (cat == MobCategory.MONSTER) catMult = 1.0F;
        else if (cat == MobCategory.CREATURE) catMult = 0.35F;
        else if (cat == MobCategory.WATER_CREATURE) catMult = 0.45F;
        else if (cat == MobCategory.AMBIENT) catMult = 0.15F;
        else catMult = 0.20F;
        int base = 6 + (hp / 4) + (armor / 2);
        return clampInt(Math.round(base * catMult), 1, 80);
    }

    public static int survivalPreventedXp(float preventedBySkills) {
        if (preventedBySkills <= 0.0F) return 0;
        return clampInt((int) Math.floor(preventedBySkills * 2.4F), 1, 90);
    }

    public static float applyIncomingReductions(ServerPlayer target, PlayerSkills skills, DamageSource src, float amount) {
        float out = amount;
        int resistance = skills.level(SkillId.RESISTANCE);
        if (resistance > 0) {
            out *= (float) (1.0D - SkillBalance.resistance(resistance));
        }

        int fire = skills.level(SkillId.FIRE_RESISTANCE);
        if (fire > 0 && src.is(DamageTypeTags.IS_FIRE)) {
            out *= (float) (1.0D - SkillBalance.fireResistance(fire));
        }

        int proj = skills.level(SkillId.PROJECTILE_RESISTANCE);
        if (proj > 0 && src.is(DamageTypeTags.IS_PROJECTILE)) {
            out *= (float) (1.0D - SkillBalance.projectileResistance(proj));
        }
        return out;
    }

    public static void applyAllEffects(ServerPlayer player, PlayerSkills skills) {
        if (skills.consumeModifiersDirty()) {
            applyAttributeModifiers(player, skills);
        }
        applyTickEffects(player, skills);
    }

    private static void applyAttributeModifiers(ServerPlayer player, PlayerSkills skills) {
        int vitality = skills.level(SkillId.VITALITY);
        int agility = skills.level(SkillId.AGILITY);
        int kb = skills.level(SkillId.KNOCKBACK_RESISTANCE);
        int luck = skills.level(SkillId.LUCK);

        applyModifier(player, Attributes.MAX_HEALTH, MOD_MAX_HEALTH, SkillBalance.vitalityHearts(vitality) * 2.0D, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, Attributes.MOVEMENT_SPEED, MOD_MOVEMENT_SPEED, SkillBalance.agilitySpeed(agility), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        applyModifier(player, Attributes.KNOCKBACK_RESISTANCE, MOD_KB_RES, SkillBalance.knockbackResistance(kb), AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, Attributes.LUCK, MOD_LUCK, SkillBalance.luck(luck), AttributeModifier.Operation.ADD_VALUE);
        clampToMaxHealth(player);
    }

    private static void applyTickEffects(ServerPlayer player, PlayerSkills skills) {
        int regen = skills.level(SkillId.REGENERATION);
        if (regen > 0 && player.getHealth() < player.getMaxHealth()) {
            float heal = SkillBalance.regenHeartsPerSecond(regen);
            if (heal > 0.0F) player.heal(heal);
        }

        int jump = skills.level(SkillId.LEAPING);
        if (jump > 0) {
            int amp = Math.max(0, Math.min(4, (int) Math.floor(SkillBalance.leapingBonus(jump))));
            addIfStronger(player, new MobEffectInstance(MobEffects.JUMP, 220, amp, true, false, false));
        }

        int cleanse = skills.level(SkillId.CLEANSE);
        if (cleanse > 0 && player.tickCount % 20 == 0) {
            int removals = SkillBalance.cleanseImmunities(cleanse);
            if (removals > 0) {
                player.getActiveEffects().stream()
                        .filter(inst -> !inst.getEffect().value().isBeneficial())
                        .limit(removals)
                        .toList()
                        .forEach(inst -> player.removeEffect(inst.getEffect()));
            }
        }

        clampToMaxHealth(player);
    }

    private static void clampToMaxHealth(ServerPlayer player) {
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void addIfStronger(ServerPlayer player, MobEffectInstance inst) {
        MobEffectInstance cur = player.getEffect(inst.getEffect());
        if (cur == null || cur.getAmplifier() < inst.getAmplifier() || cur.getDuration() < 40) player.addEffect(inst);
    }

    private static void applyModifier(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        AttributeModifier existing = inst.getModifier(id);
        if (amount == 0.0D) {
            if (existing != null) inst.removeModifier(id);
            return;
        }
        if (existing != null && existing.operation() == op && Double.compare(existing.amount(), amount) == 0) {
            return;
        }
        inst.removeModifier(id);
        inst.addTransientModifier(new AttributeModifier(id, amount, op));
    }

    private static float streakMultiplier(Map<UUID, Streak> map, UUID id, int tick, int window, float per, float cap) {
        Streak s = map.get(id);
        if (s == null) {
            s = new Streak();
            s.lastTick = tick;
            s.count = 1;
            map.put(id, s);
            return 1.0F;
        }
        if (tick - s.lastTick <= window) s.count++;
        else s.count = 1;
        s.lastTick = tick;
        float mult = 1.0F + (Math.max(0, s.count - 1) * per);
        if (mult > cap) mult = cap;
        return mult;
    }

    private static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static final class Streak {
        int lastTick;
        int count;
    }
}
