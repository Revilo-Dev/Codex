package net.revilodev.codex.skills.logic;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.skills.SkillsAttachments;
import net.revilodev.codex.skills.SkillsNetwork;

public final class SkillSyncEvents {
    private SkillSyncEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SkillSyncEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(SkillSyncEvents::onRespawn);
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerSkills skills = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
        SkillLogic.applyAllEffects(sp, skills);
        SkillsNetwork.syncTo(sp);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerSkills skills = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
        SkillLogic.applyAllEffects(sp, skills);
        SkillsNetwork.syncTo(sp);
    }
}
