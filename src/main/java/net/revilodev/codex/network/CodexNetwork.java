// src/main/java/net/revilodev/codex/network/CodexNetwork.java
package net.revilodev.codex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
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
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.client.screen.StandaloneSkillsBookScreen;
import net.revilodev.codex.skills.SkillId;
import net.revilodev.codex.skills.SkillsAttachments;
import net.revilodev.codex.skills.logic.SkillLogic;

public final class CodexNetwork {

    private static final String VERSION = "1";
    private static boolean REGISTERED = false;

    private CodexNetwork() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(CodexNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent e) {
        if (REGISTERED) return;
        REGISTERED = true;

        // IMPORTANT: registrar id must be your MOD_ID (NOT "1")
        PayloadRegistrar r = e.registrar(CodexMod.MOD_ID).versioned(VERSION);

        // client -> server
        r.playToServer(Upgrade.TYPE, Upgrade.STREAM_CODEC, CodexNetwork::handleUpgrade);
        r.playToServer(Downgrade.TYPE, Downgrade.STREAM_CODEC, CodexNetwork::handleDowngrade);

        // server -> client
        r.playToClient(OpenSkillsBook.TYPE, OpenSkillsBook.STREAM_CODEC, CodexNetwork::handleOpenSkillsBook);
    }

    // --------------------
    // SERVER -> CLIENT
    // --------------------

    public static void sendOpenSkillsBook(ServerPlayer sp) {
        PacketDistributor.sendToPlayer(sp, new OpenSkillsBook());
    }

    private static void handleOpenSkillsBook(OpenSkillsBook msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() != null && ctx.player().level().isClientSide()) {
                ClientOnly.openSkillsBook();
            }
        });
    }

    // --------------------
    // CLIENT -> SERVER
    // --------------------

    private static void handleUpgrade(Upgrade msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            var type = SkillsAttachments.PLAYER_SKILLS.get();
            var data = sp.getData(type);
            if (SkillLogic.tryUpgrade(data, msg.skill)) sp.syncData(type);
        });
    }

    private static void handleDowngrade(Downgrade msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            var type = SkillsAttachments.PLAYER_SKILLS.get();
            var data = sp.getData(type);
            if (SkillLogic.tryDowngrade(data, msg.skill)) sp.syncData(type);
        });
    }

    // --------------------
    // PAYLOADS
    // --------------------

    public record OpenSkillsBook() implements CustomPacketPayload {
        public static final Type<OpenSkillsBook> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "open_skills_book"));

        public static final StreamCodec<RegistryFriendlyByteBuf, OpenSkillsBook> STREAM_CODEC =
                StreamCodec.unit(new OpenSkillsBook());

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Upgrade(SkillId skill) implements CustomPacketPayload {
        public static final Type<Upgrade> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skill_upgrade"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Upgrade> STREAM_CODEC = StreamCodec.of(
                (buf, msg) -> buf.writeVarInt(msg.skill.ordinal()),
                buf -> new Upgrade(SkillId.values()[buf.readVarInt()])
        );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Downgrade(SkillId skill) implements CustomPacketPayload {
        public static final Type<Downgrade> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skill_downgrade"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Downgrade> STREAM_CODEC = StreamCodec.of(
                (buf, msg) -> buf.writeVarInt(msg.skill.ordinal()),
                buf -> new Downgrade(SkillId.values()[buf.readVarInt()])
        );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientOnly {
        private static void openSkillsBook() {
            net.minecraft.client.Minecraft.getInstance().setScreen(new StandaloneSkillsBookScreen());
        }
    }
}
