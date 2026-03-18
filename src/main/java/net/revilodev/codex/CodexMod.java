package net.revilodev.codex;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.revilodev.codex.item.ModItems;
import net.revilodev.codex.skills.SkillsAttachments;
import net.revilodev.codex.skills.SkillConfig;
import net.revilodev.codex.skills.SkillsEvents;
import net.revilodev.codex.skills.SkillsNetwork;

@Mod(CodexMod.MOD_ID)
public final class CodexMod {
    public static final String MOD_ID = "codex";

    public CodexMod(IEventBus modBus, ModContainer container) {
        ModItems.register(modBus);                 // <-- REQUIRED
        container.registerConfig(ModConfig.Type.SERVER, SkillConfig.SPEC);

        SkillsAttachments.REGISTER.register(modBus);
        modBus.addListener(SkillsNetwork::onRegisterPayloadHandlers);
        net.revilodev.codex.skills.command.SkillsCommands.register();

        SkillsEvents.register();
    }
}
