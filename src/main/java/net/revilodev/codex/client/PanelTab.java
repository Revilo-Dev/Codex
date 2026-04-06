package net.revilodev.codex.client;

import net.minecraft.resources.ResourceLocation;
import net.revilodev.codex.CodexMod;

public enum PanelTab {
    SKILLS("Skills", ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/skills.png")),
    ABILITIES("Abilities", ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/abilities.png"));

    private final String title;
    private final ResourceLocation iconTexture;

    PanelTab(String title, ResourceLocation iconTexture) {
        this.title = title;
        this.iconTexture = iconTexture;
    }

    public String title() {
        return title;
    }

    public ResourceLocation iconTexture() {
        return iconTexture;
    }
}
