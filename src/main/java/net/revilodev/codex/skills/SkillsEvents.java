package net.revilodev.codex.skills;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SkillsEvents {
    private SkillsEvents() {}

    private static final Map<UUID, Float> LAST_INCOMING = new HashMap<>();

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SkillsEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(SkillsEvents::onRespawn);
        NeoForge.EVENT_BUS.addListener(SkillsEvents::onKill);
        NeoForge.EVENT_BUS.addListener(SkillsEvents::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(SkillsEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(SkillsEvents::onFinalDamage);
        NeoForge.EVENT_BUS.addListener(SkillsEvents::onKnockback);
        NeoForge.EVENT_BUS.addListener(SkillsEvents::onPlayerTick);
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerSkills skills = sp.getData(SkillsAttachments.PLAYER_SKILLS);
        SkillsEffects.applyServerTickEffects(sp, skills);
        SkillsNetwork.syncTo(sp);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerSkills skills = sp.getData(SkillsAttachments.PLAYER_SKILLS);
        SkillsEffects.applyServerTickEffects(sp, skills);
        SkillsNetwork.syncTo(sp);
    }

    private static void onKill(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        DamageSource src = event.getSource();
        if (!(src.getEntity() instanceof ServerPlayer sp)) return;

        PlayerSkills skills = sp.getData(SkillsAttachments.PLAYER_SKILLS);
        skills.addPoints(SkillCategory.COMBAT, 1);
        SkillsNetwork.syncTo(sp);
    }

    private static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getPlayer() instanceof ServerPlayer sp)) return;

        PlayerSkills skills = sp.getData(SkillsAttachments.PLAYER_SKILLS);
        skills.addPoints(SkillCategory.UTILITY, 1);
        SkillsNetwork.syncTo(sp);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;

        DamageSource src = event.getSource();

        if (event.getEntity() instanceof ServerPlayer target) {
            PlayerSkills ts = target.getData(SkillsAttachments.PLAYER_SKILLS);
            float amt = event.getAmount();

            int fire = ts.level(SkillId.FIRE_RESISTANCE);
            if (fire > 0 && src.is(DamageTypeTags.IS_FIRE)) amt *= (float) Math.max(0.0D, 1.0D - fire * 0.02D);

            int blast = ts.level(SkillId.BLAST_RESISTANCE);
            if (blast > 0 && src.is(DamageTypeTags.IS_EXPLOSION)) amt *= (float) Math.max(0.0D, 1.0D - blast * 0.02D);

            int projRes = ts.level(SkillId.PROJECTILE_RESISTANCE);
            if (projRes > 0 && src.is(DamageTypeTags.IS_PROJECTILE)) amt *= (float) Math.max(0.0D, 1.0D - projRes * 0.02D);

            LAST_INCOMING.put(target.getUUID(), amt);
            event.setAmount(amt);
        }

        if (src.getEntity() instanceof ServerPlayer attacker) {
            PlayerSkills as = attacker.getData(SkillsAttachments.PLAYER_SKILLS);
            float amt = event.getAmount();

            boolean projectile = event.getSource().getDirectEntity() instanceof AbstractArrow;
            if (projectile) {
                int p = as.level(SkillId.POWER);
                if (p > 0) amt += (float) (p * 0.15D);
            } else {
                int sh = as.level(SkillId.SHARPNESS);
                if (sh > 0) amt += (float) (sh * 0.20D);

                int crit = as.level(SkillId.CRIT_BONUS);
                if (crit > 0 && isCritical(attacker)) {
                    amt *= (float) (1.0D + crit * 0.01D);
                }
            }

            event.setAmount(amt);
        }
    }

    private static void onFinalDamage(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        Float incoming = LAST_INCOMING.remove(sp.getUUID());
        if (incoming == null) return;

        float finalAmt = event.getNewDamage();
        float prevented = incoming - finalAmt;
        if (prevented <= 0.0F) return;

        int pts = (int) (prevented / 2.0F);
        if (pts <= 0) return;

        PlayerSkills skills = sp.getData(SkillsAttachments.PLAYER_SKILLS);
        skills.addPoints(SkillCategory.SURVIVAL, pts);
        SkillsNetwork.syncTo(sp);
    }

    private static void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        PlayerSkills skills = sp.getData(SkillsAttachments.PLAYER_SKILLS);
        int lvl = skills.level(SkillId.KNOCKBACK_RESISTANCE);
        if (lvl <= 0) return;

        float scale = (float) Math.max(0.0D, 1.0D - lvl * 0.02D);
        event.setStrength(event.getStrength() * scale);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 20 != 0) return;
        SkillsEffects.applyServerTickEffects(sp, sp.getData(SkillsAttachments.PLAYER_SKILLS));
    }

    private static boolean isCritical(ServerPlayer player) {
        if (player.onGround()) return false;
        if (player.isInWater() || player.isInLava()) return false;
        if (player.isPassenger()) return false;
        return player.fallDistance > 0.0F;
    }
}
