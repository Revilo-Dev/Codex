package net.revilodev.codex.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.client.skills.SkillsPanelClient;

@Mod(value = CodexMod.MOD_ID, dist = Dist.CLIENT)
public final class CodexClientMod {
    public CodexClientMod(IEventBus modBus) {
        SkillsPanelClient.register();
    }
}
