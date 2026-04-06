package net.revilodev.codex.skills.logic;

import com.revilo.levelup.event.LevelUpLevelChangedEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.abilities.AbilitiesAttachments;
import net.revilodev.codex.abilities.AbilityConfig;
import net.revilodev.codex.abilities.PlayerAbilities;
import net.revilodev.codex.skills.SkillConfig;
import net.revilodev.codex.skills.SkillsAttachments;
import net.revilodev.codex.skills.SkillsNetwork;

public final class LevelUpSkillIntegrationEvents {
    private LevelUpSkillIntegrationEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(LevelUpSkillIntegrationEvents::onLevelStep);
    }

    private static void onLevelStep(LevelUpLevelChangedEvent.LevelUp event) {
        ServerPlayer player = event.getPlayer();
        int levelsGained = Math.max(0, event.getNewLevel() - event.getOldLevel());
        if (levelsGained <= 0) return;

        int skillPointsGained = levelsGained * Math.max(0, SkillConfig.pointsPerLevel());
        PlayerSkills skills = player.getData(SkillsAttachments.PLAYER_SKILLS.get());
        skills.adminAddPoints(skillPointsGained);
        SkillSyncEvents.markDirty(player);

        int interval = Math.max(1, AbilityConfig.pointIntervalLevels());
        int oldThresholds = Math.max(0, event.getOldLevel()) / interval;
        int newThresholds = Math.max(0, event.getNewLevel()) / interval;
        int abilityPointsGained = Math.max(0, newThresholds - oldThresholds);

        SkillsNetwork.sendLevelUpToast(player, event.getOldLevel(), event.getNewLevel(), skillPointsGained, abilityPointsGained);
    }
}
