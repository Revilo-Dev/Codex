package net.revilodev.codex.skills.logic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.skills.SkillBalance;
import net.revilodev.codex.skills.SkillId;
import net.revilodev.codex.skills.SkillsAttachments;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.joml.Vector3f;

public final class SkillEvents {
    private SkillEvents() {}

    private static final Map<UUID, Snap> INCOMING = new HashMap<>();
    private static final ThreadLocal<Boolean> REAPPLYING_REDUCED_EFFECT = ThreadLocal.withInitial(() -> false);

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SkillEvents::onKill);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onLivingDrops);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onFinalDamage);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onKnockback);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onMobEffectApplicable);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onLogout);
    }

    private static void onKill(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        DamageSource src = event.getSource();
        if (!(src.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.isCreative() || sp.isSpectator()) return;
        SkillLogic.awardCombatKill(sp, mob);
    }

    private static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        PlayerSkills skills = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
        int looting = skills.level(SkillId.LOOTING);
        if (looting <= 0) return;
        double chance = SkillBalance.lootingChance(looting);
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (!stack.isEmpty() && sp.getRandom().nextDouble() < chance) stack.grow(1);
        }
    }

    private static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer sp)) return;
        String blockPath = event.getState().getBlockHolder().unwrapKey()
                .map(key -> key.location().getPath())
                .orElse("");
        if (!blockPath.contains("ore")) return;
        PlayerSkills skills = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
        int fortune = skills.level(SkillId.FORTUNE);
        if (fortune <= 0) return;
        int bonus = SkillBalance.fortuneBonus(fortune);
        if (bonus <= 0) return;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (!stack.isEmpty()) stack.setCount(stack.getCount() * (bonus + 1));
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        float amt = event.getAmount();

        if (event.getSource().getEntity() instanceof ServerPlayer attacker && !attacker.isSpectator()) {
            PlayerSkills data = attacker.getData(SkillsAttachments.PLAYER_SKILLS.get());
            boolean projectile = event.getSource().getDirectEntity() instanceof AbstractArrow;
            if (projectile) {
                int power = data.level(SkillId.POWER);
                if (power > 0) amt += (float) SkillBalance.powerDamage(power);
            }
            int lifeLeach = data.level(SkillId.HEALTH_BOOST);
            if (lifeLeach > 0 && amt > 0.0F) {
                int luckLevel = data.level(SkillId.LUCK);
                double leachChance = SkillBalance.lifeLeach(luckLevel);
                double leachAmount = SkillBalance.lifeLeachAmount();
                if (attacker.getRandom().nextDouble() < leachChance) {
                    LivingEntity target = event.getEntity();
                    float stolen = (float) (target.getMaxHealth() * leachAmount);
                    if (stolen > 0.0F) {
                        amt += stolen;
                        attacker.heal(stolen);
                        spawnLifeLeachParticles(attacker, target);
                    }
                }
            }
            int crit = data.level(SkillId.CRIT_POWER);
            if (crit > 0 && isCritical(attacker)) {
                amt *= (float) (1.0D + SkillBalance.critPowerDamage(crit));
            }
        }

        if (event.getEntity() instanceof ServerPlayer target && !target.isSpectator()) {
            PlayerSkills data = target.getData(SkillsAttachments.PLAYER_SKILLS.get());
            float raw = amt;
            float after = SkillLogic.applyIncomingReductions(target, data, event.getSource(), raw);
            Snap s = new Snap();
            s.raw = raw;
            s.afterSkill = after;
            INCOMING.put(target.getUUID(), s);
            amt = after;
        }

        event.setAmount(amt);
    }

    private static void onFinalDamage(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.isSpectator()) return;
        Snap s = INCOMING.remove(sp.getUUID());
        if (s == null) return;
        float preventedBySkills = s.raw - s.afterSkill;
        if (preventedBySkills <= 0.0F) return;
        SkillLogic.awardSurvivalPrevented(sp, preventedBySkills);
    }

    private static void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.isSpectator()) return;
        PlayerSkills data = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
        int lvl = data.level(SkillId.KNOCKBACK_RESISTANCE);
        if (lvl <= 0) return;
        float scale = (float) Math.max(0.0D, 1.0D - SkillBalance.knockbackResistance(lvl));
        event.setStrength(event.getStrength() * scale);
    }

    private static void onBreakSpeed(BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerSkills data = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
        int haste = data.level(SkillId.HASTE);
        if (haste <= 0) return;
        event.setNewSpeed((float) (event.getNewSpeed() + SkillBalance.hasteBreakSpeed(haste)));
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;
        if (sp.tickCount % 20 != 0) return;
        PlayerSkills skills = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
        SkillLogic.applyAllEffects(sp, skills);
    }

    private static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (REAPPLYING_REDUCED_EFFECT.get()) return;

        MobEffectInstance effect = event.getEffectInstance();
        if (effect == null || effect.getEffect().value().isBeneficial()) return;

        PlayerSkills skills = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
        int cleanse = skills.level(SkillId.CLEANSE);
        if (cleanse <= 0) return;

        double reduction = SkillBalance.cleanseImmunities(cleanse);
        if (reduction <= 0.0D) return;

        MobEffectInstance reduced = scaledNegativeEffect(effect, reduction);
        if (reduced == effect) return;

        event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        REAPPLYING_REDUCED_EFFECT.set(true);
        try {
            sp.addEffect(reduced, event.getEffectSource());
        } finally {
            REAPPLYING_REDUCED_EFFECT.set(false);
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        UUID id = sp.getUUID();
        INCOMING.remove(id);
        SkillLogic.clearStreaks(id);
    }

    private static boolean isCritical(ServerPlayer player) {
        if (player.onGround()) return false;
        if (player.isInWater() || player.isInLava()) return false;
        if (player.isPassenger()) return false;
        return player.fallDistance > 0.0F;
    }

    private static void spawnLifeLeachParticles(ServerPlayer attacker, LivingEntity target) {
        if (!(attacker.level() instanceof ServerLevel level)) return;

        Vector3f color = new Vector3f(1.0F, 0.1F, 0.1F);
        DustParticleOptions redDust = new DustParticleOptions(color, 1.0F);

        int count = 20;
        for (int i = 0; i < count; i++) {
            double t = (i + 1.0D) / (count + 1.0D);
            double baseX = target.getX() + (attacker.getX() - target.getX()) * t;
            double baseY = target.getY(0.6D) + (attacker.getY(0.8D) - target.getY(0.6D)) * t;
            double baseZ = target.getZ() + (attacker.getZ() - target.getZ()) * t;

            double velocityX = (attacker.getX() - baseX) * 0.08D;
            double velocityY = (attacker.getY(0.8D) - baseY) * 0.08D;
            double velocityZ = (attacker.getZ() - baseZ) * 0.08D;

            level.sendParticles(redDust, baseX, baseY, baseZ, 0, velocityX, velocityY, velocityZ, 1.0D);
        }
    }

    private static MobEffectInstance scaledNegativeEffect(MobEffectInstance effect, double reduction) {
        int duration = Math.max(1, (int) Math.ceil(effect.getDuration() * (1.0D - reduction)));
        int amplifier = effect.getAmplifier();
        int scaledAmplifier = Math.max(0, (int) Math.ceil((amplifier + 1) * (1.0D - reduction)) - 1);

        if (duration == effect.getDuration() && scaledAmplifier == amplifier) {
            return effect;
        }

        return new MobEffectInstance(
                effect.getEffect(),
                duration,
                scaledAmplifier,
                effect.isAmbient(),
                effect.isVisible(),
                effect.showIcon()
        );
    }

    private static final class Snap {
        float raw;
        float afterSkill;
    }
}
