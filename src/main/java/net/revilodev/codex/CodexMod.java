package net.revilodev.codex;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.revilodev.codex.client.CodexPanelClient;
import net.revilodev.codex.item.ModItems;
import net.revilodev.codex.network.CodexNetwork;
import org.slf4j.Logger;

@Mod(CodexMod.MOD_ID)
public final class CodexMod {
    public static final String MOD_ID = "codex";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CodexMod(ModContainer modContainer, IEventBus modBus) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC, MOD_ID + "-common.toml");

        // Register items
        ModItems.register(modBus);

        // Lifecycle events
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::addCreative);

        // Client-only setup
        if (net.neoforged.fml.loading.FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(this::clientSetup);
        }

        // Network
        CodexNetwork.bootstrap(modBus);

        // ❌ MUST NOT REGISTER THE MOD CLASS HERE
        // NeoForge.EVENT_BUS.register(this);  <-- removed
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Codex common setup complete");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.addListener(CodexPanelClient::onScreenInit);
        NeoForge.EVENT_BUS.addListener(CodexPanelClient::onScreenClosing);
        NeoForge.EVENT_BUS.addListener(CodexPanelClient::onScreenRenderPost);
        NeoForge.EVENT_BUS.addListener(CodexPanelClient::onScreenRenderPre);
        NeoForge.EVENT_BUS.addListener(CodexPanelClient::onMouseScrolled);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.GUIDE_BOOK.get());
        }
    }
}
