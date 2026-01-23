// src/main/java/net/revilodev/codex/client/screen/StandaloneSkillsBookScreen.java
package net.revilodev.codex.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.client.skills.SkillCategoryTabsWidget;
import net.revilodev.codex.client.skills.SkillDetailsPanel;
import net.revilodev.codex.client.skills.SkillListWidget;
import net.revilodev.codex.skills.SkillRegistry;

@OnlyIn(Dist.CLIENT)
public final class StandaloneSkillsBookScreen extends Screen {

    private static final ResourceLocation PANEL_TEX =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/skills_panel.png");

    private final int panelWidth = 147;
    private final int panelHeight = 166;

    private int leftX;
    private int rightX;
    private int topY;

    private SkillCategoryTabsWidget tabs;
    private SkillListWidget list;
    private SkillDetailsPanel details;

    private boolean showingDetails = false;
    private String selectedCategory = "combat";

    public StandaloneSkillsBookScreen() {
        super(Component.literal("Skills"));
    }

    @Override
    protected void init() {
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

        tabs = new SkillCategoryTabsWidget(leftX - 23, topY + 4, 26, panelHeight - 8, id -> {
            selectedCategory = id;
            list.setCategory(id);
            showingDetails = false;
            updateVisibility();
        });
        tabs.setCategories(SkillRegistry.categoriesOrdered());
        tabs.setSelected(selectedCategory);

        list = new SkillListWidget(pxLeft, py, pw, ph, def -> {
            details.setSkill(def);
            showingDetails = true;
            updateVisibility();
        });
        list.setSkills(allSkills());
        list.setCategory(selectedCategory);

        details = new SkillDetailsPanel(pxRight, py, pw, ph, () -> {
            showingDetails = false;
            updateVisibility();
        });
        details.setBounds(pxRight, py, pw, ph);

        addRenderableWidget(tabs);
        addRenderableWidget(list);
        addRenderableWidget(details);
        addRenderableWidget(details.backButton());
        addRenderableWidget(details.upgradeButton());
        addRenderableWidget(details.downgradeButton());

        updateVisibility();
    }

    private Iterable<net.revilodev.codex.skills.SkillDefinition> allSkills() {
        return () -> net.revilodev.codex.skills.SkillId.values().length == 0
                ? java.util.Collections.<net.revilodev.codex.skills.SkillDefinition>emptyIterator()
                : new java.util.Iterator<>() {
            private final net.revilodev.codex.skills.SkillId[] ids = net.revilodev.codex.skills.SkillId.values();
            private int i = 0;

            @Override public boolean hasNext() { return i < ids.length; }
            @Override public net.revilodev.codex.skills.SkillDefinition next() { return SkillRegistry.def(ids[i++]); }
        };
    }

    private void updateVisibility() {
        list.visible = true;
        list.active = true;

        details.visible = showingDetails;
        details.active = showingDetails;

        details.backButton().visible = showingDetails;
        details.backButton().active = showingDetails;

        details.upgradeButton().visible = showingDetails;
        details.upgradeButton().active = showingDetails;

        details.downgradeButton().visible = showingDetails;
        details.downgradeButton().active = showingDetails;

        tabs.visible = true;
        tabs.active = true;
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
                if (list.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
            }
        }
        if (details.visible && details.active) {
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
