package net.revilodev.codex.abilities.logic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.revilodev.codex.abilities.AbilitiesAttachments;
import net.revilodev.codex.abilities.AbilityConfig;
import net.revilodev.codex.abilities.AbilityId;
import net.revilodev.codex.abilities.PlayerAbilities;
import net.revilodev.codex.abilities.event.AbilityUseEvent;
import net.revilodev.codex.attributes.CodexAttributes;
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.skills.SkillsAttachments;
import net.revilodev.codex.stats.CodexStats;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class AbilityLogic {
    private AbilityLogic() {}

    public static boolean tryActivate(ServerPlayer player, AbilityId id) {
        if (player == null || id == null) return false;

        PlayerAbilities abilities = player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
        if (!AbilityConfig.enabled(id)) return false;

        int rank = abilities.rank(id);
        if (rank <= 0 || abilities.cooldownTicks(id) > 0) return false;

        PlayerSkills skills = player.getData(SkillsAttachments.PLAYER_SKILLS.get());
        double abilityPower = CodexAttributes.abilityPower(player, id);
        AbilityUseEvent.Pre preEvent = new AbilityUseEvent.Pre(player, id, rank, skills, abilityPower);
        if (NeoForge.EVENT_BUS.post(preEvent).isCanceled()) return false;

        abilityPower = preEvent.getAbilityPower();
        boolean executed = execute(player, id, rank, skills, abilityPower);
        if (!executed) return false;

        abilities.setCooldown(id, AbilityScaling.cooldownTicks(id, rank, skills));
        abilities.markUsed(id);
        player.awardStat(CodexStats.ABILITIES_USED);
        player.awardStat(CodexStats.abilityUse(id));
        NeoForge.EVENT_BUS.post(new AbilityUseEvent.Post(player, id, rank, skills, abilityPower));
        return true;
    }

    private static boolean execute(ServerPlayer player, AbilityId id, int rank, PlayerSkills skills, double abilityPower) {
        return switch (id) {
            case DASH -> dash(player, rank, skills, abilityPower);
            case LEAP -> leap(player, rank, skills, abilityPower);
            case LUNGE -> lunge(player, rank, skills, abilityPower);
            case HEAL -> heal(player, rank, skills, abilityPower);
            case CLEANSE -> cleanse(player, rank, skills, abilityPower);
            case WARCRY -> warcry(player, rank, skills, abilityPower);
            case EXECUTION -> execution(player, rank, skills, abilityPower);
            case CLEAVE -> cleave(player, rank, skills, abilityPower);
            case OVERPOWER -> overpower(player, rank, skills, abilityPower);
            case GUARD -> guard(player, rank, skills, abilityPower);
            case BLAST -> blast(player, rank, skills, abilityPower);
            case BLAZE -> blaze(player, rank, skills, abilityPower);
            case GLACIER -> glacier(player, rank, skills, abilityPower);
            case SMITE -> smite(player, rank, skills, abilityPower);
            case SCAVENGER -> toxic(player, rank, skills, abilityPower);
        };
    }

    private static boolean dash(ServerPlayer player, int rank, PlayerSkills skills, double abilityPower) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z).normalize().scale(AbilityScaling.dashDistance(rank, skills, abilityPower));
        player.push(horizontal.x, 0.08D, horizontal.z);
        player.hurtMarked = true;
        burst(player, ParticleTypes.CLOUD, 18, 0.38D, 0.12D, 0.38D, 0.02D);
        play(player, SoundEvents.BREEZE_WIND_CHARGE_BURST.value());
        return true;
    }

    private static boolean leap(ServerPlayer player, int rank, PlayerSkills skills, double abilityPower) {
        Vec3 look = player.getLookAngle();
        Vec3 launch = new Vec3(look.x, 0.0D, look.z).normalize().scale(AbilityScaling.leapForward(rank, skills, abilityPower));
        player.setDeltaMovement(launch.x, AbilityScaling.leapVertical(rank, skills, abilityPower), launch.z);
        player.hurtMarked = true;
        burst(player, ParticleTypes.POOF, 16, 0.25D, 0.05D, 0.25D, 0.02D);
        play(player, SoundEvents.GOAT_LONG_JUMP);
        return true;
    }

    private static boolean lunge(ServerPlayer player, int rank, PlayerSkills skills, double abilityPower) {
        PlayerAbilities abilities = player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
        int dashRank = Math.max(0, abilities.rank(AbilityId.DASH));
        LivingEntity target = target(player, AbilityScaling.lungeDistance(rank, dashRank, skills, abilityPower));
        if (target == null) return false;

        Vec3 toward = target.position().subtract(player.position());
        Vec3 horizontal = new Vec3(toward.x, 0.0D, toward.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            horizontal = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
        }
        Vec3 motion = horizontal.normalize().scale(AbilityScaling.lungeDistance(rank, dashRank, skills, abilityPower) * 0.35D);
        player.setDeltaMovement(motion.x, 0.18D, motion.z);
        player.hurtMarked = true;
        float speedBonus = AbilityScaling.lungeSpeedDamageBonus(new Vec3(motion.x, 0.0D, motion.z).length());
        target.hurt(player.damageSources().playerAttack(player), weaponScaledDamage(player, AbilityScaling.lungeDamage(rank, dashRank, skills, abilityPower) + speedBonus));
        knockbackFrom(player, target, AbilityScaling.lungeKnockback(rank, dashRank, skills, abilityPower));
        line(player, ParticleTypes.SWEEP_ATTACK, player.position().add(0.0D, 1.0D, 0.0D), target.position().add(0.0D, 1.0D, 0.0D), 7);
        burst(player, ParticleTypes.CLOUD, 10, 0.2D, 0.1D, 0.2D, 0.01D);
        play(player, SoundEvents.PLAYER_ATTACK_KNOCKBACK);
        return true;
    }

    private static boolean heal(ServerPlayer player, int rank, PlayerSkills skills, double abilityPower) {
        player.heal(AbilityScaling.healAmount(rank, skills, abilityPower));
        ring(player, ParticleTypes.HEART, 10, 0.95D);
        play(player, SoundEvents.AMETHYST_BLOCK_CHIME);
        return true;
    }

    private static boolean cleanse(ServerPlayer player, int rank, PlayerSkills skills, double abilityPower) {
        boolean removed = false;
        for (MobEffectInstance effect : List.copyOf(player.getActiveEffects())) {
            if (!effect.getEffect().value().isBeneficial()) {
                player.removeEffect(effect.getEffect());
                removed = true;
            }
        }
        if (player.isOnFire()) {
            player.clearFire();
            removed = true;
        }
        player.heal(AbilityScaling.cleanseHeal(rank, skills, abilityPower));
        burst(player, removed ? ParticleTypes.WAX_OFF : ParticleTypes.HAPPY_VILLAGER, 16, 0.35D, 0.35D, 0.35D, 0.01D);
        play(player, removed ? SoundEvents.GENERIC_DRINK : SoundEvents.AMETHYST_BLOCK_CHIME);
        return true;
    }

    private static boolean guard(ServerPlayer player, int rank, PlayerSkills skills, double abilityPower) {
        int duration = AbilityScaling.guardDurationTicks(rank, skills, abilityPower);
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

    private static boolean warcry(ServerPlayer player, int rank, PlayerSkills skills, double abilityPower) {
        int duration = AbilityScaling.warcryDurationTicks(rank, skills, abilityPower);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, AbilityScaling.warcryStrengthAmp(rank, skills), false, true, true));
        burst(player, ParticleTypes.ANGRY_VILLAGER, 8, 0.45D, 0.2D, 0.45D, 0.01D);
        play(player, SoundEvents.RAID_HORN.value());
        return true;
    }

    private static boolean execution(ServerPlayer player, int rank, PlayerSkills skills, double abilityPower) {
        LivingEntity target = target(player, 4.5D);
        if (target == null) return false;
        float damage = AbilityScaling.executionDamage(rank, skills, abilityPower);
        if (target.getHealth() <= target.getMaxHealth() * 0.35F) {
            damage *= AbilityScaling.executionMissingHealthBonus(rank);
        }
        target.hurt(player.damageSources().playerAttack(player), weaponScaledDamage(player, damage));
        burst(player, ParticleTypes.CRIT, 12, 0.2D, 0.2D, 0.2D, 0.15D);
        play(player, SoundEvents.PLAYER_ATTACK_CRIT);
        return true;
    }

    private static boolean cleave(ServerPlayer player, int rank, PlayerSkills skills, double abilityPower) {
        double radius = AbilityScaling.cleaveRadius(rank, abilityPower);
        Vec3 facing = player.getLookAngle().normalize();
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                entity -> entity != player && entity.isAlive() && isInFront(player, entity, facing));
        if (targets.isEmpty()) return false;
        float damage = AbilityScaling.cleaveDamage(rank, skills, abilityPower);
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().playerAttack(player), weaponScaledDamage(player, damage));
            knockbackFrom(player, target, 0.25D + rank * 0.05D);
        }
        ring(player, ParticleTypes.SWEEP_ATTACK, 9, 1.2D);
        play(player, SoundEvents.PLAYER_ATTACK_SWEEP);
        return true;
    }

    private static boolean overpower(ServerPlayer player, int rank, PlayerSkills skills, double abilityPower) {
        LivingEntity target = target(player, 4.0D);
        if (target == null) return false;
        target.hurt(player.damageSources().playerAttack(player), weaponScaledDamage(player, AbilityScaling.overpowerDamage(rank, skills, abilityPower)));
        knockbackFrom(player, target, AbilityScaling.overpowerKnockback(rank, skills, abilityPower));
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(
                LivingEntity.class,
                target.getBoundingBox().inflate(1.6D + rank * 0.12D),
                entity -> entity != player && entity != target && entity.isAlive()
        );
        for (LivingEntity other : nearby) {
            knockbackFrom(target, other, 0.3D + rank * 0.04D);
        }
        target.setDeltaMovement(target.getDeltaMovement().add(0.0D, 0.16D + rank * 0.03D, 0.0D));
        burstAt(player, target.position().add(0.0D, 0.6D, 0.0D), ParticleTypes.CRIT, 14, 0.15D, 0.2D, 0.15D, 0.18D);
        burstAt(player, target.position().add(0.0D, 0.25D, 0.0D), ParticleTypes.SWEEP_ATTACK, 6, 0.45D, 0.08D, 0.45D, 0.0D);
        play(player, SoundEvents.MACE_SMASH_GROUND_HEAVY);
        return true;
    }

    private static boolean blast(ServerPlayer player, int rank, PlayerSkills skills, double abilityPower) {
        double radius = AbilityScaling.blastRadius(rank, skills, abilityPower);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                entity -> entity != player && entity.isAlive()
        );
        if (targets.isEmpty()) return false;

        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().explosion(player, player), weaponScaledDamage(player, AbilityScaling.blastDamage(rank, skills, abilityPower)));
            knockbackFrom(player, target, 0.35D + rank * 0.08D);
        }

        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 0.6D, player.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        ring(player, ParticleTypes.SMOKE, 18, radius * 0.6D);
        play(player, SoundEvents.GENERIC_EXPLODE.value());
        return true;
    }

    private static boolean blaze(ServerPlayer player, int rank, PlayerSkills skills, double abilityPower) {
        double radius = AbilityScaling.blazeRadius(rank, skills, abilityPower);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                entity -> entity != player && entity.isAlive()
        );
        if (targets.isEmpty()) return false;

        int fireSeconds = AbilityScaling.blazeBurnSeconds(rank, abilityPower);
        boolean soulFire = rank >= AbilityId.BLAZE.maxRank();
        for (LivingEntity target : targets) {
            target.igniteForSeconds(fireSeconds);
            if (soulFire) {
                target.hurt(player.damageSources().magic(), weaponScaledDamage(player, 2.0F));
            }
        }

        ring(player, soulFire ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, 22, radius);
        ring(player, ParticleTypes.SMOKE, 16, radius);
        play(player, soulFire ? SoundEvents.SOUL_ESCAPE.value() : SoundEvents.BLAZE_SHOOT);
        return true;
    }

    private static boolean glacier(ServerPlayer player, int rank, PlayerSkills skills, double abilityPower) {
        LivingEntity primaryTarget = target(player, 12.0D);
        if (primaryTarget == null) return false;

        List<LivingEntity> projectiles = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(12.0D),
                entity -> entity != player && entity.isAlive()
        ).stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .toList();
        java.util.ArrayList<LivingEntity> targets = new java.util.ArrayList<>();
        targets.add(primaryTarget);
        for (LivingEntity candidate : projectiles) {
            if (targets.size() >= AbilityScaling.glacierProjectiles(rank)) break;
            if (candidate == primaryTarget || targets.contains(candidate)) continue;
            targets.add(candidate);
        }

        float damage = AbilityScaling.glacierDamage(rank, skills, abilityPower);
        Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.4D));
        Vec3 impactPos = primaryTarget.getBoundingBox().getCenter();
        for (LivingEntity hit : targets) {
            Vec3 end = hit.getBoundingBox().getCenter();
            if (end.subtract(start).lengthSqr() < 1.0E-4D) continue;
            hit.hurt(player.damageSources().playerAttack(player), weaponScaledDamage(player, damage));
            line(player, ParticleTypes.SNOWFLAKE, start, end, 18);
            line(player, ParticleTypes.ITEM_SNOWBALL, start, end, 10);
            burstAt(player, hit.position().add(0.0D, 0.8D, 0.0D), ParticleTypes.SNOWFLAKE, 8, 0.12D, 0.18D, 0.12D, 0.01D);
            impactPos = end;
        }

        if (rank >= AbilityId.GLACIER.maxRank()) {
            shrapnelBurst(player, impactPos, damage * 0.6F, AbilityScaling.glacierProjectiles(rank));
        }

        play(player, SoundEvents.GLASS_BREAK);
        return true;
    }

    private static boolean smite(ServerPlayer player, int rank, PlayerSkills skills, double abilityPower) {
        double radius = AbilityScaling.smiteRadius(rank, skills, abilityPower);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                entity -> entity != player && entity.isAlive() && entity instanceof Enemy
        ).stream()
                .sorted(Comparator.comparingDouble(LivingEntity::getMaxHealth).reversed())
                .limit(AbilityScaling.smiteTargets(rank))
                .toList();
        if (targets.isEmpty()) return false;

        boolean blueLightning = rank >= AbilityId.SMITE.maxRank();
        float damage = AbilityScaling.smiteDamage(rank, skills, abilityPower);
        for (LivingEntity target : targets) {
            spawnVisualLightning(player, target, blueLightning);
            target.hurt(player.damageSources().magic(), weaponScaledDamage(player, damage));
            burstAt(player, target.position().add(0.0D, 1.0D, 0.0D), blueLightning ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.ELECTRIC_SPARK, 18, 0.25D, 0.6D, 0.25D, 0.02D);
        }
        play(player, SoundEvents.LIGHTNING_BOLT_THUNDER);
        return true;
    }

    private static boolean toxic(ServerPlayer player, int rank, PlayerSkills skills, double abilityPower) {
        double radius = AbilityScaling.toxicRadius(rank, skills, abilityPower);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                entity -> entity != player && entity.isAlive() && entity instanceof Enemy
        );
        if (targets.isEmpty()) return false;

        int poisonTicks = AbilityScaling.toxicPoisonTicks(rank, skills, abilityPower);
        int amplifier = AbilityScaling.toxicAmplifier(rank);
        float damage = AbilityScaling.toxicDamage(rank, skills, abilityPower);
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().magic(), weaponScaledDamage(player, damage));
            target.addEffect(new MobEffectInstance(MobEffects.POISON, poisonTicks, amplifier, false, true, true));
            burstAt(player, target.position().add(0.0D, 0.8D, 0.0D), ParticleTypes.WITCH, 8, 0.18D, 0.25D, 0.18D, 0.01D);
        }

        ring(player, ParticleTypes.WITCH, 18, radius);
        ring(player, ParticleTypes.ITEM_SLIME, 14, Math.max(0.8D, radius * 0.8D));
        play(player, SoundEvents.WITCH_THROW);
        return true;
    }

    public static void tickActive(ServerPlayer player, PlayerAbilities abilities, PlayerSkills skills) {
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

    private static void knockbackFrom(LivingEntity source, LivingEntity target, double strength) {
        Vec3 dir = target.position().subtract(source.position());
        Vec3 horizontal = new Vec3(dir.x, 0.0D, dir.z);
        if (horizontal.lengthSqr() < 1.0E-4D) horizontal = source.getLookAngle();
        horizontal = horizontal.normalize().scale(strength);
        target.push(horizontal.x, 0.1D, horizontal.z);
        target.hurtMarked = true;
    }

    private static float weaponScaledDamage(ServerPlayer player, float baseDamage) {
        return baseDamage + heldWeaponAttackDamage(player);
    }

    private static float heldWeaponAttackDamage(ServerPlayer player) {
        if (player == null) return 0.0F;

        AtomicReference<Double> bonus = new AtomicReference<>(0.0D);
        player.getMainHandItem().forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (!attribute.is(Attributes.ATTACK_DAMAGE)) return;

            double current = bonus.get();
            if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                bonus.set(current + modifier.amount());
            }
        });
        return Math.max(0.0F, bonus.get().floatValue());
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

    private static void shrapnelBurst(ServerPlayer player, Vec3 impactPos, float damage, int pierceBudget) {
        List<LivingEntity> shards = player.level().getEntitiesOfClass(
                LivingEntity.class,
                new AABB(impactPos, impactPos).inflate(3.2D),
                entity -> entity != player && entity.isAlive()
        ).stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(impactPos.x, impactPos.y, impactPos.z)))
                .limit(Math.max(2, pierceBudget))
                .toList();

        for (LivingEntity shardTarget : shards) {
            Vec3 targetPos = shardTarget.getBoundingBox().getCenter();
            shardTarget.hurt(player.damageSources().playerAttack(player), weaponScaledDamage(player, damage));
            line(player, ParticleTypes.SNOWFLAKE, impactPos, targetPos, 6);
            line(player, ParticleTypes.ITEM_SNOWBALL, impactPos, targetPos, 4);
            burstAt(player, targetPos, ParticleTypes.SNOWFLAKE, 5, 0.12D, 0.12D, 0.12D, 0.01D);
        }

        burstAt(player, impactPos, ParticleTypes.ITEM_SNOWBALL, 12, 0.35D, 0.18D, 0.35D, 0.05D);
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
