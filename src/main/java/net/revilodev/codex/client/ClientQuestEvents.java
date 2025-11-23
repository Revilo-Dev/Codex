package net.revilodev.codex.client;

import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.revilodev.codex.quest.QuestData;
import net.revilodev.codex.quest.QuestTracker;

public final class ClientQuestEvents {

    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn e) {
        QuestData.loadClient(true);
    }

    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        QuestTracker.forceSave();
    }

    public static void onClientLevelUnload(LevelEvent.Unload e) {
        if (!e.getLevel().isClientSide()) return;
        QuestTracker.forceSave();
    }
}
