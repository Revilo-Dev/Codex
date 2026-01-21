// src/main/java/net/revilodev/codex/network/CodexNetwork.java
package net.revilodev.codex.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.skills.SkillId;
import net.revilodev.codex.skills.SkillsAttachments;
import net.revilodev.codex.skills.logic.SkillLogic;

public final class CodexNetwork {
    private CodexNetwork() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(CodexNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent e) {
        PayloadRegistrar r = e.registrar("1");
        r.playToServer(Upgrade.TYPE, Upgrade.STREAM_CODEC, CodexNetwork::handleUpgrade);
        r.playToServer(Downgrade.TYPE, Downgrade.STREAM_CODEC, CodexNetwork::handleDowngrade);
    }

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

    public record Upgrade(SkillId skill) implements CustomPacketPayload {
        public static final Type<Upgrade> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skill_upgrade"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Upgrade> STREAM_CODEC = StreamCodec.of(
                (buf, msg) -> buf.writeVarInt(msg.skill.ordinal()),
                buf -> new Upgrade(SkillId.values()[buf.readVarInt()])
        );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Downgrade(SkillId skill) implements CustomPacketPayload {
        public static final Type<Downgrade> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skill_downgrade"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Downgrade> STREAM_CODEC = StreamCodec.of(
                (buf, msg) -> buf.writeVarInt(msg.skill.ordinal()),
                buf -> new Downgrade(SkillId.values()[buf.readVarInt()])
        );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
