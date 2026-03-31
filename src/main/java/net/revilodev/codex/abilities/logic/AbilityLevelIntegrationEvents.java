package net.revilodev.codex.abilities.logic;

import com.revilo.levelup.event.LevelUpLevelChangedEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.revilodev.codex.abilities.AbilitiesAttachments;
import net.revilodev.codex.abilities.AbilitiesNetwork;
import net.revilodev.codex.abilities.AbilityConfig;
import net.revilodev.codex.abilities.PlayerAbilities;

public final class AbilityLevelIntegrationEvents {
    private AbilityLevelIntegrationEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(AbilityLevelIntegrationEvents::onLevelStep);
    }

    private static void onLevelStep(LevelUpLevelChangedEvent.LevelUp event) {
        ServerPlayer player = event.getPlayer();
        int interval = Math.max(1, AbilityConfig.pointIntervalLevels());
        int oldThresholds = Math.max(0, event.getOldLevel()) / interval;
        int newThresholds = Math.max(0, event.getNewLevel()) / interval;
        int gained = Math.max(0, newThresholds - oldThresholds);
        if (gained <= 0) return;

        PlayerAbilities abilities = player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
        abilities.addPoints(gained);
        AbilitySyncEvents.markDirty(player);
        AbilitiesNetwork.sendAbilityPointToast(player, gained, abilities.points());
    }
}
