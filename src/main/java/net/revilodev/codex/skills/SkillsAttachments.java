package net.revilodev.codex.skills;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.revilodev.codex.CodexMod;

import java.util.function.Supplier;

public final class SkillsAttachments {
    private SkillsAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CodexMod.MOD_ID);

    public static final Supplier<AttachmentType<PlayerSkills>> PLAYER_SKILLS =
            REGISTER.register("player_skills", () -> AttachmentType.serializable(PlayerSkills::new).copyOnDeath().build());
}
