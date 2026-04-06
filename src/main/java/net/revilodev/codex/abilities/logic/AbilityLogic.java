package net.revilodev.codex.abilities.logic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LightningBolt;
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
            case LUNGE -> lunge(player, rank, skills);
            case HEAL -> heal(player, rank, skills);
            case CLEANSE -> cleanse(player, rank, skills);
            case WARCRY -> warcry(player, rank, skills);
            case EXECUTION -> execution(player, rank, skills);
            case CLEAVE -> cleave(player, rank, skills);
            case OVERPOWER -> overpower(player, rank, skills);
            case GUARD -> guard(player, rank, skills);
            case BLAST -> blast(player, rank, skills);
            case BLAZE -> blaze(player, rank, skills);
            case GLACIER -> glacier(player, rank, skills);
            case SMITE -> smite(player, rank, skills);
            case SCAVENGER -> magnetism(player, rank, skills);
        };
    }

    private static boolean dash(ServerPlayer player, int rank, PlayerSkills skills) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z).normalize().scale(AbilityScaling.dashDistance(rank, skills));
        player.push(horizontal.x, 0.08D, horizontal.z);
        player.hurtMarked = true;
        burst(player, ParticleTypes.CLOUD, 18, 0.38D, 0.12D, 0.38D, 0.02D);
        play(player, SoundEvents.BREEZE_WIND_CHARGE_BURST.value());
        return true;
    }

    private static boolean leap(ServerPlayer player, int rank, PlayerSkills skills) {
        Vec3 look = player.getLookAngle();
        Vec3 launch = new Vec3(look.x, 0.0D, look.z).normalize().scale(AbilityScaling.leapForward(rank, skills));
        player.setDeltaMovement(launch.x, AbilityScaling.leapVertical(rank, skills), launch.z);
        player.hurtMarked = true;
        burst(player, ParticleTypes.POOF, 16, 0.25D, 0.05D, 0.25D, 0.02D);
        play(player, SoundEvents.GOAT_LONG_JUMP);
        return true;
    }

    private static boolean lunge(ServerPlayer player, int rank, PlayerSkills skills) {
        LivingEntity target = target(player, AbilityScaling.lungeDistance(rank, skills));
        if (target == null) return false;

        Vec3 toward = target.position().subtract(player.position());
        Vec3 horizontal = new Vec3(toward.x, 0.0D, toward.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            horizontal = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
        }
        Vec3 motion = horizontal.normalize().scale(AbilityScaling.lungeDistance(rank, skills) * 0.35D);
        player.setDeltaMovement(motion.x, 0.18D, motion.z);
        player.hurtMarked = true;
        float speedBonus = AbilityScaling.lungeSpeedDamageBonus(new Vec3(motion.x, 0.0D, motion.z).length());
        target.hurt(player.damageSources().playerAttack(player), AbilityScaling.lungeDamage(rank, skills) + speedBonus);
        knockbackFrom(player, target, AbilityScaling.lungeKnockback(rank, skills));
        line(player, ParticleTypes.SWEEP_ATTACK, player.position().add(0.0D, 1.0D, 0.0D), target.position().add(0.0D, 1.0D, 0.0D), 7);
        burst(player, ParticleTypes.CLOUD, 10, 0.2D, 0.1D, 0.2D, 0.01D);
        play(player, SoundEvents.PLAYER_ATTACK_KNOCKBACK);
        return true;
    }

    private static boolean heal(ServerPlayer player, int rank, PlayerSkills skills) {
        player.heal(AbilityScaling.healAmount(rank, skills));
        ring(player, ParticleTypes.HEART, 10, 0.95D);
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
        burst(player, removed ? ParticleTypes.WAX_OFF : ParticleTypes.HAPPY_VILLAGER, 16, 0.35D, 0.35D, 0.35D, 0.01D);
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
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(3.2D + rank * 0.25D),
                entity -> entity != player && entity.isAlive()
        );
        for (LivingEntity target : targets) {
            knockbackFrom(player, target, 0.55D + rank * 0.08D);
        }
        ring(player, ParticleTypes.TOTEM_OF_UNDYING, 12, 1.05D);
        play(player, SoundEvents.SHIELD_BLOCK);
        return true;
    }

    private static boolean warcry(ServerPlayer player, int rank, PlayerSkills skills) {
        int duration = AbilityScaling.warcryDurationTicks(rank, skills);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, AbilityScaling.warcryStrengthAmp(rank, skills), false, true, true));
        burst(player, ParticleTypes.ANGRY_VILLAGER, 8, 0.45D, 0.2D, 0.45D, 0.01D);
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
        burst(player, ParticleTypes.CRIT, 12, 0.2D, 0.2D, 0.2D, 0.15D);
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
        ring(player, ParticleTypes.SWEEP_ATTACK, 9, 1.2D);
        play(player, SoundEvents.PLAYER_ATTACK_SWEEP);
        return true;
    }

    private static boolean overpower(ServerPlayer player, int rank, PlayerSkills skills) {
        LivingEntity target = target(player, 4.0D);
        if (target == null) return false;
        target.hurt(player.damageSources().playerAttack(player), AbilityScaling.overpowerDamage(rank, skills));
        knockbackFrom(player, target, AbilityScaling.overpowerKnockback(rank, skills));
        target.setDeltaMovement(target.getDeltaMovement().add(0.0D, 0.16D + rank * 0.03D, 0.0D));
        burstAt(player, target.position().add(0.0D, 0.6D, 0.0D), ParticleTypes.CRIT, 14, 0.15D, 0.2D, 0.15D, 0.18D);
        play(player, SoundEvents.MACE_SMASH_GROUND_HEAVY);
        return true;
    }

    private static boolean blast(ServerPlayer player, int rank, PlayerSkills skills) {
        double radius = AbilityScaling.blastRadius(rank, skills);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                entity -> entity != player && entity.isAlive()
        );
        if (targets.isEmpty()) return false;

        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().explosion(player, player), AbilityScaling.blastDamage(rank, skills));
            knockbackFrom(player, target, 0.35D + rank * 0.08D);
        }

        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 0.6D, player.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        ring(player, ParticleTypes.SMOKE, 18, radius * 0.6D);
        play(player, SoundEvents.GENERIC_EXPLODE.value());
        return true;
    }

    private static boolean blaze(ServerPlayer player, int rank, PlayerSkills skills) {
        double radius = AbilityScaling.blazeRadius(rank, skills);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                entity -> entity != player && entity.isAlive()
        );
        if (targets.isEmpty()) return false;

        int fireSeconds = AbilityScaling.blazeBurnSeconds(rank);
        boolean soulFire = rank >= AbilityId.BLAZE.maxRank();
        for (LivingEntity target : targets) {
            target.igniteForSeconds(fireSeconds);
            if (soulFire) {
                target.hurt(player.damageSources().magic(), 2.0F);
            }
        }

        ring(player, soulFire ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, 22, radius);
        ring(player, ParticleTypes.SMOKE, 16, radius);
        play(player, soulFire ? SoundEvents.SOUL_ESCAPE.value() : SoundEvents.BLAZE_SHOOT);
        return true;
    }

    private static boolean glacier(ServerPlayer player, int rank, PlayerSkills skills) {
        LivingEntity target = target(player, 12.0D);
        if (target == null) return false;

        Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.4D));
        Vec3 end = target.getBoundingBox().getCenter();
        if (end.subtract(start).lengthSqr() < 1.0E-4D) return false;

        List<LivingEntity> hits = player.level().getEntitiesOfClass(
                LivingEntity.class,
                new AABB(start, end).inflate(1.5D),
                entity -> entity != player && entity.isAlive() && distanceToLine(entity.getBoundingBox().getCenter(), start, end) <= 1.1D
        ).stream().sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(start))).toList();

        int remainingPierce = AbilityScaling.glacierPierce(rank);
        float damage = AbilityScaling.glacierDamage(rank, skills);
        boolean hitAny = false;
        for (LivingEntity hit : hits) {
            hit.hurt(player.damageSources().playerAttack(player), damage);
            burstAt(player, hit.position().add(0.0D, 0.8D, 0.0D), ParticleTypes.SNOWFLAKE, 8, 0.12D, 0.18D, 0.12D, 0.01D);
            hitAny = true;
            remainingPierce--;
            if (remainingPierce <= 0) break;
        }
        if (!hitAny) return false;

        line(player, ParticleTypes.SNOWFLAKE, start, end, 18);
        line(player, ParticleTypes.ITEM_SNOWBALL, start, end, 10);
        play(player, SoundEvents.GLASS_BREAK);
        return true;
    }

    private static boolean smite(ServerPlayer player, int rank, PlayerSkills skills) {
        double radius = AbilityScaling.smiteRadius(rank, skills);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                entity -> entity != player && entity.isAlive()
        ).stream()
                .sorted(Comparator.comparingDouble(LivingEntity::getMaxHealth).reversed())
                .limit(AbilityScaling.smiteTargets(rank))
                .toList();
        if (targets.isEmpty()) return false;

        boolean blueLightning = rank >= AbilityId.SMITE.maxRank();
        float damage = AbilityScaling.smiteDamage(rank, skills);
        for (LivingEntity target : targets) {
            spawnVisualLightning(player, target, blueLightning);
            target.hurt(player.damageSources().magic(), damage);
            burstAt(player, target.position().add(0.0D, 1.0D, 0.0D), blueLightning ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.ELECTRIC_SPARK, 18, 0.25D, 0.6D, 0.25D, 0.02D);
        }
        play(player, SoundEvents.LIGHTNING_BOLT_THUNDER);
        return true;
    }

    private static boolean magnetism(ServerPlayer player, int rank, PlayerSkills skills) {
        PlayerAbilities abilities = player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
        abilities.setActiveTicks(AbilityId.SCAVENGER, AbilityScaling.magnetismDurationTicks(rank, skills));
        ring(player, ParticleTypes.ENCHANT, 12, 1.0D);
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
        if (next % 10 == 0) {
            ring(player, ParticleTypes.ENCHANT, 8, Math.max(0.7D, radius * 0.4D));
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

    private static void burst(ServerPlayer player, ParticleOptions particle, int count, double spreadX, double spreadY, double spreadZ, double speed) {
        burstAt(player, player.position().add(0.0D, 1.0D, 0.0D), particle, count, spreadX, spreadY, spreadZ, speed);
    }

    private static void burstAt(ServerPlayer player, Vec3 pos, ParticleOptions particle, int count, double spreadX, double spreadY, double spreadZ, double speed) {
        if (!(player.level() instanceof ServerLevel level)) return;
        level.sendParticles(particle, pos.x, pos.y, pos.z, count, spreadX, spreadY, spreadZ, speed);
    }

    private static void ring(ServerPlayer player, ParticleOptions particle, int points, double radius) {
        if (!(player.level() instanceof ServerLevel level)) return;
        double centerY = player.getY() + 0.15D;
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0D * i) / points;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            level.sendParticles(particle, x, centerY + (i % 2 == 0 ? 0.0D : 0.25D), z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void line(ServerPlayer player, ParticleOptions particle, Vec3 start, Vec3 end, int steps) {
        if (!(player.level() instanceof ServerLevel level)) return;
        Vec3 delta = end.subtract(start);
        for (int i = 0; i <= steps; i++) {
            double progress = i / (double) steps;
            Vec3 pos = start.add(delta.scale(progress));
            level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static double distanceToLine(Vec3 point, Vec3 lineStart, Vec3 lineEnd) {
        Vec3 line = lineEnd.subtract(lineStart);
        double lengthSqr = line.lengthSqr();
        if (lengthSqr < 1.0E-4D) {
            return point.distanceTo(lineStart);
        }
        double t = Mth.clamp(point.subtract(lineStart).dot(line) / lengthSqr, 0.0D, 1.0D);
        Vec3 projected = lineStart.add(line.scale(t));
        return point.distanceTo(projected);
    }

    private static void spawnVisualLightning(ServerPlayer player, LivingEntity target, boolean blueLightning) {
        if (!(player.level() instanceof ServerLevel level)) return;

        LightningBolt lightningBolt = new LightningBolt(net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, level);
        lightningBolt.moveTo(target.getX(), target.getY(), target.getZ());
        lightningBolt.setVisualOnly(true);
        level.addFreshEntity(lightningBolt);

        if (blueLightning) {
            line(player, ParticleTypes.ELECTRIC_SPARK, target.position().add(0.0D, 3.0D, 0.0D), target.position().add(0.0D, 0.5D, 0.0D), 14);
            line(player, ParticleTypes.SOUL_FIRE_FLAME, target.position().add(0.1D, 3.0D, 0.1D), target.position().add(0.1D, 0.5D, 0.1D), 10);
        }
    }
}
