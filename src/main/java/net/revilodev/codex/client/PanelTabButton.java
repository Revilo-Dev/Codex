package net.revilodev.codex.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.codex.CodexMod;

@OnlyIn(Dist.CLIENT)
public final class PanelTabButton extends AbstractButton {
    private static final ResourceLocation TAB =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/tab.png");
    private static final ResourceLocation TAB_SELECTED =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/tab_selected.png");

    private final PanelTab tab;
    private final Runnable onPress;
    private boolean selected;

    public PanelTabButton(int x, int y, PanelTab tab, Runnable onPress) {
        super(x, y, 35, 27, Component.literal(tab.title()));
        this.tab = tab;
        this.onPress = onPress;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void onPress() {
        if (onPress != null) onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        ResourceLocation texture = selected ? TAB_SELECTED : TAB;
        gg.blit(texture, getX(), getY(), 0, 0, width, height, width, height);
        gg.renderItem(new ItemStack(tab.icon()), getX() + 9, getY() + 5);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {}
}
