package net.revilodev.codex.skills;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.skills.logic.SkillLogic;

public final class SkillsNetwork {
    private SkillsNetwork() {}

    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(CodexMod.MOD_ID).versioned("1.0.0");
        registrar.playToClient(SkillsSyncPayload.TYPE, SkillsSyncPayload.STREAM_CODEC, SkillsNetwork::handleSync);
        registrar.playToServer(SkillActionPayload.TYPE, SkillActionPayload.STREAM_CODEC, SkillsNetwork::handleAction);
    }

    public static void syncTo(ServerPlayer player) {
        PlayerSkills skills = player.getData(SkillsAttachments.PLAYER_SKILLS.get());
        CompoundTag tag = skills.serializeNBT(player.level().registryAccess());
        PacketDistributor.sendToPlayer(player, new SkillsSyncPayload(tag));
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

    private static void handleAction(SkillActionPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;

            SkillId id = SkillId.byOrdinal(payload.skillOrdinal());
            if (id == null) return;

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

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

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

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
