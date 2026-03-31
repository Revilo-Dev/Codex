package net.revilodev.codex.abilities.logic;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.revilodev.codex.abilities.AbilitiesAttachments;
import net.revilodev.codex.abilities.AbilitiesNetwork;
import net.revilodev.codex.abilities.PlayerAbilities;
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.skills.SkillsAttachments;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AbilitySyncEvents {
    private static final java.util.Set<UUID> PENDING = ConcurrentHashMap.newKeySet();

    private AbilitySyncEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(AbilitySyncEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(AbilitySyncEvents::onRespawn);
        NeoForge.EVENT_BUS.addListener(AbilitySyncEvents::onServerTickPost);
    }

    public static void markDirty(ServerPlayer player) {
        if (player != null) PENDING.add(player.getUUID());
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) markDirty(player);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) markDirty(player);
    }

    private static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerAbilities data = player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
            data.tickCooldowns();
            PlayerSkills skills = player.getData(SkillsAttachments.PLAYER_SKILLS.get());
            AbilityLogic.tickActive(player, data, skills);
        }

        if (PENDING.isEmpty()) return;
        UUID[] ids = PENDING.toArray(new UUID[0]);
        PENDING.clear();
        for (UUID id : ids) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) AbilitiesNetwork.syncTo(player);
        }
    }
}
