package net.revilodev.codex.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class CodexNetwork {

    private CodexNetwork() {}

    public record OpenCodexBook() implements CustomPacketPayload {
        public static final Type<OpenCodexBook> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("codex", "open_codex_book"));

        public static final StreamCodec<FriendlyByteBuf, OpenCodexBook> CODEC =
                StreamCodec.of((buf, p) -> {}, buf -> new OpenCodexBook());

        @Override
        public Type<OpenCodexBook> type() {
            return TYPE;
        }
    }
}
