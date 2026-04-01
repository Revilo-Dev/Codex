package net.revilodev.codex.abilities.logic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.revilodev.codex.abilities.AbilitiesAttachments;
import net.revilodev.codex.abilities.AbilityConfig;
import net.revilodev.codex.abilities.AbilityId;
import net.revilodev.codex.abilities.PlayerAbilities;
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.skills.SkillsAttachments;

import java.util.Comparator;
import java.util.List;

public final class AbilityLogic {
    private AbilityLogic() {}

    public static boolean tryActivate(ServerPlayer player, AbilityId id) {
        if (player == null || id == null) return false;

        PlayerAbilities abilities = player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
        if (!AbilityConfig.enabled(id)) return false;

        int rank = abilities.rank(id);
        if (rank <= 0 || abilities.cooldownTicks(id) > 0) return false;

        PlayerSkills skills = player.getData(SkillsAttachments.PLAYER_SKILLS.get());
        boolean executed = execute(player, id, rank, skills);
        if (!executed) return false;

        abilities.setCooldown(id, AbilityScaling.cooldownTicks(id, rank, skills));
        abilities.markUsed(id);
        return true;
    }

    private static boolean execute(ServerPlayer player, AbilityId id, int rank, PlayerSkills skills) {
        return switch (id) {
            case DASH -> dash(player, rank, skills);
            case LEAP -> leap(player, rank, skills);
            case HEAL -> heal(player, rank, skills);
            case CLEANSE -> cleanse(player, rank, skills);
            case GUARD -> guard(player, rank, skills);
            case WARCRY -> warcry(player, rank, skills);
            case EXECUTION -> execution(player, rank, skills);
            case CLEAVE -> cleave(player, rank, skills);
            case OVERPOWER -> overpower(player, rank, skills);
            case SCAVENGER -> magnetism(player, rank, skills);
        };
    }

    private static boolean dash(ServerPlayer player, int rank, PlayerSkills skills) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z).normalize().scale(AbilityScaling.dashDistance(rank, skills));
        player.push(horizontal.x, 0.08D, horizontal.z);
        player.hurtMarked = true;
        play(player, SoundEvents.BREEZE_WIND_CHARGE_BURST.value());
        return true;
    }

    private static boolean leap(ServerPlayer player, int rank, PlayerSkills skills) {
        Vec3 look = player.getLookAngle();
        Vec3 launch = new Vec3(look.x, 0.0D, look.z).normalize().scale(AbilityScaling.leapForward(rank, skills));
        player.setDeltaMovement(launch.x, AbilityScaling.leapVertical(rank, skills), launch.z);
        player.hurtMarked = true;
        play(player, SoundEvents.GOAT_LONG_JUMP);
        return true;
    }

    private static boolean heal(ServerPlayer player, int rank, PlayerSkills skills) {
        player.heal(AbilityScaling.healAmount(rank, skills));
        play(player, SoundEvents.AMETHYST_BLOCK_CHIME);
        return true;
    }

    private static boolean cleanse(ServerPlayer player, int rank, PlayerSkills skills) {
        boolean removed = false;
        for (MobEffectInstance effect : List.copyOf(player.getActiveEffects())) {
            if (!effect.getEffect().value().isBeneficial()) {
                player.removeEffect(effect.getEffect());
                removed = true;
            }
        }
        player.heal(AbilityScaling.cleanseHeal(rank, skills));
        play(player, removed ? SoundEvents.GENERIC_DRINK : SoundEvents.AMETHYST_BLOCK_CHIME);
        return true;
    }

    private static boolean guard(ServerPlayer player, int rank, PlayerSkills skills) {
        int duration = AbilityScaling.guardDurationTicks(rank, skills);
        int amplifier = AbilityScaling.guardAmplifier(rank, skills);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, amplifier, false, true, true));
        if (rank >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 0, false, true, true));
        }
        play(player, SoundEvents.SHIELD_BLOCK);
        return true;
    }

    private static boolean warcry(ServerPlayer player, int rank, PlayerSkills skills) {
        int duration = AbilityScaling.warcryDurationTicks(rank, skills);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, AbilityScaling.warcryStrengthAmp(rank, skills), false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, rank >= 3 ? 1 : 0, false, true, true));
        play(player, SoundEvents.RAID_HORN.value());
        return true;
    }

    private static boolean execution(ServerPlayer player, int rank, PlayerSkills skills) {
        LivingEntity target = target(player, 4.5D);
        if (target == null) return false;
        float damage = AbilityScaling.executionDamage(rank, skills);
        if (target.getHealth() <= target.getMaxHealth() * 0.35F) {
            damage *= AbilityScaling.executionMissingHealthBonus(rank);
        }
        target.hurt(player.damageSources().playerAttack(player), damage);
        play(player, SoundEvents.PLAYER_ATTACK_CRIT);
        return true;
    }

    private static boolean cleave(ServerPlayer player, int rank, PlayerSkills skills) {
        double radius = AbilityScaling.cleaveRadius(rank);
        Vec3 facing = player.getLookAngle().normalize();
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                entity -> entity != player && entity.isAlive() && isInFront(player, entity, facing));
        if (targets.isEmpty()) return false;
        float damage = AbilityScaling.cleaveDamage(rank, skills);
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().playerAttack(player), damage);
            knockbackFrom(player, target, 0.25D + rank * 0.05D);
        }
        play(player, SoundEvents.PLAYER_ATTACK_SWEEP);
        return true;
    }

    private static boolean overpower(ServerPlayer player, int rank, PlayerSkills skills) {
        LivingEntity target = target(player, 4.0D);
        if (target == null) return false;
        target.hurt(player.damageSources().playerAttack(player), AbilityScaling.overpowerDamage(rank, skills));
        knockbackFrom(player, target, AbilityScaling.overpowerKnockback(rank, skills));
        target.setDeltaMovement(target.getDeltaMovement().add(0.0D, 0.16D + rank * 0.03D, 0.0D));
        play(player, SoundEvents.MACE_SMASH_GROUND_HEAVY);
        return true;
    }

    private static boolean magnetism(ServerPlayer player, int rank, PlayerSkills skills) {
        PlayerAbilities abilities = player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
        abilities.setActiveTicks(AbilityId.SCAVENGER, AbilityScaling.magnetismDurationTicks(rank, skills));
        play(player, SoundEvents.ITEM_PICKUP);
        return true;
    }

    public static void tickActive(ServerPlayer player, PlayerAbilities abilities, PlayerSkills skills) {
        int active = abilities.activeTicks(AbilityId.SCAVENGER);
        if (active <= 0) return;

        int next = active - 1;
        abilities.setActiveTicks(AbilityId.SCAVENGER, next);
        if (next <= 0) return;

        double radius = AbilityScaling.magnetismRadius(Math.max(1, abilities.rank(AbilityId.SCAVENGER)), skills);
        List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(radius), Entity::isAlive);
        for (ItemEntity item : items) {
            Vec3 toward = player.position().add(0.0D, 0.35D, 0.0D).subtract(item.position());
            if (toward.lengthSqr() > 1.0E-4D) {
                item.setDeltaMovement(toward.normalize().scale(0.45D));
                item.hasImpulse = true;
            }
        }
    }

    private static LivingEntity target(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        AABB box = player.getBoundingBox().expandTowards(player.getLookAngle().scale(range)).inflate(1.2D);
        return player.level().getEntitiesOfClass(LivingEntity.class, box, entity -> entity != player && entity.isAlive())
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.position().distanceToSqr(end)))
                .orElse(null);
    }

    private static boolean isInFront(Player player, LivingEntity target, Vec3 facing) {
        Vec3 toTarget = target.position().subtract(player.position()).normalize();
        return facing.dot(toTarget) > 0.15D;
    }

    private static void knockbackFrom(Player player, LivingEntity target, double strength) {
        Vec3 dir = target.position().subtract(player.position());
        Vec3 horizontal = new Vec3(dir.x, 0.0D, dir.z);
        if (horizontal.lengthSqr() < 1.0E-4D) horizontal = player.getLookAngle();
        horizontal = horizontal.normalize().scale(strength);
        target.push(horizontal.x, 0.1D, horizontal.z);
        target.hurtMarked = true;
    }

    private static void play(ServerPlayer player, SoundEvent sound) {
        if (!(player.level() instanceof ServerLevel level)) return;
        level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.8F, 1.0F);
    }
}
