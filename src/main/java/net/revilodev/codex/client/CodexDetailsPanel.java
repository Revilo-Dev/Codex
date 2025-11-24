package net.revilodev.codex.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.codex.data.GuideData;

@OnlyIn(Dist.CLIENT)
public final class CodexDetailsPanel extends AbstractWidget {
    private static final int BOTTOM_PADDING = 28;

    private final Minecraft mc = Minecraft.getInstance();
    private GuideData.Chapter chapter;

    private final BackButton back;
    private final Runnable onBack;

    private float scrollY = 0f;
    private int measuredContentHeight = 0;

    public CodexDetailsPanel(int x, int y, int w, int h, Runnable onBack) {
        super(x, y, w, h, Component.empty());
        this.onBack = onBack;

        this.back = new BackButton(getX(), getY(), () -> {
            if (this.onBack != null) this.onBack.run();
        });
        this.back.visible = false;
        this.back.active = false;
    }

    public AbstractButton backButton() {
        return back;
    }

    public void setBounds(int x, int y, int w, int h) {
        this.setX(x);
        this.setY(y);
        this.width = w;
        this.height = h;

        int cy = y + h - back.getHeight() - 4;
        back.setPosition(x + 2, cy);
    }

    public void setChapter(GuideData.Chapter chapter) {
        this.chapter = chapter;
        this.scrollY = 0f;
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!this.visible || chapter == null) return;

        int x = this.getX();
        int y = this.getY();
        int w = this.width;

        int contentTop = y + 4;
        int contentBottom = back.getY() - 6;
        int viewportH = Math.max(0, contentBottom - contentTop);

        measuredContentHeight = measureContentHeight(w);
        int maxScroll = Math.max(0, measuredContentHeight + BOTTOM_PADDING - viewportH);
        scrollY = Mth.clamp(scrollY, 0f, maxScroll);

        gg.enableScissor(x, contentTop, x + w, contentBottom);

        int[] curY = {contentTop + 4 - Mth.floor(scrollY)};

        // Icon + title
        int nameWidth = w - 32;
        chapter.iconItem().ifPresent(item ->
                gg.renderItem(new ItemStack(item), x + 4, curY[0]));
        gg.drawWordWrap(mc.font, Component.literal(chapter.name),
                x + 26, curY[0] + 2, nameWidth, 0xFFFFFF);
        curY[0] += mc.font.wordWrapHeight(chapter.name, nameWidth) + 12;

        // Description
        if (!chapter.description.isBlank()) {
            gg.drawWordWrap(mc.font, Component.literal(chapter.description),
                    x + 4, curY[0], w - 8, 0xCFCFCF);
            curY[0] += mc.font.wordWrapHeight(chapter.description, w - 8) + 8;
        }

        // Optional image at the bottom
        chapter.imageLocation().ifPresent(rl -> {
            int imgW = w - 8;
            int imgH = imgW / 2; // simple 2:1 aspect
            gg.blit(rl, x + 4, curY[0], 0, 0, imgW, imgH, imgW, imgH);
            curY[0] += imgH + 4;
        });

        gg.disableScissor();
    }

    private int measureContentHeight(int panelWidth) {
        if (chapter == null) return 0;
        int w = panelWidth;
        int y = 4;

        y += mc.font.wordWrapHeight(chapter.name, w - 32) + 12;
        if (!chapter.description.isBlank())
            y += mc.font.wordWrapHeight(chapter.description, w - 8) + 8;

        if (chapter.imageLocation().isPresent()) {
            int imgW = w - 8;
            int imgH = imgW / 2;
            y += imgH + 4;
        }

        return y;
    }

    // ------------------------------------------------------------
    // Scrolling
    // ------------------------------------------------------------

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!this.visible || !this.active) return false;
        int contentTop = this.getY() + 4;
        int contentBottom = back.getY() - 6;
        if (mouseX < this.getX() || mouseX > this.getX() + this.width) return false;
        if (mouseY < contentTop || mouseY > contentBottom) return false;
        int viewportH = Math.max(0, contentBottom - contentTop);
        int maxScroll = Math.max(0, measuredContentHeight + BOTTOM_PADDING - viewportH);
        if (maxScroll <= 0) return false;
        scrollY = Mth.clamp(scrollY - (float) (delta * 12), 0f, maxScroll);
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        return mouseScrolled(mouseX, mouseY, deltaY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
    }

    // ------------------------------------------------------------
    // Back button
    // ------------------------------------------------------------

    private static final class BackButton extends AbstractButton {
        private static final ResourceLocation TEX_NORMAL =
                ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_back_button.png");
        private static final ResourceLocation TEX_HOVER =
                ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_back_highlighted.png");
        private final Runnable onPress;

        public BackButton(int x, int y, Runnable onPress) {
            super(x, y, 24, 20, Component.empty());
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isMouseOver(mouseX, mouseY);
            ResourceLocation tex = hovered ? TEX_HOVER : TEX_NORMAL;
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }
}
