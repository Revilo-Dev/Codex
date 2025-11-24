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

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class CodexDetailsPanel extends AbstractWidget {
    private static final int BOTTOM_PADDING = 28;

    private final Minecraft mc = Minecraft.getInstance();
    private GuideData.Chapter chapter;

    private final BackButton back;
    private final CompleteButton complete;
    private final RejectButton reject;
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

        this.complete = new CompleteButton(getX(), getY(), () -> {
            if (this.onBack != null) this.onBack.run();
        });
        this.complete.visible = false;
        this.complete.active = false;

        this.reject = new RejectButton(getX(), getY(), () -> {
            if (this.onBack != null) this.onBack.run();
        });
        this.reject.visible = false;
        this.reject.active = false;
    }

    public AbstractButton backButton() {
        return back;
    }

    public AbstractButton completeButton() {
        return complete;
    }

    public AbstractButton rejectButton() {
        return reject;
    }

    public void setBounds(int x, int y, int w, int h) {
        this.setX(x);
        this.setY(y);
        this.width = w;
        this.height = h;

        int cy = y + h - complete.getHeight() - 4;
        int cxCenter = x + (w - complete.getWidth()) / 2;
        back.setPosition(x + 2, cy);
        complete.setPosition(cxCenter, cy);
        reject.setPosition(x + w - reject.getWidth() - 2, cy);
    }

    public void setChapter(GuideData.Chapter chapter) {
        this.chapter = chapter;
        this.scrollY = 0f;
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!this.visible || chapter == null) return;

        List<Component> hoveredTooltips = new ArrayList<>();

        int x = this.getX();
        int y = this.getY();
        int w = this.width;

        int contentTop = y + 4;
        int contentBottom = complete.getY() - 6;
        int viewportH = Math.max(0, contentBottom - contentTop);

        measuredContentHeight = measureContentHeight(w);
        int maxScroll = Math.max(0, measuredContentHeight + BOTTOM_PADDING - viewportH);
        scrollY = Mth.clamp(scrollY, 0f, maxScroll);

        gg.enableScissor(x, contentTop, x + w, contentBottom);

        int[] curY = {contentTop + 4 - Mth.floor(scrollY)};

        int nameWidth = w - 32;
        chapter.iconItem().ifPresent(item -> gg.renderItem(new ItemStack(item), x + 4, curY[0]));
        gg.drawWordWrap(mc.font, Component.literal(chapter.name), x + 26, curY[0] + 2, nameWidth, 0xFFFFFF);
        curY[0] += mc.font.wordWrapHeight(chapter.name, nameWidth) + 12;

        if (!chapter.description.isBlank()) {
            gg.drawWordWrap(mc.font, Component.literal(chapter.description), x + 4, curY[0], w - 8, 0xCFCFCF);
            curY[0] += mc.font.wordWrapHeight(chapter.description, w - 8) + 8;
        }

        chapter.imageLocation().ifPresent(img -> {
            int imgX = x + 4;
            int imgW = w - 8;
            int imgH = 80;
            gg.blit(img, imgX, curY[0], 0, 0, imgW, imgH, imgW, imgH);
            curY[0] += imgH + 4;
        });

        gg.disableScissor();
        for (Component tip : hoveredTooltips) gg.renderTooltip(mc.font, tip, mouseX, mouseY);

        complete.active = false;
        complete.visible = false;
        reject.setOptionalAllowed(false);
        reject.active = false;
        reject.visible = false;
    }

    private int measureContentHeight(int panelWidth) {
        if (chapter == null) return 0;
        int w = panelWidth;
        int y = 4;
        y += mc.font.wordWrapHeight(chapter.name, w - 32) + 12;
        if (!chapter.description.isBlank())
            y += mc.font.wordWrapHeight(chapter.description, w - 8) + 8;
        if (chapter.imageLocation().isPresent())
            y += 80 + 4;
        return y;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!this.visible || !this.active) return false;
        int contentTop = this.getY() + 4;
        int contentBottom = complete.getY() - 6;
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

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {}

    private static final class BackButton extends AbstractButton {
        private static final ResourceLocation TEX_NORMAL = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/back_button.png");
        private static final ResourceLocation TEX_HOVER = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/back_highlighted.png");
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
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private static final class CompleteButton extends AbstractButton {
        private static final ResourceLocation TEX_NORMAL = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button.png");
        private static final ResourceLocation TEX_HOVER = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button_highlighted.png");
        private static final ResourceLocation TEX_DISABLED = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button_disabled.png");
        private final Runnable onPress;

        public CompleteButton(int x, int y, Runnable onPress) {
            super(x, y, 68, 20, Component.translatable("quest.boundless.complete"));
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
            this.active = false;
            this.visible = false;
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.active && this.isMouseOver(mouseX, mouseY);
            ResourceLocation tex = !this.active ? TEX_DISABLED : (hovered ? TEX_HOVER : TEX_NORMAL);
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
            var font = Minecraft.getInstance().font;
            int textW = font.width(getMessage());
            int textX = getX() + (this.width - textW) / 2 + 2;
            int textY = getY() + (this.height - font.lineHeight) / 2 + 1;
            int color = this.active ? 0xFFFFFF : 0x808080;
            gg.drawString(font, getMessage(), textX, textY, color, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private static final class RejectButton extends AbstractButton {
        private static final ResourceLocation TEX_NORMAL = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_reject.png");
        private static final ResourceLocation TEX_HOVER = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_reject_highlighted.png");
        private static final ResourceLocation TEX_DISABLED = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_reject_disabled.png");
        private final Runnable onPress;
        private boolean optionalAllowed;

        public RejectButton(int x, int y, Runnable onPress) {
            super(x, y, 24, 20, Component.empty());
            this.onPress = onPress;
        }

        public void setOptionalAllowed(boolean v) {
            this.optionalAllowed = v;
        }

        @Override
        public void onPress() {
            if (this.active && onPress != null) onPress.run();
            this.active = false;
            this.visible = false;
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isMouseOver(mouseX, mouseY);
            ResourceLocation tex = !this.active ? TEX_DISABLED : (hovered ? TEX_HOVER : TEX_NORMAL);
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
            if (hovered && !this.active && !optionalAllowed)
                gg.renderTooltip(Minecraft.getInstance().font, Component.literal("This quest is not optional"), mouseX, mouseY);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }
}
