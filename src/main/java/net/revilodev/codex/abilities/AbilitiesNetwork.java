package net.revilodev.codex.abilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
import net.revilodev.codex.abilities.logic.AbilityLogic;
import net.revilodev.codex.abilities.logic.AbilitySyncEvents;
import net.revilodev.codex.client.toast.SkillPointToast;

public final class AbilitiesNetwork {
    private static final String VERSION = "1";
    private static boolean REGISTERED = false;

    private AbilitiesNetwork() {}

    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        if (REGISTERED) return;
        REGISTERED = true;

        PayloadRegistrar registrar = event.registrar(CodexMod.MOD_ID).versioned(VERSION);
        registrar.playToClient(AbilitiesSyncPayload.TYPE, AbilitiesSyncPayload.STREAM_CODEC, AbilitiesNetwork::handleSync);
        registrar.playToClient(AbilityPointToastPayload.TYPE, AbilityPointToastPayload.STREAM_CODEC, AbilitiesNetwork::handleToast);
        registrar.playToServer(AbilityActionPayload.TYPE, AbilityActionPayload.STREAM_CODEC, AbilitiesNetwork::handleAction);
        registrar.playToServer(AbilityUsePayload.TYPE, AbilityUsePayload.STREAM_CODEC, AbilitiesNetwork::handleUse);
    }

    public static void syncTo(ServerPlayer player) {
        PlayerAbilities abilities = player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
        PacketDistributor.sendToPlayer(player, new AbilitiesSyncPayload(abilities.serializeNBT(player.level().registryAccess())));
    }

    public static void sendAbilityPointToast(ServerPlayer player, int delta, int total) {
        PacketDistributor.sendToPlayer(player, new AbilityPointToastPayload(delta, total));
    }

    private static void handleSync(AbilitiesSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() == null) return;
            PlayerAbilities abilities = ctx.player().getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
            CompoundTag tag = payload.data();
            if (tag == null) tag = new CompoundTag();
            abilities.deserializeNBT(ctx.player().level().registryAccess(), tag);
        });
    }

    private static void handleToast(AbilityPointToastPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() != null && ctx.player().level().isClientSide()) {
                ClientOnly.showAbilityPointToast(payload.delta(), payload.total());
            }
        });
    }

    private static void handleAction(AbilityActionPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            PlayerAbilities abilities = player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
            AbilityId id = AbilityId.byOrdinal(payload.abilityOrdinal());
            boolean changed = switch (payload.action()) {
                case 0 -> abilities.tryUpgrade(id);
                case 1 -> abilities.tryDowngrade(id);
                case 2 -> abilities.assign(payload.slot(), id);
                case 3 -> abilities.clearAssignmentFor(id);
                default -> false;
            };
            if (!changed) return;
            AbilitySyncEvents.markDirty(player);
        });
    }

    private static void handleUse(AbilityUsePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (AbilityLogic.tryActivate(player, payload.slot())) {
                AbilitySyncEvents.markDirty(player);
            }
        });
    }

    public record AbilitiesSyncPayload(CompoundTag data) implements CustomPacketPayload {
        public static final Type<AbilitiesSyncPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "abilities_sync"));

        public static final StreamCodec<RegistryFriendlyByteBuf, AbilitiesSyncPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, msg) -> buf.writeNbt(msg.data),
                        buf -> {
                            CompoundTag tag = buf.readNbt();
                            return new AbilitiesSyncPayload(tag == null ? new CompoundTag() : tag);
                        }
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AbilityActionPayload(int action, int abilityOrdinal, int slot) implements CustomPacketPayload {
        public static final Type<AbilityActionPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "ability_action"));

        public static final StreamCodec<RegistryFriendlyByteBuf, AbilityActionPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, msg) -> {
                            buf.writeVarInt(msg.action);
                            buf.writeVarInt(msg.abilityOrdinal);
                            buf.writeVarInt(msg.slot);
                        },
                        buf -> new AbilityActionPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt())
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AbilityUsePayload(int slot) implements CustomPacketPayload {
        public static final Type<AbilityUsePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "ability_use"));

        public static final StreamCodec<RegistryFriendlyByteBuf, AbilityUsePayload> STREAM_CODEC =
                StreamCodec.of((buf, msg) -> buf.writeVarInt(msg.slot), buf -> new AbilityUsePayload(buf.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AbilityPointToastPayload(int delta, int total) implements CustomPacketPayload {
        public static final Type<AbilityPointToastPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "ability_points_toast"));

        public static final StreamCodec<RegistryFriendlyByteBuf, AbilityPointToastPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, msg) -> {
                            buf.writeVarInt(msg.delta);
                            buf.writeVarInt(msg.total);
                        },
                        buf -> new AbilityPointToastPayload(buf.readVarInt(), buf.readVarInt())
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientOnly {
        private static void showAbilityPointToast(int delta, int total) {
            SkillPointToast.showGlobal(delta, total, Component.literal("Ability point earned"));
        }
    }
}
