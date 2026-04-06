package net.revilodev.codex.skills.logic;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.revilodev.codex.item.ModItems;
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.skills.SkillConfig;
import net.revilodev.codex.skills.SkillsAttachments;
import net.revilodev.codex.skills.SkillsNetwork;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillSyncEvents {
    private SkillSyncEvents() {}

    private static final java.util.Set<UUID> PENDING = ConcurrentHashMap.newKeySet();

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SkillSyncEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(SkillSyncEvents::onRespawn);
        NeoForge.EVENT_BUS.addListener(SkillSyncEvents::onServerTickPost);
    }

    public static void markDirty(ServerPlayer sp) {
        if (sp == null) return;
        PENDING.add(sp.getUUID());
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        giveStartingBook(sp);
        applyEffectsNow(sp);
        markDirty(sp);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        applyEffectsNow(sp);
        markDirty(sp);
    }

    private static void onServerTickPost(ServerTickEvent.Post e) {
        if (PENDING.isEmpty()) return;

        MinecraftServer server = e.getServer();

        UUID[] ids = PENDING.toArray(new UUID[0]);
        PENDING.clear();

        for (UUID id : ids) {
            ServerPlayer sp = server.getPlayerList().getPlayer(id);
            if (sp == null) continue;
            SkillsNetwork.syncTo(sp);
        }
    }

    private static void applyEffectsNow(ServerPlayer sp) {
        PlayerSkills skills = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
        SkillLogic.applyAllEffects(sp, skills);
    }

    private static void giveStartingBook(ServerPlayer sp) {
        if (!SkillConfig.spawnWithSkillsBook()) return;

        var persistent = sp.getPersistentData();
        if (persistent.getBoolean("codex_received_skills_book")) return;

        ItemStack stack = new ItemStack(ModItems.SKILLS_BOOK.get());
        boolean added = sp.getInventory().add(stack);
        if (!added && !stack.isEmpty()) {
            sp.drop(stack, false);
        }
        persistent.putBoolean("codex_received_skills_book", true);
    }
}
