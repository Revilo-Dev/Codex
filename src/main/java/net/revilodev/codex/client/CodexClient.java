package net.revilodev.codex.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;

@OnlyIn(Dist.CLIENT)
public final class CodexClient {
    private CodexClient() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener(CodexPanelClient::onScreenInit);
        NeoForge.EVENT_BUS.addListener(CodexPanelClient::onScreenClosing);
        NeoForge.EVENT_BUS.addListener(CodexPanelClient::onScreenRenderPre);
        NeoForge.EVENT_BUS.addListener(CodexPanelClient::onScreenRenderPost);
        NeoForge.EVENT_BUS.addListener(CodexPanelClient::onMouseScrolled);
    }
}
