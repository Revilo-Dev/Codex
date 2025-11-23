package net.revilodev.codex;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.revilodev.codex.client.ClientQuestEvents;
import net.revilodev.codex.client.QuestPanelClient;
import net.revilodev.codex.command.BoundlessCommands;
import net.revilodev.codex.item.ModItems;
import net.revilodev.codex.network.CodexNetwork;
import net.revilodev.codex.quest.KillCounterState;
import net.revilodev.codex.quest.QuestData;
import net.revilodev.codex.quest.QuestEvents;
import org.slf4j.Logger;

import java.util.List;

@Mod(CodexMod.MOD_ID)
public final class CodexMod {
    public static final String MOD_ID = "codex";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CodexMod(ModContainer modContainer, IEventBus modBus) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC, MOD_ID + "-common.toml");
        ModItems.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::addCreative);
        if (net.neoforged.fml.loading.FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(this::clientSetup);
        }
        CodexNetwork.bootstrap(modBus);
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(QuestEvents::onPlayerTick);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Codex common setup complete");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.addListener(QuestPanelClient::onScreenInit);
        NeoForge.EVENT_BUS.addListener(QuestPanelClient::onScreenClosing);
        NeoForge.EVENT_BUS.addListener(QuestPanelClient::onScreenRenderPost);
        NeoForge.EVENT_BUS.addListener(QuestPanelClient::onScreenRenderPre);
        NeoForge.EVENT_BUS.addListener(QuestPanelClient::onMouseScrolled);
        NeoForge.EVENT_BUS.addListener(ClientQuestEvents::onClientLogout);
        NeoForge.EVENT_BUS.addListener(ClientQuestEvents::onClientLevelUnload);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.GUID_BOOK.get());
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        BoundlessCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Codex server starting");
        QuestData.loadServer(event.getServer(), true);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            CodexNetwork.syncPlayer(sp);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel server)) return;
        ResourceLocation rl = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        if (rl == null) return;
        KillCounterState.get(server).inc(sp.getUUID(), rl.toString());
        int count = KillCounterState.get(server).get(sp.getUUID(), rl.toString());
        CodexNetwork.KillEntry entry = new CodexNetwork.KillEntry(rl.toString(), count);
        CodexNetwork.SyncKills payload = new CodexNetwork.SyncKills(List.of(entry));
        PacketDistributor.sendToPlayer(sp, payload);
    }
}
