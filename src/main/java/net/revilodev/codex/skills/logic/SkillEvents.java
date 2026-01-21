package net.revilodev.codex.skills.logic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.skills.SkillId;
import net.revilodev.codex.skills.SkillsAttachments;
import net.revilodev.codex.skills.SkillsNetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SkillEvents {
    private SkillEvents() {}

    private static final Map<UUID, Float> RAW_INCOMING = new HashMap<>();

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SkillEvents::onKill);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onFinalDamage);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onKnockback);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onPlayerTick);
    }

    private static void onKill(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        DamageSource src = event.getSource();
        if (!(src.getEntity() instanceof ServerPlayer sp)) return;

        PlayerSkills data = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
        boolean changed = SkillLogic.awardCombatKill(data);
        if (changed) SkillsNetwork.syncTo(sp);
    }

    private static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getPlayer() instanceof ServerPlayer sp)) return;

        PlayerSkills data = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
        boolean changed = SkillLogic.awardUtilityBlock(data);
        if (changed) SkillsNetwork.syncTo(sp);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;

        if (event.getEntity() instanceof ServerPlayer target) {
            PlayerSkills data = target.getData(SkillsAttachments.PLAYER_SKILLS.get());

            float raw = event.getAmount();
            RAW_INCOMING.put(target.getUUID(), raw);

            float reduced = SkillLogic.applyIncomingReductions(target, data, event.getSource(), raw);
            event.setAmount(reduced);
        }

        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            PlayerSkills data = attacker.getData(SkillsAttachments.PLAYER_SKILLS.get());
            float amt = event.getAmount();

            int sharp = data.level(SkillId.SHARPNESS);
            if (sharp > 0) amt += (float) (sharp * 0.20D);

            int power = data.level(SkillId.POWER);
            if (power > 0) amt += (float) (power * 0.15D);

            int crit = data.level(SkillId.CRIT_BONUS);
            if (crit > 0 && isCritical(attacker)) amt *= (float) (1.0D + crit * 0.01D);

            event.setAmount(amt);
        }
    }

    private static void onFinalDamage(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        Float raw = RAW_INCOMING.remove(sp.getUUID());
        if (raw == null) return;

        float finalAmt = event.getNewDamage();
        float evaded = raw - finalAmt;
        if (evaded <= 0.0F) return;

        PlayerSkills data = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
        boolean changed = SkillLogic.awardSurvivalEvaded(data, evaded);
        if (changed) SkillsNetwork.syncTo(sp);
    }

    private static void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        PlayerSkills data = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
        int lvl = data.level(SkillId.KNOCKBACK_RESISTANCE);
        if (lvl <= 0) return;

        float scale = (float) Math.max(0.0D, 1.0D - lvl * 0.02D);
        event.setStrength(event.getStrength() * scale);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 20 != 0) return;

        PlayerSkills skills = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
        SkillLogic.applyAllEffects(sp, skills);
    }

    private static boolean isCritical(ServerPlayer player) {
        if (player.onGround()) return false;
        if (player.isInWater() || player.isInLava()) return false;
        if (player.isPassenger()) return false;
        return player.fallDistance > 0.0F;
    }
}
