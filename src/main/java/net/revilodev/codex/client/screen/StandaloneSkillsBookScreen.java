package net.revilodev.codex.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.client.PanelTab;
import net.revilodev.codex.client.PanelTabButton;
import net.revilodev.codex.client.abilities.AbilityDetailsPanel;
import net.revilodev.codex.client.abilities.AbilityListWidget;
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

    private SkillListWidget skillsList;
    private SkillDetailsPanel skillsDetails;
    private AbilityListWidget abilityList;
    private AbilityDetailsPanel abilityDetails;
    private PanelTabButton skillsTab;
    private PanelTabButton abilitiesTab;
    private PanelTab activeTab = PanelTab.SKILLS;

    private static final int INNER_PAD_X = 6;
    private static final int INNER_PAD_TOP = 5;
    private static final int INNER_PAD_BOTTOM = 6;
    private static final int HEADER_OFFSET_X = 5;
    private static final int HEADER_OFFSET_Y = 3;

    public StandaloneSkillsBookScreen() {
        super(Component.literal("Codex"));
    }

    @Override
    protected void init() {
        panelX = this.width / 2 - panelWidth / 2;
        panelY = this.height / 2 - panelHeight / 2;

        int innerLeft = panelX + INNER_PAD_X;
        int innerRight = panelX + panelWidth - INNER_PAD_X;
        int innerTop = panelY + INNER_PAD_TOP;
        int innerBottom = panelY + panelHeight - INNER_PAD_BOTTOM;
        int detailsH = panelHeight / 3 + 10;
        int detailsW = Math.max(20, (innerRight - innerLeft) - 5);
        int detailsX = innerLeft + 2;
        int detailsY = innerBottom - detailsH;

        skillsList = new SkillListWidget(panelX + (panelWidth - SkillListWidget.gridWidth()) / 2, innerTop, SkillListWidget.gridWidth(), SkillListWidget.preferredHeight(), def -> {
            skillsDetails.setSkill(def);
            skillsList.setSelected(def == null ? null : def.id());
        });
        skillsList.reloadSkills();
        skillsDetails = new SkillDetailsPanel(detailsX, detailsY, detailsW, detailsH);

        abilityList = new AbilityListWidget(panelX + (panelWidth - AbilityListWidget.gridWidth()) / 2, innerTop, AbilityListWidget.gridWidth(), AbilityListWidget.preferredHeight(), def -> {
            abilityDetails.setAbility(def);
            abilityList.setSelected(def == null ? null : def.id());
        });
        abilityList.setShowLocked(false);
        abilityDetails = new AbilityDetailsPanel(detailsX, detailsY, detailsW, detailsH);

        skillsTab = new PanelTabButton(panelX - 31, panelY + 6, PanelTab.SKILLS, () -> setTab(PanelTab.SKILLS));
        abilitiesTab = new PanelTabButton(panelX - 31, panelY + 34, PanelTab.ABILITIES, () -> setTab(PanelTab.ABILITIES));

        addRenderableWidget(skillsList);
        addRenderableWidget(skillsDetails);
        addRenderableWidget(skillsDetails.upgradeButton());
        addRenderableWidget(skillsDetails.downgradeButton());
        addRenderableWidget(abilityList);
        addRenderableWidget(abilityDetails);
        addRenderableWidget(abilityDetails.upgradeButton());
        addRenderableWidget(abilityDetails.downgradeButton());
        addRenderableWidget(skillsTab);
        addRenderableWidget(abilitiesTab);

        setTab(activeTab);
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
                activeTab.title()
        );
        super.render(gg, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (activeTab == PanelTab.SKILLS && skillsDetails != null && skillsDetails.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (activeTab == PanelTab.ABILITIES && abilityDetails != null && abilityDetails.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean used = super.mouseClicked(mouseX, mouseY, button);
        if (button == 0 && !used) {
            if (activeTab == PanelTab.SKILLS && skillsDetails.hasSkill()) {
                boolean inListNode = skillsList.isOnSkillNode(mouseX, mouseY);
                boolean onButton = skillsDetails.isOnButtons(mouseX, mouseY);
                boolean inDetails = skillsDetails.containsPoint(mouseX, mouseY);
                if (!inListNode && !onButton && !inDetails) {
                    skillsDetails.setSkill(null);
                    skillsList.setSelected(null);
                }
            } else if (activeTab == PanelTab.ABILITIES && abilityDetails.hasAbility()) {
                boolean inListNode = abilityList.isOnAbilityNode(mouseX, mouseY);
                boolean onButton = abilityDetails.isOnButtons(mouseX, mouseY);
                boolean inDetails = abilityDetails.containsPoint(mouseX, mouseY);
                if (!inListNode && !onButton && !inDetails) {
                    abilityDetails.setAbility(null);
                    abilityList.setSelected(null);
                }
            }
        }
        return used;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void setTab(PanelTab tab) {
        activeTab = tab;
        boolean skillsActive = activeTab == PanelTab.SKILLS;
        skillsTab.setSelected(skillsActive);
        abilitiesTab.setSelected(!skillsActive);

        skillsList.visible = skillsActive;
        skillsList.active = skillsActive;
        skillsDetails.visible = skillsActive;
        skillsDetails.active = skillsActive;
        skillsDetails.upgradeButton().visible = skillsActive;
        skillsDetails.upgradeButton().active = skillsActive;
        skillsDetails.downgradeButton().visible = skillsActive;
        skillsDetails.downgradeButton().active = skillsActive;

        abilityList.visible = !skillsActive;
        abilityList.active = !skillsActive;
        abilityDetails.visible = !skillsActive;
        abilityDetails.active = !skillsActive;
        abilityDetails.upgradeButton().visible = !skillsActive;
        abilityDetails.upgradeButton().active = !skillsActive;
        abilityDetails.downgradeButton().visible = !skillsActive;
        abilityDetails.downgradeButton().active = !skillsActive;
    }
}
