package net.revilodev.codex.skills.logic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.skills.SkillBalance;
import net.revilodev.codex.skills.SkillId;
import net.revilodev.codex.skills.SkillsAttachments;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SkillEvents {
    private SkillEvents() {}

    private static final Map<UUID, Snap> INCOMING = new HashMap<>();

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SkillEvents::onKill);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onFinalDamage);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onKnockback);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onLogout);
    }

    private static void onKill(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        DamageSource src = event.getSource();
        if (!(src.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.isCreative() || sp.isSpectator()) return;

        PlayerSkills data = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
        boolean changed = SkillLogic.awardCombatKill(sp, data, event.getEntity());
        if (changed) SkillSyncEvents.markDirty(sp);
    }

    private static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.isCanceled()) return;
        if (!(event.getPlayer() instanceof ServerPlayer sp)) return;
        if (sp.isCreative() || sp.isSpectator()) return;

        PlayerSkills data = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
        boolean changed = SkillLogic.awardUtilityBlock(sp, data, event.getState(), event.getLevel(), event.getPos());
        if (changed) SkillSyncEvents.markDirty(sp);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;

        float amt = event.getAmount();

        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            if (!attacker.isSpectator()) {
                PlayerSkills data = attacker.getData(SkillsAttachments.PLAYER_SKILLS.get());

                boolean projectile = event.getSource().getDirectEntity() instanceof AbstractArrow;
                if (projectile) {
                    int power = data.level(SkillId.POWER);
                    if (power > 0) amt += (float) (power * SkillBalance.POWER_DAMAGE_PER_LEVEL);
                } else {
                    int sharp = data.level(SkillId.SHARPNESS);
                    if (sharp > 0) amt += (float) (sharp * SkillBalance.SHARPNESS_DAMAGE_PER_LEVEL);

                    int crit = data.level(SkillId.CRIT_BONUS);
                    if (crit > 0 && isCritical(attacker)) {
                        amt *= (float) (1.0D + (crit * SkillBalance.CRIT_BONUS_PCT_PER_LEVEL) / 100.0D);
                    }
                }
            }
        }

        if (event.getEntity() instanceof ServerPlayer target) {
            if (!target.isSpectator()) {
                PlayerSkills data = target.getData(SkillsAttachments.PLAYER_SKILLS.get());
                float raw = amt;
                float after = SkillLogic.applyIncomingReductions(target, data, event.getSource(), raw);

                Snap s = new Snap();
                s.raw = raw;
                s.afterSkill = after;
                INCOMING.put(target.getUUID(), s);

                amt = after;
            }
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

        PlayerSkills data = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
        boolean changed = SkillLogic.awardSurvivalPrevented(data, preventedBySkills);
        if (changed) SkillSyncEvents.markDirty(sp);
    }

    private static void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.isSpectator()) return;

        PlayerSkills data = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
        int lvl = data.level(SkillId.KNOCKBACK_RESISTANCE);
        if (lvl <= 0) return;

        float scale = (float) Math.max(0.0D, 1.0D - (lvl * SkillBalance.KNOCKBACK_RES_PCT_PER_LEVEL) / 100.0D);
        event.setStrength(event.getStrength() * scale);
    }

    private static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.isSpectator() || sp.isCreative()) return;

        PlayerSkills data = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
        int eff = data.level(SkillId.EFFICIENCY);
        int chop = data.level(SkillId.CHOPPING);
        if (eff <= 0 && chop <= 0) return;

        float speed = event.getNewSpeed();
        if (speed <= 0.0F) return;

        float mult = 1.0F;
        if (eff > 0) {
            mult += (float) ((eff * SkillBalance.EFFICIENCY_PCT_PER_LEVEL) / 100.0D);
        }
        if (chop > 0 && event.getState().is(BlockTags.MINEABLE_WITH_AXE)) {
            mult += (float) ((chop * SkillBalance.CHOPPING_PCT_PER_LEVEL) / 100.0D);
        }

        if (mult != 1.0F) event.setNewSpeed(speed * mult);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;
        if (sp.tickCount % 20 != 0) return;

        PlayerSkills skills = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
        SkillLogic.applyAllEffects(sp, skills);
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

    private static final class Snap {
        float raw;
        float afterSkill;
    }
}
