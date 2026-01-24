// src/main/java/net/revilodev/codex/skills/SkillsNetwork.java
package net.revilodev.codex.skills;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.skills.logic.SkillLogic;

public final class SkillsNetwork {
    private SkillsNetwork() {}

    private static final String VERSION = "1";
    private static boolean REGISTERED = false;

    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        if (REGISTERED) return;
        REGISTERED = true;

        PayloadRegistrar r = event.registrar(CodexMod.MOD_ID).versioned(VERSION);

        // server -> client
        r.playToClient(SkillsSyncPayload.TYPE, SkillsSyncPayload.STREAM_CODEC, SkillsNetwork::handleSync);
        r.playToClient(OpenSkillsBookPayload.TYPE, OpenSkillsBookPayload.STREAM_CODEC, SkillsNetwork::handleOpenSkillsBook);

        // client -> server
        r.playToServer(SkillActionPayload.TYPE, SkillActionPayload.STREAM_CODEC, SkillsNetwork::handleAction);
    }

    public static void syncTo(ServerPlayer player) {
        PlayerSkills skills = player.getData(SkillsAttachments.PLAYER_SKILLS.get());
        CompoundTag tag = skills.serializeNBT(player.level().registryAccess());
        PacketDistributor.sendToPlayer(player, new SkillsSyncPayload(tag));
    }

    public static void sendOpenSkillsBook(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new OpenSkillsBookPayload());
    }

    private static void handleSync(SkillsSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() == null) return;

            CompoundTag tag = payload.data();
            if (tag == null) tag = new CompoundTag();

            ctx.player().getData(SkillsAttachments.PLAYER_SKILLS.get())
                    .deserializeNBT(ctx.player().level().registryAccess(), tag);
        });
    }

    private static void handleOpenSkillsBook(OpenSkillsBookPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() != null && ctx.player().level().isClientSide()) {
                ClientOnly.openSkillsBook();
            }
        });
    }

    private static void handleAction(SkillActionPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;

            SkillId[] vals = SkillId.values();
            int ord = payload.skillOrdinal();
            if (ord < 0 || ord >= vals.length) return;

            SkillId id = vals[ord];

            PlayerSkills skills = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
            boolean changed = payload.upgrade()
                    ? SkillLogic.tryUpgrade(skills, id)
                    : SkillLogic.tryDowngrade(skills, id);

            if (!changed) return;

            SkillLogic.applyAllEffects(sp, skills);
            syncTo(sp);
        });
    }

    public record SkillsSyncPayload(CompoundTag data) implements CustomPacketPayload {
        public static final Type<SkillsSyncPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skills_sync"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SkillsSyncPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, msg) -> buf.writeNbt(msg.data),
                        buf -> {
                            CompoundTag tag = buf.readNbt();
                            return new SkillsSyncPayload(tag == null ? new CompoundTag() : tag);
                        }
                );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // Used by SkillDetailsPanel (client -> server)
    public record SkillActionPayload(int skillOrdinal, boolean upgrade) implements CustomPacketPayload {
        public static final Type<SkillActionPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "skill_action"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SkillActionPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, msg) -> {
                            buf.writeVarInt(msg.skillOrdinal);
                            buf.writeBoolean(msg.upgrade);
                        },
                        buf -> new SkillActionPayload(buf.readVarInt(), buf.readBoolean())
                );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // Used by SkillsBookItem (server -> client)
    public record OpenSkillsBookPayload() implements CustomPacketPayload {
        public static final Type<OpenSkillsBookPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "open_skills_book"));

        public static final StreamCodec<RegistryFriendlyByteBuf, OpenSkillsBookPayload> STREAM_CODEC =
                StreamCodec.of((buf, msg) -> {}, buf -> new OpenSkillsBookPayload());

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientOnly {
        private static void openSkillsBook() {
            net.minecraft.client.Minecraft.getInstance()
                    .setScreen(new net.revilodev.codex.client.screen.StandaloneSkillsBookScreen());
        }
    }
}
