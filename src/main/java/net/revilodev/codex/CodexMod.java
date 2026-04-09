package net.revilodev.codex;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.revilodev.codex.abilities.AbilitiesAttachments;
import net.revilodev.codex.abilities.AbilitiesEvents;
import net.revilodev.codex.abilities.AbilitiesNetwork;
import net.revilodev.codex.abilities.AbilityConfig;
import net.revilodev.codex.attributes.CodexAttributes;
import net.revilodev.codex.effect.CodexMobEffects;
import net.revilodev.codex.effect.CodexPotions;
import net.revilodev.codex.item.ModItems;
import net.revilodev.codex.skills.SkillsAttachments;
import net.revilodev.codex.skills.SkillConfig;
import net.revilodev.codex.skills.SkillsEvents;
import net.revilodev.codex.skills.SkillsNetwork;
import net.revilodev.codex.stats.CodexStats;

@Mod(CodexMod.MOD_ID)
public final class CodexMod {
    public static final String MOD_ID = "codex";

    public CodexMod(IEventBus modBus, ModContainer container) {
        ModItems.register(modBus);                 // <-- REQUIRED
        CodexAttributes.register(modBus);
        CodexMobEffects.register(modBus);
        CodexPotions.register(modBus);
        CodexStats.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, SkillConfig.SPEC);
        container.registerConfig(ModConfig.Type.SERVER, AbilityConfig.SPEC, MOD_ID + "-abilities-server.toml");

        SkillsAttachments.REGISTER.register(modBus);
        AbilitiesAttachments.REGISTER.register(modBus);
        modBus.addListener(SkillsNetwork::onRegisterPayloadHandlers);
        modBus.addListener(AbilitiesNetwork::onRegisterPayloadHandlers);
        net.revilodev.codex.skills.command.SkillsCommands.register();
        net.revilodev.codex.abilities.command.AbilitiesCommands.register();

        SkillsEvents.register();
        AbilitiesEvents.register();
    }
}
