package net.revilodev.codex.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.client.skills.SkillDetailsPanel;
import net.revilodev.codex.client.skills.SkillListWidget;
import net.revilodev.codex.client.skills.SkillPanelHeaderRenderer;

@OnlyIn(Dist.CLIENT)
public final class StandaloneSkillsBookScreen extends Screen {

    private static final ResourceLocation PANEL_TEX =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/skills_panel.png");

    private final int panelWidth = 147;
    private final int panelHeight = 166;

    private int panelX;
    private int panelY;

    private SkillListWidget list;
    private SkillDetailsPanel details;
    private static final int INNER_PAD_X = 6;
    private static final int INNER_PAD_TOP = 5;
    private static final int INNER_PAD_BOTTOM = 6;
    private static final int HEADER_OFFSET_X = 5;
    private static final int HEADER_OFFSET_Y = 3;

    public StandaloneSkillsBookScreen() {
        super(Component.literal("Skills"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        panelX = cx - panelWidth / 2;
        panelY = cy - panelHeight / 2;

        int innerLeft = panelX + INNER_PAD_X;
        int innerRight = panelX + panelWidth - INNER_PAD_X;
        int innerTop = panelY + INNER_PAD_TOP;
        int innerBottom = panelY + panelHeight - INNER_PAD_BOTTOM;

        int listW = SkillListWidget.gridWidth();
        int listX = panelX + (panelWidth - listW) / 2;
        int listY = innerTop;
        int listH = SkillListWidget.preferredHeight();

        int detailsH = panelHeight / 3;
        int detailsW = Math.max(20, (innerRight - innerLeft) - 5);
        int detailsX = innerLeft + 2;
        int detailsY = panelY + panelHeight - detailsH - 7;

        list = new SkillListWidget(listX, listY, listW, listH, def -> {
            if (details != null) details.setSkill(def);
            list.setSelected(def == null ? null : def.id());
        });
        list.reloadSkills();
        addRenderableWidget(list);

        details = new SkillDetailsPanel(detailsX, detailsY, detailsW, detailsH);
        details.setSkill(null);
        addRenderableWidget(details);
        addRenderableWidget(details.upgradeButton());
        addRenderableWidget(details.downgradeButton());
    }

    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        gg.fill(0, 0, this.width, this.height, 0xA0000000);
        gg.blit(PANEL_TEX, panelX, panelY, 0, 0, panelWidth, panelHeight, panelWidth, panelHeight);
        SkillPanelHeaderRenderer.draw(
                gg,
                Minecraft.getInstance().font,
                panelX + HEADER_OFFSET_X,
                panelY - SkillPanelHeaderRenderer.height() + HEADER_OFFSET_Y,
                "Skills"
        );
        super.render(gg, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (list.visible && list.active) {
            if (mouseX >= list.getX() && mouseX <= list.getX() + list.getWidth()
                    && mouseY >= list.getY() && mouseY <= list.getY() + list.getHeight()) {
                if (list.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
            }
        }
        if (details != null && details.visible && details.active) {
            if (mouseX >= details.getX() && mouseX <= details.getX() + details.getWidth()
                    && mouseY >= details.getY() && mouseY <= details.getY() + details.getHeight()) {
                if (details.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean used = super.mouseClicked(mouseX, mouseY, button);
        if (button == 0 && !used && details != null && details.hasSkill()) {
            boolean inListNode = list != null && list.isOnSkillNode(mouseX, mouseY);
            boolean onDetailsButton = details.isOnButtons(mouseX, mouseY);
            if (!inListNode && !onDetailsButton) {
                details.setSkill(null);
                if (list != null) list.setSelected(null);
            }
        }
        return used;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
