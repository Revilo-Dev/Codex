package net.revilodev.codex.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.codex.Config;
import net.revilodev.codex.data.GuideData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class CodexListWidget extends AbstractWidget {
    private static final ResourceLocation ROW_TEX =
            ResourceLocation.fromNamespaceAndPath("codex", "textures/gui/sprites/quest_widget.png");
    private static final ResourceLocation ROW_TEX_DISABLED =
            ResourceLocation.fromNamespaceAndPath("codex", "textures/gui/sprites/quest_widget_disabled.png");

    private final Minecraft mc;
    private final List<GuideData.Chapter> chapters = new ArrayList<>();
    private final Consumer<GuideData.Chapter> onClick;

    private float scrollY = 0f;
    private final int rowHeight = 27;
    private final int rowPad = 2;

    private String category = "all";

    public CodexListWidget(int x, int y, int width, int height, Consumer<GuideData.Chapter> onClick) {
        super(x, y, width, height, Component.empty());
        this.mc = Minecraft.getInstance();
        this.onClick = onClick;
    }

    public void setChapters(Iterable<GuideData.Chapter> all) {
        chapters.clear();
        for (GuideData.Chapter c : all) chapters.add(c);
        scrollY = 0f;
    }

    public void setCategory(String categoryId) {
        this.category = categoryId == null ? "all" : categoryId;
        scrollY = 0f;
    }

    public void setBounds(int x, int y, int w, int h) {
        this.setX(x);
        this.setY(y);
        this.width = w;
        this.height = h;
    }

    private boolean categoryUnlocked(String catId) {
        var c = GuideData.categoryById(catId).orElse(null);
        if (mc.player == null) return true;
        return GuideData.isCategoryUnlocked(c, mc.player);
    }

    private boolean includeInAll(GuideData.Chapter c) {
        if (mc.player == null) return true;
        return GuideData.includeChapterInAll(c, mc.player);
    }

    private boolean matchesCategory(GuideData.Chapter c) {
        if (Config.disabledCategories().contains(c.category)) return false;
        if ("all".equalsIgnoreCase(category)) return includeInAll(c);
        if (!c.category.equalsIgnoreCase(category)) return false;
        return categoryUnlocked(c.category);
    }

    private int contentHeight() {
        if (mc.player == null) return 0;
        int visible = 0;
        for (GuideData.Chapter c : chapters) {
            if (!matchesCategory(c)) continue;
            visible++;
        }
        return visible * (rowHeight + rowPad);
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;
        RenderSystem.enableBlend();
        gg.enableScissor(getX(), getY(), getX() + width, getY() + height);
        int yOff = this.getY() - Mth.floor(scrollY);
        int drawn = 0;

        for (GuideData.Chapter c : chapters) {
            if (mc.player == null) continue;
            if (!matchesCategory(c)) continue;

            int top = yOff + drawn * (rowHeight + rowPad);
            drawn++;
            if (top > this.getY() + this.height) break;
            if (top + rowHeight < this.getY()) continue;

            boolean deps = true;
            ResourceLocation rowTex = deps ? ROW_TEX : ROW_TEX_DISABLED;
            gg.blit(rowTex, this.getX(), top, 0, 0, 127, 27, 127, 27);

            Item iconItem = c.iconItem().orElse(null);
            if (iconItem != null) {
                gg.renderItem(new ItemStack(iconItem), this.getX() + 6, top + 5);
            }

            String name = c.name;
            int maxWidth = this.width - 42;
            int nameWidth = mc.font.width(name);
            int color = deps ? 0xFFFFFF : 0xA0A0A0;

            if (nameWidth > maxWidth) {
                String trimmed = mc.font.plainSubstrByWidth(name, maxWidth - mc.font.width("...")) + "...";
                gg.drawString(mc.font, trimmed, this.getX() + 30, top + 9, color, false);
            } else {
                gg.drawString(mc.font, name, this.getX() + 30, top + 9, color, false);
            }
        }

        gg.disableScissor();

        int content = contentHeight();
        if (content > this.height) {
            float ratio = (float) this.height / content;
            int barHeight = Math.max(10, (int) (this.height * ratio));
            int barY = getY() + (int) ((this.height - barHeight) * (scrollY / (content - this.height)));
            gg.fill(getX() + width + 4, barY, getX() + width + 6, barY + barHeight, 0xFF808080);
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!this.visible || !this.active) return false;
        int content = contentHeight();
        int view = this.height;
        if (content <= view) return false;
        float max = content - view;
        scrollY = Mth.clamp(scrollY - (float) (delta * 12), 0f, max);
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        return mouseScrolled(mouseX, mouseY, deltaY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.active || !this.isMouseOver(mouseX, mouseY)) return false;
        if (button != 0) return false;
        if (mc.player == null) return false;
        int localY = (int) (mouseY - this.getY() + scrollY);
        int idx = localY / (rowHeight + rowPad);
        int visibleIndex = 0;

        for (GuideData.Chapter c : chapters) {
            if (!matchesCategory(c)) continue;
            if (visibleIndex == idx) {
                if (onClick != null) onClick.accept(c);
                return true;
            }
            visibleIndex++;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {}
}
