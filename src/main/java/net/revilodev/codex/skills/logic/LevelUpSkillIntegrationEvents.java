package net.revilodev.codex.skills.logic;

import com.revilo.levelup.event.LevelUpLevelChangedEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.skills.SkillConfig;
import net.revilodev.codex.skills.SkillsAttachments;

public final class LevelUpSkillIntegrationEvents {
    private LevelUpSkillIntegrationEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(LevelUpSkillIntegrationEvents::onLevelStep);
    }

    private static void onLevelStep(LevelUpLevelChangedEvent.LevelUp event) {
        ServerPlayer player = event.getPlayer();
        int levelsGained = Math.max(0, event.getNewLevel() - event.getOldLevel());
        if (levelsGained <= 0) return;

        PlayerSkills skills = player.getData(SkillsAttachments.PLAYER_SKILLS.get());
        skills.adminAddPoints(levelsGained * Math.max(0, SkillConfig.pointsPerLevel()));
        SkillSyncEvents.markDirty(player);
    }
}
