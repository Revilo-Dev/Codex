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
import net.revilodev.codex.data.GuideData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class CodexListWidget extends AbstractWidget {

    private static final ResourceLocation ROW_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_widget.png");

    private final Minecraft mc;

    private enum Mode { CATEGORIES, CHAPTERS }

    private Mode mode = Mode.CATEGORIES;

    private final List<ListEntry> entries = new ArrayList<>();

    private final Consumer<GuideData.Category> onCategoryClick;
    private final Consumer<GuideData.Chapter> onChapterClick;

    private float scrollY = 0f;
    private final int rowHeight = 27;
    private final int rowPad = 2;

    private static sealed interface ListEntry permits CategoryEntry, ChapterEntry {}

    private static final class CategoryEntry implements ListEntry {
        final GuideData.Category category;
        CategoryEntry(GuideData.Category c) { this.category = c; }
    }

    private static final class ChapterEntry implements ListEntry {
        final GuideData.Chapter chapter;
        ChapterEntry(GuideData.Chapter c) { this.chapter = c; }
    }

    public CodexListWidget(int x, int y, int width, int height,
                           Consumer<GuideData.Category> onCategoryClick,
                           Consumer<GuideData.Chapter> onChapterClick) {
        super(x, y, width, height, Component.empty());
        this.mc = Minecraft.getInstance();
        this.onCategoryClick = onCategoryClick;
        this.onChapterClick = onChapterClick;
    }

    // ------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------

    public void showCategories(Iterable<GuideData.Category> categories) {
        entries.clear();
        mode = Mode.CATEGORIES;
        for (GuideData.Category c : categories) {
            entries.add(new CategoryEntry(c));
        }
        scrollY = 0f;
    }

    public void showChapters(Iterable<GuideData.Chapter> chapters) {
        entries.clear();
        mode = Mode.CHAPTERS;
        for (GuideData.Chapter c : chapters) {
            entries.add(new ChapterEntry(c));
        }
        scrollY = 0f;
    }

    public void setBounds(int x, int y, int w, int h) {
        this.setX(x);
        this.setY(y);
        this.width = w;
        this.height = h;
    }

    // ------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------

    private int contentHeight() {
        return entries.size() * (rowHeight + rowPad);
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;

        RenderSystem.enableBlend();
        gg.enableScissor(getX(), getY(), getX() + width, getY() + height);

        int yOff = this.getY() - Mth.floor(scrollY);
        int drawn = 0;

        for (ListEntry le : entries) {
            int top = yOff + drawn * (rowHeight + rowPad);
            drawn++;

            if (top > this.getY() + this.height) break;
            if (top + rowHeight < this.getY()) continue;

            gg.blit(ROW_TEX, this.getX(), top, 0, 0, 127, 27, 127, 27);

            Item iconItem = null;
            String label = "";

            if (le instanceof CategoryEntry ce) {
                iconItem = ce.category.iconItem().orElse(null);
                label = ce.category.name;
            } else if (le instanceof ChapterEntry che) {
                iconItem = che.chapter.iconItem().orElse(null);
                label = che.chapter.name;
            }

            if (iconItem != null) {
                gg.renderItem(new ItemStack(iconItem), this.getX() + 6, top + 5);
            }

            int maxWidth = this.width - 42;
            int color = 0xFFFFFF;

            int nameWidth = mc.font.width(label);
            String toDraw = label;
            if (nameWidth > maxWidth) {
                toDraw = mc.font.plainSubstrByWidth(label, maxWidth - mc.font.width("...")) + "...";
            }

            gg.drawString(mc.font, toDraw, this.getX() + 30, top + 9, color, false);
        }

        gg.disableScissor();

        // Scroll bar
        int content = contentHeight();
        if (content > this.height) {
            float ratio = (float) this.height / content;
            int barHeight = Math.max(10, (int) (this.height * ratio));
            int barY = getY() + (int) ((this.height - barHeight) * (scrollY / (content - this.height)));
            gg.fill(getX() + width + 4, barY, getX() + width + 6, barY + barHeight, 0xFF808080);
        }
    }

    // ------------------------------------------------------------
    // Input
    // ------------------------------------------------------------

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!this.visible || !this.active) return false;
        int content = contentHeight();
        int view = this.height;
        if (content <= view) return false;
        float max = content - view;
        scrollY = Mth.clamp(scrollY - (float) (delta * 12), 0f, max);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        return mouseScrolled(mouseX, mouseY, deltaY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.active || !this.isMouseOver(mouseX, mouseY)) return false;
        if (button != 0) return false;

        int localY = (int) (mouseY - this.getY() + scrollY);
        int idx = localY / (rowHeight + rowPad);
        if (idx < 0 || idx >= entries.size()) return false;

        ListEntry le = entries.get(idx);
        if (le instanceof CategoryEntry ce) {
            if (onCategoryClick != null) onCategoryClick.accept(ce.category);
            return true;
        } else if (le instanceof ChapterEntry che) {
            if (onChapterClick != null) onChapterClick.accept(che.chapter);
            return true;
        }

        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
    }
}
