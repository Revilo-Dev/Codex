package net.revilodev.codex.skills.logic;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
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
        markDirty(sp);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
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
}
