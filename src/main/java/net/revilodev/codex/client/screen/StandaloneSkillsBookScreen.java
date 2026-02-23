package net.revilodev.codex.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.client.skills.SkillDetailsPanel;
import net.revilodev.codex.client.skills.SkillListWidget;
import net.revilodev.codex.skills.SkillDefinition;
import net.revilodev.codex.skills.SkillId;
import net.revilodev.codex.skills.SkillRegistry;

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
    private static final int SECTION_GAP = 4;

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

        int detailsX = innerLeft;
        int detailsY = listY + listH + SECTION_GAP;
        int detailsW = innerRight - innerLeft;
        int detailsH = Math.max(20, innerBottom - detailsY);

        list = new SkillListWidget(listX, listY, listW, listH, def -> {
            if (def != null && details != null) details.setSkill(def);
        });
        list.setSkills(allSkills());
        addRenderableWidget(list);

        details = new SkillDetailsPanel(detailsX, detailsY, detailsW, detailsH, () -> {});
        details.setShowBackButton(false);
        details.setSkill(firstSkill());
        addRenderableWidget(details);
        addRenderableWidget(details.backButton());
        addRenderableWidget(details.upgradeButton());
        addRenderableWidget(details.downgradeButton());
    }

    private Iterable<SkillDefinition> allSkills() {
        return () -> new java.util.Iterator<>() {
            private final SkillId[] ids = SkillId.values();
            private int i = 0;

            @Override public boolean hasNext() { return i < ids.length; }
            @Override public SkillDefinition next() { return SkillRegistry.def(ids[i++]); }
        };
    }

    private SkillDefinition firstSkill() {
        SkillId[] ids = SkillId.values();
        if (ids.length == 0) return null;
        return SkillRegistry.def(ids[0]);
    }

    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        gg.fill(0, 0, this.width, this.height, 0xA0000000);
        gg.blit(PANEL_TEX, panelX, panelY, 0, 0, panelWidth, panelHeight, panelWidth, panelHeight);
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
    public boolean isPauseScreen() {
        return false;
    }
}
