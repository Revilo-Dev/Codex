package net.revilodev.codex;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.revilodev.codex.network.CodexNetwork;
import net.revilodev.codex.skills.SkillsAttachments;
import net.revilodev.codex.skills.logic.SkillEvents;
import net.revilodev.codex.skills.logic.SkillSyncEvents;

@Mod(CodexMod.MOD_ID)
public final class CodexMod {
    public static final String MOD_ID = "codex";

    public CodexMod(IEventBus modBus, ModContainer container) {
        SkillsAttachments.REGISTER.register(modBus);

        CodexNetwork.register(modBus);

        SkillEvents.register();
        SkillSyncEvents.register();
    }
}
