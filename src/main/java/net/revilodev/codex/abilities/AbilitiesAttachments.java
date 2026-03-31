package net.revilodev.codex.abilities;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.revilodev.codex.CodexMod;

import java.util.function.Supplier;

public final class AbilitiesAttachments {
    public static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CodexMod.MOD_ID);

    public static final Supplier<AttachmentType<PlayerAbilities>> PLAYER_ABILITIES =
            REGISTER.register("player_abilities", () -> AttachmentType.serializable(PlayerAbilities::new).copyOnDeath().build());

    private AbilitiesAttachments() {}
}
