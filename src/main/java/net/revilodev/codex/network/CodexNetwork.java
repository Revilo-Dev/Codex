package net.revilodev.codex.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.revilodev.codex.data.GuideData;

public final class CodexNetwork {

    private static final String CHANNEL = "codex";
    private static final String VERSION = "1";
    private static boolean REGISTERED = false;

    private static final Gson GSON = new GsonBuilder().setLenient().create();

    private CodexNetwork() {}

    public static void bootstrap(IEventBus bus) {
        bus.addListener(CodexNetwork::register);
    }

    private static void register(RegisterPayloadHandlersEvent event) {
        if (REGISTERED) return;
        REGISTERED = true;

        PayloadRegistrar r = event.registrar(CHANNEL).versioned(VERSION);

        r.playToClient(SyncGuides.TYPE, SyncGuides.CODEC, CodexNetwork::handleSyncGuides);
        r.playToClient(OpenCodexBook.TYPE, OpenCodexBook.CODEC, CodexNetwork::handleOpenCodexBook);
    }

    public record SyncGuides(String json) implements CustomPacketPayload {
        public static final Type<SyncGuides> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("codex", "sync_guides"));
        public static final StreamCodec<FriendlyByteBuf, SyncGuides> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeUtf(p.json),
                buf -> new SyncGuides(buf.readUtf())
        );
        @Override public Type<SyncGuides> type() { return TYPE; }
    }

    public record OpenCodexBook() implements CustomPacketPayload {
        public static final Type<OpenCodexBook> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("codex", "open_codex_book"));
        public static final StreamCodec<FriendlyByteBuf, OpenCodexBook> CODEC =
                StreamCodec.of((buf, p) -> {}, buf -> new OpenCodexBook());
        @Override public Type<OpenCodexBook> type() { return TYPE; }
    }

    public static void syncPlayer(ServerPlayer p) {
        sendGuideData(p);
    }

    private static void sendGuideData(ServerPlayer p) {
        var chapters = GuideData.allServer(p.server);
        var categories = GuideData.categoriesOrderedServer(p.server);

        JsonObject root = new JsonObject();

        JsonArray cats = new JsonArray();
        for (GuideData.Category c : categories) {
            JsonObject o = new JsonObject();
            o.addProperty("id", c.id);
            o.addProperty("icon", c.icon);
            o.addProperty("name", c.name);
            o.addProperty("order", c.order);
            o.addProperty("excludeFromAll", c.excludeFromAll);
            o.addProperty("dependency", c.dependency);
            cats.add(o);
        }
        root.add("categories", cats);

        JsonArray chs = new JsonArray();
        for (GuideData.Chapter c : chapters) {
            JsonObject o = new JsonObject();
            o.addProperty("id", c.id);
            o.addProperty("name", c.name);
            o.addProperty("icon", c.icon);
            o.addProperty("description", c.description);
            o.addProperty("image", c.image);
            o.addProperty("category", c.category);
            chs.add(o);
        }
        root.add("chapters", chs);

        String json = GSON.toJson(root);
        PacketDistributor.sendToPlayer(p, new SyncGuides(json));
    }

    public static void sendOpenCodexBook(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new OpenCodexBook());
    }

    private static void handleSyncGuides(SyncGuides p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> GuideData.applyNetworkJson(p.json()));
    }

    private static void handleOpenCodexBook(OpenCodexBook p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player().level().isClientSide()) {
                ClientOnly.openCodexBook();
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientOnly {
        private static void openCodexBook() {
            net.minecraft.client.Minecraft.getInstance()
                    .setScreen(new net.revilodev.codex.client.screen.CodexBookScreen());
        }
    }
}
