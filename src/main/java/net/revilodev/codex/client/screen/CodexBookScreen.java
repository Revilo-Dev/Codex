package net.revilodev.codex.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.codex.client.CodexDetailsPanel;
import net.revilodev.codex.client.CodexListWidget;
import net.revilodev.codex.data.GuideData;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class CodexBookScreen extends Screen {

    private static final ResourceLocation PANEL_TEX =
            ResourceLocation.fromNamespaceAndPath("codex", "textures/gui/panel.png");

    private int panelWidth = 147;
    private int panelHeight = 166;

    private int leftX;
    private int rightX;
    private int topY;

    private CodexListWidget list;
    private CodexDetailsPanel details;
    private ListBackButton listBackButton;

    private boolean showingDetails = false;
    private boolean showingChapters = false;

    private GuideData.Category currentCategory;

    private final List<GuideData.Category> categories = new ArrayList<>();
    private final List<GuideData.Chapter> chapters = new ArrayList<>();

    public CodexBookScreen() {
        super(Component.literal("Codex"));
    }

    @Override
    protected void init() {
        GuideData.loadClient(false);

        categories.clear();
        categories.addAll(GuideData.allCategories());

        chapters.clear();
        chapters.addAll(GuideData.allChapters());

        int cx = this.width / 2;
        int cy = this.height / 2;

        leftX = cx - panelWidth - 2;
        rightX = cx + 2;
        topY = cy - panelHeight / 2;

        int pxLeft = leftX + 10;
        int pxRight = rightX + 10;
        int py = topY + 10;
        int pw = 127;
        int ph = panelHeight - 20;

        // LIST WIDGET (categories or chapters)
        list = new CodexListWidget(
                pxLeft, py, pw, ph,
                category -> {
                    currentCategory = category;
                    showingChapters = true;
                    updateListContents();
                    updateVisibility();
                },
                chapter -> {
                    details.setChapter(chapter);
                    showingDetails = true;
                    updateVisibility();
                }
        );
        addRenderableWidget(list);
        updateListContents();

        // DETAILS PANEL
        details = new CodexDetailsPanel(pxRight, py, pw, ph, () -> {
            showingDetails = false;
            updateVisibility();
        });
        addRenderableWidget(details);
        addRenderableWidget(details.backButton());

        // CHAPTER BACK BUTTON (go back to categories)
        int backX = pxLeft + 2;
        int backY = py + ph - 20 - 4;

        listBackButton = new ListBackButton(backX, backY, () -> {
            showingChapters = false;
            currentCategory = null;
            showingDetails = false;
            updateListContents();
            updateVisibility();
        });
        addRenderableWidget(listBackButton);

        updateVisibility();
    }

    private void updateListContents() {
        if (!showingChapters || currentCategory == null) {
            list.showCategories(categories);
        } else {
            list.showChapters(
                    chapters.stream()
                            .filter(ch -> ch.category.equalsIgnoreCase(currentCategory.id))
                            .toList()
            );
        }
    }

    private void updateVisibility() {
        list.visible = !showingDetails;
        list.active = !showingDetails;

        details.visible = showingDetails;
        details.active = showingDetails;
        details.backButton().visible = showingDetails;

        listBackButton.visible = showingChapters && !showingDetails;
        listBackButton.active = listBackButton.visible;
    }

    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        gg.fill(0, 0, this.width, this.height, 0xA0000000);

        gg.blit(PANEL_TEX, leftX, topY, 0, 0, panelWidth, panelHeight, panelWidth, panelHeight);
        gg.blit(PANEL_TEX, rightX, topY, 0, 0, panelWidth, panelHeight, panelWidth, panelHeight);

        super.render(gg, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (list.visible && list.active) {
            if (mouseX >= list.getX() && mouseX <= list.getX() + list.getWidth()
                    && mouseY >= list.getY() && mouseY <= list.getY() + list.getHeight()) {

                return list.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            }
        }

        if (details.visible) {
            if (mouseX >= details.getX() && mouseX <= details.getX() + details.getWidth()
                    && mouseY >= details.getY() && mouseY <= details.getY() + details.getHeight()) {

                return details.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class ListBackButton extends AbstractButton {
        private static final ResourceLocation TEX =
                ResourceLocation.fromNamespaceAndPath("codex", "textures/gui/sprites/back.png");

        private final Runnable onPressAction;

        public ListBackButton(int x, int y, Runnable onPressAction) {
            super(x, y, 24, 20, Component.empty());
            this.onPressAction = onPressAction;
        }

        @Override
        public void onPress() {
            onPressAction.run();
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            gg.blit(TEX, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            // No narration needed
        }
    }
}
