package net.revilodev.codex.skills.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.skills.SkillBalance;
import net.revilodev.codex.skills.SkillCategory;
import net.revilodev.codex.skills.SkillId;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SkillLogic {
    private SkillLogic() {}

    private static final ResourceLocation MOD_MAX_HEALTH = ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skill_max_health");
    private static final ResourceLocation MOD_ARMOR = ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skill_armor");
    private static final ResourceLocation MOD_SPEED = ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skill_speed");
    private static final ResourceLocation MOD_KB_RES = ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skill_knockback_res");
    private static final ResourceLocation MOD_LUCK = ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skill_luck");

    private static final Map<UUID, Streak> COMBAT_STREAK = new HashMap<>();
    private static final Map<UUID, Streak> UTILITY_STREAK = new HashMap<>();

    private static final int COMBAT_WINDOW_TICKS = 200;
    private static final int UTILITY_WINDOW_TICKS = 160;

    public static boolean tryUpgrade(PlayerSkills skills, SkillId id) {
        return skills.tryUpgrade(id);
    }

    public static boolean tryDowngrade(PlayerSkills skills, SkillId id) {
        return skills.tryDowngrade(id);
    }

    public static boolean awardCombatKill(ServerPlayer killer, PlayerSkills skills, LivingEntity victim) {
        if (killer == null || victim == null) return false;
        int xp = combatKillXp(victim);
        float mult = streakMultiplier(COMBAT_STREAK, killer.getUUID(), killer.tickCount, COMBAT_WINDOW_TICKS, 0.06F, 1.6F);
        int out = clampInt(Math.round(xp * mult), 1, 120);
        return skills.addProgress(SkillCategory.COMBAT, out);
    }

    public static boolean awardUtilityBlock(ServerPlayer player, PlayerSkills skills, BlockState state, LevelAccessor level, BlockPos pos) {
        if (player == null || state == null) return false;
        int xp = utilityBlockXp(state, level, pos);
        if (xp <= 0) return false;
        float mult = streakMultiplier(UTILITY_STREAK, player.getUUID(), player.tickCount, UTILITY_WINDOW_TICKS, 0.03F, 1.35F);
        int out = clampInt(Math.round(xp * mult), 1, 60);
        return skills.addProgress(SkillCategory.UTILITY, out);
    }

    public static boolean awardSurvivalPrevented(PlayerSkills skills, float preventedBySkills) {
        int xp = survivalPreventedXp(preventedBySkills);
        if (xp <= 0) return false;
        return skills.addProgress(SkillCategory.SURVIVAL, xp);
    }

    public static int combatKillXp(LivingEntity victim) {
        int hp = (int) Math.ceil(Math.max(1.0F, victim.getMaxHealth()));
        int armor = Math.max(0, victim.getArmorValue());

        float catMult = 1.0F;
        MobCategory cat = victim.getType().getCategory();
        if (cat == MobCategory.MONSTER) catMult = 1.0F;
        else if (cat == MobCategory.CREATURE) catMult = 0.35F;
        else if (cat == MobCategory.WATER_CREATURE) catMult = 0.45F;
        else if (cat == MobCategory.AMBIENT) catMult = 0.15F;
        else catMult = 0.20F;

        int base = 6 + (hp / 4) + (armor / 2);
        int xp = Math.round(base * catMult);
        return clampInt(xp, 1, 80);
    }

    public static int utilityBlockXp(BlockState state, LevelAccessor level, BlockPos pos) {
        float h = state.getDestroySpeed(level, pos);
        if (h < 0.0F) h = 0.0F;
        if (h <= 0.0F) return 0;
        int xp = 1 + (int) Math.floor(h * 1.8F);
        return clampInt(xp, 1, 28);
    }

    public static int survivalPreventedXp(float preventedBySkills) {
        if (preventedBySkills <= 0.0F) return 0;
        int xp = (int) Math.floor(preventedBySkills * 2.4F);
        return clampInt(xp, 1, 90);
    }

    public static float applyIncomingReductions(ServerPlayer target, PlayerSkills skills, DamageSource src, float amount) {
        float out = amount;

        int fire = skills.level(SkillId.FIRE_RESISTANCE);
        if (fire > 0 && src.is(DamageTypeTags.IS_FIRE)) {
            out *= (float) Math.max(0.0D, 1.0D - (fire * SkillBalance.FIRE_RES_PCT_PER_LEVEL) / 100.0D);
        }

        int blast = skills.level(SkillId.BLAST_RESISTANCE);
        if (blast > 0 && src.is(DamageTypeTags.IS_EXPLOSION)) {
            out *= (float) Math.max(0.0D, 1.0D - (blast * SkillBalance.BLAST_RES_PCT_PER_LEVEL) / 100.0D);
        }

        int proj = skills.level(SkillId.PROJECTILE_RESISTANCE);
        if (proj > 0 && src.is(DamageTypeTags.IS_PROJECTILE)) {
            out *= (float) Math.max(0.0D, 1.0D - (proj * SkillBalance.PROJECTILE_RES_PCT_PER_LEVEL) / 100.0D);
        }

        return out;
    }

    public static void applyAllEffects(ServerPlayer player, PlayerSkills skills) {
        int hp = skills.level(SkillId.HEALTH);
        int armor = skills.level(SkillId.DEFENSE);
        int speed = skills.level(SkillId.SWIFTNESS);
        int kb = skills.level(SkillId.KNOCKBACK_RESISTANCE);
        int foraging = skills.level(SkillId.FORAGING);
        int fishing = skills.level(SkillId.FISHING);
        int fortune = skills.level(SkillId.FORTUNE);
        int looting = skills.level(SkillId.LOOTING);

        applyModifier(player, Attributes.MAX_HEALTH, MOD_MAX_HEALTH, hp * SkillBalance.HEALTH_POINTS_PER_LEVEL, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, Attributes.ARMOR, MOD_ARMOR, armor * SkillBalance.DEFENSE_POINTS_PER_LEVEL, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(
                player,
                Attributes.MOVEMENT_SPEED,
                MOD_SPEED,
                (speed * SkillBalance.SWIFTNESS_PCT_PER_LEVEL) / 100.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        applyModifier(
                player,
                Attributes.KNOCKBACK_RESISTANCE,
                MOD_KB_RES,
                Math.min(1.0D, (kb * SkillBalance.KNOCKBACK_RES_PCT_PER_LEVEL) / 100.0D),
                AttributeModifier.Operation.ADD_VALUE
        );
        double luck =
                ((foraging * SkillBalance.FORAGING_PCT_PER_LEVEL)
                        + (fishing * SkillBalance.FISHING_PCT_PER_LEVEL)
                        + (fortune * SkillBalance.FORTUNE_PCT_PER_LEVEL)
                        + (looting * SkillBalance.LOOTING_PCT_PER_LEVEL))
                        * SkillBalance.LUCK_PER_PERCENT;
        applyModifier(player, Attributes.LUCK, MOD_LUCK, luck, AttributeModifier.Operation.ADD_VALUE);

        int regen = skills.level(SkillId.REGENERATION);
        if (regen > 0 && player.getHealth() < player.getMaxHealth()) {
            float pct = (float) ((regen * SkillBalance.REGEN_PCT_PER_LEVEL) / 100.0D);
            float heal = SkillBalance.REGEN_HEAL_PER_SECOND_AT_100_PCT * pct;
            if (heal > 0.0F) player.heal(heal);
        }

        int jump = skills.level(SkillId.LEAPING);
        if (jump > 0) addIfStronger(player, new MobEffectInstance(net.minecraft.world.effect.MobEffects.JUMP, 220, ampSteps(jump, 10), true, false, false));

        int sat = skills.level(SkillId.SATURATION);
        if (sat > 0 && player.tickCount % 40 == 0) {
            if (player.getFoodData().getFoodLevel() < 20) {
                float pct = (float) ((sat * SkillBalance.SATURATION_PCT_PER_LEVEL) / 100.0D);
                float amt = SkillBalance.SATURATION_POINTS_AT_100_PCT * pct;
                player.getFoodData().eat(0, Math.min(6.0F, amt));
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
        if (cur == null || cur.getAmplifier() < inst.getAmplifier() || cur.getDuration() < 40) player.addEffect(inst);
    }

    private static void applyModifier(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount != 0.0D) inst.addTransientModifier(new AttributeModifier(id, amount, op));
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
