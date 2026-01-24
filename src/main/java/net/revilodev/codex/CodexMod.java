package net.revilodev.codex;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.revilodev.codex.item.ModItems;
import net.revilodev.codex.skills.SkillsAttachments;
import net.revilodev.codex.skills.SkillsNetwork;
import net.revilodev.codex.skills.logic.SkillEvents;
import net.revilodev.codex.skills.logic.SkillSyncEvents;

@Mod(CodexMod.MOD_ID)
public final class CodexMod {
    public static final String MOD_ID = "codex";

    public CodexMod(IEventBus modBus, ModContainer container) {
        ModItems.register(modBus);                 // <-- REQUIRED

        SkillsAttachments.REGISTER.register(modBus);
        modBus.addListener(SkillsNetwork::onRegisterPayloadHandlers);
        net.revilodev.codex.skills.command.SkillsCommands.register();

        SkillEvents.register();
        SkillSyncEvents.register();
    }
}
