package net.revilodev.codex.client.skills;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.abilities.AbilitiesAttachments;
import net.revilodev.codex.abilities.PlayerAbilities;
import net.revilodev.codex.client.PanelTab;
import net.revilodev.codex.client.PanelTabButton;
import net.revilodev.codex.client.SkillsToggleButton;
import net.revilodev.codex.client.abilities.AbilityDetailsPanel;
import net.revilodev.codex.client.abilities.AbilityListWidget;
import net.revilodev.codex.skills.SkillsAttachments;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public final class SkillsPanelClient {
    private static final ResourceLocation BTN_TEX =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/skills_button.png");
    private static final ResourceLocation BTN_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/skills_button_hovered.png");
    private static final ResourceLocation PANEL_TEX =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/skills_panel.png");

    private static final int PANEL_W = 147;
    private static final int PANEL_H = 166;
    private static final int INNER_PAD_X = 6;
    private static final int INNER_PAD_TOP = 5;
    private static final int INNER_PAD_BOTTOM = 6;
    private static final int HEADER_OFFSET_X = 5;
    private static final int HEADER_OFFSET_Y = 3;

    private static final Map<Screen, State> STATES = new WeakHashMap<>();
    private static Field LEFT_FIELD;
    private static boolean lastOpen = false;
    private static PanelTab lastTab = PanelTab.SKILLS;

    private SkillsPanelClient() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Init.Post.class, SkillsPanelClient::onScreenInit);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Closing.class, SkillsPanelClient::onScreenClosing);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, ScreenEvent.Render.Pre.class, SkillsPanelClient::onScreenRenderPre);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Render.Post.class, SkillsPanelClient::onScreenRenderPost);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.MouseScrolled.Pre.class, SkillsPanelClient::onMouseScrolled);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.MouseButtonPressed.Pre.class, SkillsPanelClient::onMousePressed);
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof InventoryScreen inv)) return;

        State st = new State(inv);
        STATES.put(screen, st);

        st.btn = new SkillsToggleButton(inv.getGuiLeft() + 145, inv.getGuiTop() + 61, BTN_TEX, BTN_TEX_HOVER, () -> toggle(st));
        st.bg = new PanelBackground(0, 0, PANEL_W, PANEL_H, () -> st.activeTab.title());
        st.skillsList = new SkillListWidget(0, 0, SkillListWidget.gridWidth(), SkillListWidget.preferredHeight(), def -> {
            st.skillsDetails.setSkill(def);
            st.skillsList.setSelected(def == null ? null : def.id());
        });
        st.skillsList.setHeaderVisible(false);
        st.skillsList.reloadSkills();
        st.skillsDetails = new SkillDetailsPanel(0, 0, SkillListWidget.gridWidth(), PANEL_H / 3);
        st.abilityList = new AbilityListWidget(0, 0, AbilityListWidget.gridWidth(), AbilityListWidget.preferredHeight(), def -> {
            st.abilityDetails.setAbility(def);
            st.abilityList.setSelected(def == null ? null : def.id());
        });
        st.abilityList.setHeaderVisible(false);
        st.abilityList.setShowLocked(false);
        st.abilityDetails = new AbilityDetailsPanel(0, 0, AbilityListWidget.gridWidth(), PANEL_H / 3);
        st.skillsTab = new PanelTabButton(0, 0, PanelTab.SKILLS, () -> setTab(st, PanelTab.SKILLS));
        st.abilitiesTab = new PanelTabButton(0, 0, PanelTab.ABILITIES, () -> setTab(st, PanelTab.ABILITIES));

        event.addListener(st.btn);

        reposition(inv, st);
        st.recipeBtn = findRecipeButton(inv);

        if (lastOpen) {
            st.open = true;
            st.originalLeft = getLeft(inv);
            setLeft(inv, computeCenteredLeft(inv));
        }
        st.activeTab = lastTab;
        updateVisibility(st);
        applySkillsVsRecipePanelRule(inv, st);
        if (st.open) forceHideRecipeButtonIfSkillsOpen(st);
    }

    public static void onScreenClosing(ScreenEvent.Closing event) {
        State st = STATES.remove(event.getScreen());
        if (st == null) return;
        if (st.open && st.originalLeft != null) {
            setLeft(st.inv, st.originalLeft);
        }
    }

    public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        Screen screen = event.getScreen();
        State st = STATES.get(screen);
        if (st == null || !(screen instanceof InventoryScreen inv)) return;

        if (st.open) setLeft(inv, computeCenteredLeft(inv));
        reposition(inv, st);
        updateVisibility(st);
        applySkillsVsRecipePanelRule(inv, st);

        if (st.recipeBtn == null) st.recipeBtn = findRecipeButton(inv);
        if (st.open) forceHideRecipeButtonIfSkillsOpen(st);
        else restoreRecipeButtonIfWeHidIt(inv, st);
    }

    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        State st = STATES.get(screen);
        if (st == null || !st.open || !(screen instanceof InventoryScreen inv)) return;
        event.getGuiGraphics().pose().pushPose();
        event.getGuiGraphics().pose().translate(0.0F, 0.0F, 400.0F);
        st.bg.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
        st.skillsTab.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
        st.abilitiesTab.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
        if (st.activeTab == PanelTab.SKILLS) {
            st.skillsList.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            st.skillsDetails.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            st.skillsDetails.upgradeButton().render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            st.skillsDetails.downgradeButton().render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
        } else {
            st.abilityList.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            st.abilityDetails.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            st.abilityDetails.upgradeButton().render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            st.abilityDetails.downgradeButton().render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
        }
        event.getGuiGraphics().pose().popPose();
        renderPointsBadge(event.getGuiGraphics(), st, inv);
    }

    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        State st = STATES.get(event.getScreen());
        if (st == null || !st.open) return;

        double mx = event.getMouseX();
        double my = event.getMouseY();
        double deltaY = event.getScrollDeltaY();
        boolean used = false;

        if (st.activeTab == PanelTab.SKILLS && st.skillsDetails.visible) {
            used = st.skillsDetails.mouseScrolled(mx, my, deltaY);
        } else if (st.activeTab == PanelTab.ABILITIES && st.abilityDetails.visible) {
            used = st.abilityDetails.mouseScrolled(mx, my, deltaY);
        }

        if (used) event.setCanceled(true);
    }

    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        State st = STATES.get(event.getScreen());
        if (st == null || !st.open || event.getButton() != 0) return;

        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        if (st.skillsTab.mouseClicked(mouseX, mouseY, event.getButton()) || st.abilitiesTab.mouseClicked(mouseX, mouseY, event.getButton())) {
            event.setCanceled(true);
            return;
        }

        boolean used = false;
        if (st.activeTab == PanelTab.SKILLS && st.skillsDetails.hasSkill()) {
            used = st.skillsDetails.mouseClicked(mouseX, mouseY, event.getButton()) || st.skillsList.mouseClicked(mouseX, mouseY, event.getButton());
            boolean inNode = st.skillsList.isOnSkillNode(mouseX, mouseY);
            boolean onButton = st.skillsDetails.isOnButtons(mouseX, mouseY);
            boolean inDetails = st.skillsDetails.containsPoint(mouseX, mouseY);
            if (!inNode && !onButton && !inDetails) {
                st.skillsDetails.setSkill(null);
                st.skillsList.setSelected(null);
            }
        } else if (st.activeTab == PanelTab.ABILITIES && st.abilityDetails.hasAbility()) {
            used = st.abilityDetails.mouseClicked(mouseX, mouseY, event.getButton()) || st.abilityList.mouseClicked(mouseX, mouseY, event.getButton());
            boolean inNode = st.abilityList.isOnAbilityNode(mouseX, mouseY);
            boolean onButton = st.abilityDetails.isOnButtons(mouseX, mouseY);
            boolean inDetails = st.abilityDetails.containsPoint(mouseX, mouseY);
            if (!inNode && !onButton && !inDetails) {
                st.abilityDetails.setAbility(null);
                st.abilityList.setSelected(null);
            }
        } else if (st.activeTab == PanelTab.SKILLS) {
            used = st.skillsList.mouseClicked(mouseX, mouseY, event.getButton()) || st.skillsDetails.mouseClicked(mouseX, mouseY, event.getButton());
        } else {
            used = st.abilityList.mouseClicked(mouseX, mouseY, event.getButton()) || st.abilityDetails.mouseClicked(mouseX, mouseY, event.getButton());
        }

        if (used || isInsideOverlay(st, mouseX, mouseY)) {
            event.setCanceled(true);
        }
    }

    private static void toggle(State st) {
        st.open = !st.open;
        lastOpen = st.open;
        if (st.open) {
            if (st.originalLeft == null) st.originalLeft = getLeft(st.inv);
            setLeft(st.inv, computeCenteredLeft(st.inv));
        } else if (st.originalLeft != null) {
            setLeft(st.inv, st.originalLeft);
        }

        reposition(st.inv, st);
        updateVisibility(st);
        applySkillsVsRecipePanelRule(st.inv, st);

        if (st.recipeBtn == null) st.recipeBtn = findRecipeButton(st.inv);
        if (st.open) forceHideRecipeButtonIfSkillsOpen(st);
        else restoreRecipeButtonIfWeHidIt(st.inv, st);
    }

    private static void setTab(State st, PanelTab tab) {
        st.activeTab = tab;
        lastTab = tab;
        updateVisibility(st);
    }

    private static void applySkillsVsRecipePanelRule(InventoryScreen inv, State st) {
        if (st.open) {
            if (st.btn != null) st.btn.visible = true;
            return;
        }

        if (isRecipePanelOpen(inv)) {
            if (st.btn != null) st.btn.visible = false;
        } else {
            if (st.btn != null) st.btn.visible = true;
        }
    }

    private static void forceHideRecipeButtonIfSkillsOpen(State st) {
        if (st.recipeBtn == null) return;
        st.recipeBtn.visible = false;
        st.recipeBtn.active = false;
        st.recipeHiddenBySkills = true;
    }

    private static void restoreRecipeButtonIfWeHidIt(InventoryScreen inv, State st) {
        if (!st.recipeHiddenBySkills) return;
        if (isQuestPanelOpen(inv)) {
            st.recipeHiddenBySkills = false;
            return;
        }
        if (st.recipeBtn == null) st.recipeBtn = findRecipeButton(inv);
        if (st.recipeBtn != null) {
            st.recipeBtn.visible = true;
            st.recipeBtn.active = true;
        }
        st.recipeHiddenBySkills = false;
    }

    private static ImageButton findRecipeButton(InventoryScreen inv) {
        for (var child : inv.children()) {
            if (child instanceof ImageButton btn && btn.getWidth() == 20 && btn.getHeight() == 18) {
                return btn;
            }
        }
        return null;
    }

    private static boolean isQuestPanelOpen(InventoryScreen inv) {
        for (var child : inv.children()) {
            if (child instanceof AbstractWidget w) {
                String name = child.getClass().getName();
                if (name.equals("net.revilodev.boundless.client.QuestPanelClient$PanelBackground") && w.visible) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRecipePanelOpen(InventoryScreen inv) {
        int centeredLeft = (inv.width - inv.getXSize()) / 2;
        return inv.getGuiLeft() > centeredLeft + 10;
    }

    private static int computeCenteredLeft(InventoryScreen inv) {
        int total = PANEL_W + 2 + inv.getXSize();
        return (inv.width - total) / 2 + PANEL_W + 2;
    }

    private static int computePanelX(InventoryScreen inv) {
        return inv.getGuiLeft() - PANEL_W - 2;
    }

    private static void reposition(InventoryScreen inv, State st) {
        if (st.btn != null) st.btn.setPosition(inv.getGuiLeft() + 145, inv.getGuiTop() + 61);

        int bgx = computePanelX(inv);
        int bgy = inv.getGuiTop();
        int innerLeft = bgx + INNER_PAD_X;
        int innerRight = bgx + PANEL_W - INNER_PAD_X;
        int innerTop = bgy + INNER_PAD_TOP;
        int innerBottom = bgy + PANEL_H - INNER_PAD_BOTTOM;

        int listW = SkillListWidget.gridWidth();
        int listX = bgx + (PANEL_W - listW) / 2;
        int listY = innerTop;
        int detailsH = PANEL_H / 3 + 10;
        int detailsW = Math.max(20, (innerRight - innerLeft) - 5);
        int detailsX = innerLeft + 2;
        int detailsY = innerBottom - detailsH - 5;

        st.bg.setBounds(bgx, bgy, PANEL_W, PANEL_H);
        st.skillsList.setBounds(listX, listY, listW, SkillListWidget.preferredHeight());
        st.skillsDetails.setBounds(detailsX, detailsY, detailsW, detailsH);
        st.abilityList.setBounds(listX, listY, listW, AbilityListWidget.preferredHeight());
        st.abilityDetails.setBounds(detailsX, detailsY, detailsW, detailsH);
        st.skillsTab.setPosition(bgx - 31, bgy + 6);
        st.abilitiesTab.setPosition(bgx - 31, bgy + 34);
    }

    private static Integer getLeft(InventoryScreen inv) {
        try {
            if (LEFT_FIELD == null) LEFT_FIELD = findLeftField(inv.getClass());
            return (Integer) LEFT_FIELD.get(inv);
        } catch (Throwable t) {
            return inv.getGuiLeft();
        }
    }

    private static void setLeft(InventoryScreen inv, int v) {
        try {
            if (LEFT_FIELD == null) LEFT_FIELD = findLeftField(inv.getClass());
            LEFT_FIELD.setInt(inv, v);
        } catch (Throwable ignored) {}
    }

    private static Field findLeftField(Class<?> c) throws NoSuchFieldException {
        Class<?> cur = c;
        while (cur != null) {
            try {
                Field f = cur.getDeclaredField("leftPos");
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                cur = cur.getSuperclass();
            }
        }
        throw new NoSuchFieldException("leftPos");
    }

    private static void updateVisibility(State st) {
        boolean skillsActive = st.open && st.activeTab == PanelTab.SKILLS;
        boolean abilitiesActive = st.open && st.activeTab == PanelTab.ABILITIES;

        st.bg.visible = st.open;
        st.bg.active = st.open;
        st.skillsTab.visible = st.open;
        st.skillsTab.active = st.open;
        st.skillsTab.setSelected(st.activeTab == PanelTab.SKILLS);
        st.abilitiesTab.visible = st.open;
        st.abilitiesTab.active = st.open;
        st.abilitiesTab.setSelected(st.activeTab == PanelTab.ABILITIES);

        st.skillsList.visible = skillsActive;
        st.skillsList.active = skillsActive;
        st.skillsDetails.visible = skillsActive;
        st.skillsDetails.active = skillsActive;
        st.skillsDetails.upgradeButton().visible = skillsActive;
        st.skillsDetails.upgradeButton().active = skillsActive;
        st.skillsDetails.downgradeButton().visible = skillsActive;
        st.skillsDetails.downgradeButton().active = skillsActive;

        st.abilityList.visible = abilitiesActive;
        st.abilityList.active = abilitiesActive;
        st.abilityDetails.visible = abilitiesActive;
        st.abilityDetails.active = abilitiesActive;
        st.abilityDetails.upgradeButton().visible = abilitiesActive;
        st.abilityDetails.upgradeButton().active = abilitiesActive;
        st.abilityDetails.downgradeButton().visible = abilitiesActive;
        st.abilityDetails.downgradeButton().active = abilitiesActive;
    }

    private static void renderPointsBadge(GuiGraphics gg, State st, InventoryScreen inv) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) return;

        int points = st.activeTab == PanelTab.ABILITIES
                ? mc.player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get()).points()
                : mc.player.getData(SkillsAttachments.PLAYER_SKILLS.get()).points();

        String label = "Points: " + points;
        int x = st.bg.getX() + PANEL_W - mc.font.width(label) - 8;
        int y = st.bg.getY() + 8;
        gg.drawString(mc.font, label, x, y, 0xFFFFFF, true);
    }

    private static boolean isInsideOverlay(State st, double mouseX, double mouseY) {
        int left = st.bg.getX();
        int top = st.bg.getY();
        int right = left + PANEL_W;
        int bottom = top + PANEL_H;
        int tabLeft = left - 31;
        return mouseX >= tabLeft && mouseX <= right && mouseY >= top && mouseY <= bottom;
    }

    private static final class PanelBackground extends AbstractWidget {
        private final Supplier<String> titleSupplier;

        private PanelBackground(int x, int y, int w, int h, Supplier<String> titleSupplier) {
            super(x, y, w, h, Component.empty());
            this.titleSupplier = titleSupplier;
        }

        public void setBounds(int x, int y, int w, int h) {
            setX(x);
            setY(y);
            width = w;
            height = h;
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            RenderSystem.disableBlend();
            gg.blit(PANEL_TEX, getX(), getY(), 0, 0, width, height, width, height);
            SkillPanelHeaderRenderer.draw(
                    gg,
                    net.minecraft.client.Minecraft.getInstance().font,
                    getX() + HEADER_OFFSET_X,
                    getY() - SkillPanelHeaderRenderer.height() + HEADER_OFFSET_Y,
                    titleSupplier.get()
            );
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private static final class State {
        final InventoryScreen inv;
        SkillsToggleButton btn;
        PanelBackground bg;
        SkillListWidget skillsList;
        SkillDetailsPanel skillsDetails;
        AbilityListWidget abilityList;
        AbilityDetailsPanel abilityDetails;
        PanelTabButton skillsTab;
        PanelTabButton abilitiesTab;
        ImageButton recipeBtn;
        boolean recipeHiddenBySkills;
        boolean open;
        Integer originalLeft;
        PanelTab activeTab = PanelTab.SKILLS;

        State(InventoryScreen inv) {
            this.inv = inv;
        }
    }
}
