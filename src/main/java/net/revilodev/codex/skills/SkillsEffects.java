package net.revilodev.codex.skills;

import net.minecraft.server.level.ServerPlayer;
import net.revilodev.codex.skills.logic.SkillLogic;

public final class SkillsEffects {
    private SkillsEffects() {}

    public static void applyServerTickEffects(ServerPlayer player, PlayerSkills skills) {
        SkillLogic.applyAllEffects(player, skills);
    }
}
