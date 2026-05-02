package net.revilodev.codex.abilities.logic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.revilodev.codex.abilities.AbilitiesAttachments;
import net.revilodev.codex.abilities.AbilityConfig;
import net.revilodev.codex.abilities.AbilityElement;
import net.revilodev.codex.abilities.AbilityId;
import net.revilodev.codex.abilities.AbilitySpecialization;
import net.revilodev.codex.abilities.PlayerAbilities;
import net.revilodev.codex.abilities.event.AbilityUseEvent;
import net.revilodev.codex.attributes.CodexAttributes;
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.skills.SkillsAttachments;
import net.revilodev.codex.stats.CodexStats;

import java.util.Comparator;
import java.util.List;

public final class AbilityLogic {
    private AbilityLogic() {}

    public static boolean tryActivate(ServerPlayer player, AbilityId id) {
        if (player == null || id == null || !id.isSpecialization()) return false;
        PlayerAbilities abilities = player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
        if (!AbilityConfig.enabled(id) || abilities.rank(id) <= 0 || abilities.cooldownTicks(id) > 0) return false;

        PlayerSkills skills = player.getData(SkillsAttachments.PLAYER_SKILLS.get());
        double abilityPower = CodexAttributes.abilityPower(player, id);
        AbilityUseEvent.Pre preEvent = new AbilityUseEvent.Pre(player, id, abilities.rank(id), skills, abilityPower);
        if (NeoForge.EVENT_BUS.post(preEvent).isCanceled()) return false;

        int coreRank = Math.max(1, abilities.rank(id.core()));
        if (!execute(player, id, coreRank, preEvent.getAbilityPower())) return false;
        abilities.setCooldown(id, AbilityScaling.cooldownTicks(id, coreRank, skills));
        abilities.markUsed(id);
        player.awardStat(CodexStats.ABILITIES_USED);
        player.awardStat(CodexStats.abilityUse(id));
        NeoForge.EVENT_BUS.post(new AbilityUseEvent.Post(player, id, abilities.rank(id), skills, preEvent.getAbilityPower()));
        return true;
    }

    private static boolean execute(ServerPlayer player, AbilityId id, int coreRank, double abilityPower) {
        if (id == AbilityId.MAGIC_HEAL) return magicHeal(player, id, coreRank, abilityPower);
        if (id == AbilityId.MAGIC_CLEANSE) return magicCleanse(player, id, coreRank, abilityPower);
        if (id == AbilityId.WIND_DASH) return windDash(player, id, coreRank, abilityPower);
        if (id == AbilityId.WIND_LEAP) return windLeap(player, id, coreRank, abilityPower);
        if (id == AbilityId.WIND_LUNGE) return windLunge(player, id, coreRank, abilityPower);
        return switch (id.specialization()) {
            case BURST -> burst(player, id, coreRank, abilityPower);
            case NOVA -> nova(player, id, coreRank, abilityPower);
            case IMPLODE -> implode(player, id, coreRank, abilityPower);
            case STORM -> storm(player, id, coreRank, abilityPower);
            case PIERCE -> pierce(player, id, coreRank, abilityPower);
            case GLACIER -> glacier(player, id, coreRank, abilityPower);
            case STRIKE -> strike(player, id, coreRank, abilityPower);
            case ZAP -> zap(player, id, coreRank, abilityPower);
            case AEGIS -> aegis(player, id, coreRank, abilityPower);
            case RAMPAGE -> rampage(player, id, coreRank, abilityPower);
            case BASH -> bash(player, id, coreRank, abilityPower);
            default -> false;
        };
    }

    private static boolean magicHeal(ServerPlayer player, AbilityId id, int coreRank, double abilityPower) {
        player.heal(Math.max(1.0F, AbilityScaling.damage(id, coreRank, abilityPower) * 0.6F));
        fx(player, ParticleTypes.HEART, SoundEvents.AMETHYST_BLOCK_CHIME);
        return true;
    }

    private static boolean magicCleanse(ServerPlayer player, AbilityId id, int coreRank, double abilityPower) {
        for (MobEffectInstance effect : List.copyOf(player.getActiveEffects())) {
            if (!effect.getEffect().value().isBeneficial()) {
                player.removeEffect(effect.getEffect());
            }
        }
        if (player.isOnFire()) player.clearFire();
        player.heal(Math.max(1.0F, AbilityScaling.damage(id, coreRank, abilityPower) * 0.35F));
        fx(player, ParticleTypes.WAX_OFF, SoundEvents.GENERIC_DRINK);
        return true;
    }

    private static boolean windDash(ServerPlayer player, AbilityId id, int coreRank, double abilityPower) {
        Vec3 look = player.getLookAngle();
        Vec3 horiz = new Vec3(look.x, 0.0D, look.z).normalize().scale(0.8D + coreRank * 0.16D);
        player.push(horiz.x, 0.06D, horiz.z);
        player.hurtMarked = true;
        fx(player, ParticleTypes.CLOUD, SoundEvents.BREEZE_WIND_CHARGE_BURST.value());
        return true;
    }

    private static boolean windLeap(ServerPlayer player, AbilityId id, int coreRank, double abilityPower) {
        Vec3 look = player.getLookAngle();
        Vec3 horiz = new Vec3(look.x, 0.0D, look.z).normalize().scale(0.6D + coreRank * 0.12D);
        player.setDeltaMovement(horiz.x, 0.45D + coreRank * 0.04D, horiz.z);
        player.hurtMarked = true;
        fx(player, ParticleTypes.POOF, SoundEvents.GOAT_LONG_JUMP);
        return true;
    }

    private static boolean windLunge(ServerPlayer player, AbilityId id, int coreRank, double abilityPower) {
        LivingEntity target = nearby(player, AbilityScaling.radius(id, coreRank, abilityPower) + 4.0D).stream().filter(e -> inFront(player, e)).findFirst().orElse(null);
        if (target == null) return false;
        Vec3 toward = target.position().subtract(player.position()).normalize();
        player.setDeltaMovement(toward.x * 0.9D, 0.12D, toward.z * 0.9D);
        player.hurtMarked = true;
        target.hurt(player.damageSources().playerAttack(player), AbilityScaling.damage(id, coreRank, abilityPower));
        fx(player, ParticleTypes.SWEEP_ATTACK, SoundEvents.PLAYER_ATTACK_KNOCKBACK);
        return true;
    }

    private static boolean burst(ServerPlayer player, AbilityId id, int coreRank, double abilityPower) {
        List<LivingEntity> targets = nearby(player, AbilityScaling.radius(id, coreRank, abilityPower) + 4.0D).stream().filter(e -> inFront(player, e)).limit(5).toList();
        if (targets.isEmpty()) return false;
        Vec3 start = player.getEyePosition();
        for (LivingEntity target : targets) {
            applyElementHit(player, id.element(), target, AbilityScaling.damage(id, coreRank, abilityPower), AbilityScaling.durationTicks(id, coreRank, abilityPower));
            if (player.level() instanceof ServerLevel level) {
                Vec3 end = target.getBoundingBox().getCenter();
                for (int i = 0; i <= 8; i++) {
                    Vec3 p = start.lerp(end, i / 8.0D);
                    level.sendParticles(id.element() == AbilityElement.ICE ? ParticleTypes.SNOWFLAKE : (id.element() == AbilityElement.POISON ? ParticleTypes.WITCH : ParticleTypes.FLAME), p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
            }
        }
        fx(player, ParticleTypes.SMOKE, SoundEvents.BLAZE_SHOOT);
        return true;
    }

    private static boolean nova(ServerPlayer player, AbilityId id, int coreRank, double abilityPower) {
        List<LivingEntity> targets = nearby(player, AbilityScaling.radius(id, coreRank, abilityPower) + 1.5D);
        if (targets.isEmpty()) return false;
        for (LivingEntity target : targets) applyElementHit(player, id.element(), target, AbilityScaling.damage(id, coreRank, abilityPower) * 1.15F, AbilityScaling.durationTicks(id, coreRank, abilityPower));
        fx(player, id.element() == AbilityElement.ICE ? ParticleTypes.SNOWFLAKE : (id.element() == AbilityElement.POISON ? ParticleTypes.WITCH : ParticleTypes.END_ROD), SoundEvents.BEACON_ACTIVATE);
        return true;
    }

    private static boolean implode(ServerPlayer player, AbilityId id, int coreRank, double abilityPower) {
        List<LivingEntity> targets = nearby(player, AbilityScaling.radius(id, coreRank, abilityPower) + 1.0D);
        if (targets.isEmpty()) return false;
        for (LivingEntity target : targets) {
            pullToward(player, target, 0.5D + coreRank * 0.05D);
            applyElementHit(player, id.element(), target, AbilityScaling.damage(id, coreRank, abilityPower), AbilityScaling.durationTicks(id, coreRank, abilityPower));
        }
        fx(player, ParticleTypes.EXPLOSION, SoundEvents.GENERIC_EXPLODE.value());
        return true;
    }

    private static boolean storm(ServerPlayer player, AbilityId id, int coreRank, double abilityPower) {
        List<LivingEntity> targets = nearby(player, AbilityScaling.radius(id, coreRank, abilityPower) + 2.5D);
        if (targets.isEmpty()) return false;
        for (LivingEntity target : targets) {
            applyElementHit(player, id.element(), target, AbilityScaling.damage(id, coreRank, abilityPower) * 1.1F, AbilityScaling.durationTicks(id, coreRank, abilityPower));
            if (id.element() == AbilityElement.LIGHTNING && player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0D, target.getZ(), 14, 0.2D, 0.3D, 0.2D, 0.01D);
            } else if (id.element() == AbilityElement.FIRE && player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 1.0D, target.getZ(), 10, 0.25D, 0.3D, 0.25D, 0.01D);
            } else if (id.element() == AbilityElement.ICE && player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 0.5D, target.getZ(), 12, 0.2D, 0.4D, 0.2D, 0.01D);
            }
        }
        fx(player, id.element() == AbilityElement.ICE ? ParticleTypes.SNOWFLAKE : ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, SoundEvents.LIGHTNING_BOLT_THUNDER);
        return true;
    }

    private static boolean pierce(ServerPlayer player, AbilityId id, int coreRank, double abilityPower) {
        List<LivingEntity> targets = nearby(player, AbilityScaling.radius(id, coreRank, abilityPower) + 4.0D).stream().filter(e -> inFront(player, e)).toList();
        if (targets.isEmpty()) return false;
        for (LivingEntity target : targets) applyElementHit(player, id.element(), target, AbilityScaling.damage(id, coreRank, abilityPower), AbilityScaling.durationTicks(id, coreRank, abilityPower));
        fx(player, ParticleTypes.SWEEP_ATTACK, SoundEvents.PLAYER_ATTACK_SWEEP);
        return true;
    }

    private static boolean glacier(ServerPlayer player, AbilityId id, int coreRank, double abilityPower) {
        List<LivingEntity> targets = nearby(player, AbilityScaling.radius(id, coreRank, abilityPower) + 2.0D);
        if (targets.isEmpty()) return false;
        int duration = AbilityScaling.durationTicks(id, coreRank, abilityPower);
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().magic(), AbilityScaling.damage(id, coreRank, abilityPower));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 3, false, true, true));
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL, target.getX(), target.getY(), target.getZ(), 6, 0.15D, 0.7D, 0.15D, 0.01D);
            }
        }
        fx(player, ParticleTypes.SNOWFLAKE, SoundEvents.GLASS_BREAK);
        return true;
    }

    private static boolean strike(ServerPlayer player, AbilityId id, int coreRank, double abilityPower) {
        LivingEntity target = nearby(player, AbilityScaling.radius(id, coreRank, abilityPower) + 2.0D).stream().filter(e -> inFront(player, e)).findFirst().orElse(null);
        if (target == null) return false;
        applyElementHit(player, id.element(), target, AbilityScaling.damage(id, coreRank, abilityPower) * 1.2F, AbilityScaling.durationTicks(id, coreRank, abilityPower));
        if (id.element() == AbilityElement.LIGHTNING && player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0D, target.getZ(), 18, 0.2D, 0.5D, 0.2D, 0.02D);
        }
        fx(player, ParticleTypes.CRIT, SoundEvents.PLAYER_ATTACK_CRIT);
        return true;
    }

    private static boolean zap(ServerPlayer player, AbilityId id, int coreRank, double abilityPower) {
        List<LivingEntity> targets = nearby(player, AbilityScaling.radius(id, coreRank, abilityPower) + 3.0D).stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(player))).limit(4).toList();
        if (targets.isEmpty()) return false;
        for (LivingEntity target : targets) applyElementHit(player, AbilityElement.LIGHTNING, target, AbilityScaling.damage(id, coreRank, abilityPower) * 0.7F, AbilityScaling.durationTicks(id, coreRank, abilityPower));
        fx(player, ParticleTypes.ELECTRIC_SPARK, SoundEvents.LIGHTNING_BOLT_IMPACT);
        return true;
    }

    private static boolean aegis(ServerPlayer player, AbilityId id, int coreRank, double abilityPower) {
        int duration = AbilityScaling.durationTicks(id, coreRank, abilityPower);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, Math.min(3, Math.max(0, coreRank / 2)), false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, Math.max(1, coreRank / 2), false, true, true));
        for (LivingEntity target : nearby(player, AbilityScaling.radius(id, coreRank, abilityPower))) {
            pullToward(target, player, 0.8D + coreRank * 0.05D);
        }
        fx(player, ParticleTypes.TOTEM_OF_UNDYING, SoundEvents.SHIELD_BLOCK);
        return true;
    }

    private static boolean rampage(ServerPlayer player, AbilityId id, int coreRank, double abilityPower) {
        int duration = AbilityScaling.durationTicks(id, coreRank, abilityPower);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, Math.min(4, coreRank / 2), false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, Math.min(2, coreRank / 3), false, true, true));
        fx(player, ParticleTypes.ANGRY_VILLAGER, SoundEvents.RAID_HORN.value());
        return true;
    }

    private static boolean bash(ServerPlayer player, AbilityId id, int coreRank, double abilityPower) {
        LivingEntity target = nearby(player, AbilityScaling.radius(id, coreRank, abilityPower) + 1.5D).stream().filter(e -> inFront(player, e)).findFirst().orElse(null);
        if (target == null) return false;
        target.hurt(player.damageSources().playerAttack(player), AbilityScaling.damage(id, coreRank, abilityPower) * 1.4F);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30 + coreRank * 8, 4, false, true, true));
        pullToward(target, player, 0.8D + coreRank * 0.06D);
        fx(player, ParticleTypes.SWEEP_ATTACK, SoundEvents.MACE_SMASH_GROUND_HEAVY);
        return true;
    }

    private static List<LivingEntity> nearby(ServerPlayer player, double radius) {
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius), e -> e != player && e.isAlive());
    }

    private static boolean inFront(ServerPlayer player, LivingEntity target) {
        Vec3 facing = player.getLookAngle().normalize();
        Vec3 to = target.position().subtract(player.position()).normalize();
        return facing.dot(to) > 0.1D;
    }

    private static void applyElementHit(ServerPlayer player, AbilityElement element, LivingEntity target, float damage, int duration) {
        target.hurt(player.damageSources().magic(), damage);
        switch (element) {
            case FIRE -> target.igniteForSeconds(Math.max(1, duration / 20));
            case ICE -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 2, false, true, true));
            case LIGHTNING -> {}
            case POISON -> target.addEffect(new MobEffectInstance(MobEffects.POISON, duration, 1, false, true, true));
            case FORCE -> pullToward(target, player, 0.6D);
            case MAGIC -> {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Math.max(20, duration / 2), 0, false, true, true));
            }
            case WIND -> {
                pullToward(target, player, 0.5D);
            }
        }
        if (element == AbilityElement.LIGHTNING && target instanceof Enemy) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, Math.max(20, duration / 3), 0, false, false, false));
        }
    }

    private static void pullToward(LivingEntity anchor, LivingEntity target, double strength) {
        Vec3 dir = anchor.position().subtract(target.position());
        Vec3 horiz = new Vec3(dir.x, 0.0D, dir.z);
        if (horiz.lengthSqr() < 1.0E-5D) return;
        Vec3 impulse = horiz.normalize().scale(strength);
        target.push(impulse.x, 0.08D, impulse.z);
        target.hurtMarked = true;
    }

    private static void fx(ServerPlayer player, net.minecraft.core.particles.ParticleOptions particle, net.minecraft.sounds.SoundEvent sound) {
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(particle, player.getX(), player.getY() + 1.0D, player.getZ(), 16, 0.6D, 0.3D, 0.6D, 0.01D);
            level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.8F, 1.0F);
        }
    }

    public static void tickActive(ServerPlayer player, PlayerAbilities abilities, PlayerSkills skills) {
    }
}
