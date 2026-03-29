package net.revilodev.codex.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.client.skills.SkillsPanelClient;
import net.revilodev.codex.client.toast.LevelUpToast;

@Mod(value = CodexMod.MOD_ID, dist = Dist.CLIENT)
public final class CodexClientMod {
    public CodexClientMod(IEventBus modBus) {
        SkillsPanelClient.register();
        NeoForge.EVENT_BUS.addListener(LevelUpToast::render);
    }
}
