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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.client.SkillsToggleButton;
import net.revilodev.codex.skills.SkillDefinition;
import net.revilodev.codex.skills.SkillId;
import net.revilodev.codex.skills.SkillRegistry;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;

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
    private static final int SECTION_GAP = 4;

    private static final Map<Screen, State> STATES = new WeakHashMap<>();
    private static Field LEFT_FIELD;
    private static boolean lastOpen = false;

    private SkillsPanelClient() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Init.Post.class, SkillsPanelClient::onScreenInit);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Closing.class, SkillsPanelClient::onScreenClosing);

        // Run LAST so our "hide recipe button while skills open" wins,
        // but we DO NOT force-show when skills is closed (so QuestPanelClient can still hide it).
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, ScreenEvent.Render.Pre.class, SkillsPanelClient::onScreenRenderPre);

        NeoForge.EVENT_BUS.addListener(ScreenEvent.MouseScrolled.Pre.class, SkillsPanelClient::onMouseScrolled);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.MouseDragged.Pre.class, SkillsPanelClient::onMouseDragged);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.MouseButtonReleased.Pre.class, SkillsPanelClient::onMouseReleased);
    }

    public static void onScreenInit(ScreenEvent.Init.Post e) {
        Screen s = e.getScreen();
        if (!(s instanceof InventoryScreen inv)) return;

        State st = new State(inv);
        STATES.put(s, st);

        int btnX = inv.getGuiLeft() + 145;
        int btnY = inv.getGuiTop() + 61;

        SkillsToggleButton btn = new SkillsToggleButton(btnX, btnY, BTN_TEX, BTN_TEX_HOVER, () -> toggle(st));
        st.btn = btn;

        st.bg = new PanelBackground(0, 0, PANEL_W, PANEL_H);
        e.addListener(st.bg);

        st.list = new SkillListWidget(0, 0, SkillListWidget.gridWidth(), PANEL_H / 2, def -> {
            if (def != null && st.details != null) st.details.setSkill(def);
        });
        st.list.setSkills(allSkills());
        e.addListener(st.list);

        st.details = new SkillDetailsPanel(0, 0, SkillListWidget.gridWidth(), PANEL_H / 2, () -> {});
        st.details.setShowBackButton(false);
        st.details.setSkill(firstSkill());
        e.addListener(st.details);
        e.addListener(st.details.backButton());
        e.addListener(st.details.upgradeButton());
        e.addListener(st.details.downgradeButton());

        e.addListener(btn);

        reposition(inv, st);

        // Cache the vanilla recipe button (your debug shows it is ImageButton 20x18 at 229,98).
        st.recipeBtn = findRecipeButton(inv);

        if (lastOpen) {
            st.open = true;
            st.originalLeft = getLeft(inv);
            setLeft(inv, computeCenteredLeft(inv));
            updateVisibility(st);
            applySkillsVsRecipePanelRule(inv, st);
            forceHideRecipeButtonIfSkillsOpen(st);
        }
    }

    public static void onScreenClosing(ScreenEvent.Closing e) {
        State st = STATES.remove(e.getScreen());
        if (st == null) return;
        if (st.open && st.originalLeft != null) {
            setLeft(st.inv, st.originalLeft);
        }
    }

    public static void onScreenRenderPre(ScreenEvent.Render.Pre e) {
        Screen s = e.getScreen();
        State st = STATES.get(s);
        if (st == null || !(s instanceof InventoryScreen inv)) return;

        if (st.open) {
            setLeft(inv, computeCenteredLeft(inv));
        }

        reposition(inv, st);
        updateVisibility(st);
        applySkillsVsRecipePanelRule(inv, st);

        if (st.recipeBtn == null) st.recipeBtn = findRecipeButton(inv);

        // Key behavior:
        // - If skills is open -> force-hide recipe button (green book).
        // - If skills is closed -> DO NOT force-show (QuestPanelClient can hide it when quest open).
        // - If we previously hid it and skills is now closed -> restore ONLY if quest is not open.
        if (st.open) {
            forceHideRecipeButtonIfSkillsOpen(st);
        } else {
            restoreRecipeButtonIfWeHidIt(inv, st);
        }
    }

    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre e) {
        Screen s = e.getScreen();
        State st = STATES.get(s);
        if (st == null || !(s instanceof InventoryScreen inv)) return;
        if (!st.open) return;

        double mx = e.getMouseX();
        double my = e.getMouseY();
        boolean used = false;

        if (st.list != null && st.list.visible) {
            if (mx >= st.list.getX() && mx <= st.list.getX() + st.list.getWidth()
                    && my >= st.list.getY() && my <= st.list.getY() + st.list.getHeight()) {
                double dY = e.getScrollDeltaY();
                used = st.list.mouseScrolled(mx, my, dY) || st.list.mouseScrolled(mx, my, 0.0, dY);
            }
        }

        if (st.details != null && st.details.visible) {
            if (mx >= st.details.getX() && mx <= st.details.getX() + st.details.getWidth()
                    && my >= st.details.getY() && my <= st.details.getY() + st.details.getHeight()) {
                double dY = e.getScrollDeltaY();
                used = st.details.mouseScrolled(mx, my, dY) || st.details.mouseScrolled(mx, my, 0.0, dY) || used;
            }
        }

        if (used) e.setCanceled(true);
    }

    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre e) {
        Screen s = e.getScreen();
        State st = STATES.get(s);
        if (st == null || !(s instanceof InventoryScreen)) return;
        if (!st.open || st.list == null || !st.list.visible || !st.list.active) return;

        double mx = e.getMouseX();
        double my = e.getMouseY();
        boolean insideList = mx >= st.list.getX() && mx <= st.list.getX() + st.list.getWidth()
                && my >= st.list.getY() && my <= st.list.getY() + st.list.getHeight();
        if (!insideList) return;

        boolean used = st.list.mouseDragged(mx, my, e.getMouseButton(), e.getDragX(), e.getDragY());
        if (used) e.setCanceled(true);
    }

    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre e) {
        Screen s = e.getScreen();
        State st = STATES.get(s);
        if (st == null || !(s instanceof InventoryScreen)) return;
        if (!st.open || st.list == null) return;

        st.list.mouseReleased(e.getMouseX(), e.getMouseY(), e.getButton());
    }

    private static Iterable<SkillDefinition> allSkills() {
        return () -> net.revilodev.codex.skills.SkillId.values().length == 0
                ? java.util.Collections.<SkillDefinition>emptyIterator()
                : new java.util.Iterator<>() {
            private final net.revilodev.codex.skills.SkillId[] ids = net.revilodev.codex.skills.SkillId.values();
            private int i = 0;

            @Override public boolean hasNext() { return i < ids.length; }
            @Override public SkillDefinition next() { return SkillRegistry.def(ids[i++]); }
        };
    }

    private static SkillDefinition firstSkill() {
        SkillId[] ids = SkillId.values();
        if (ids.length == 0) return null;
        return SkillRegistry.def(ids[0]);
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

        if (st.open) {
            forceHideRecipeButtonIfSkillsOpen(st);
        } else {
            restoreRecipeButtonIfWeHidIt(st.inv, st);
        }
    }

    // Keep your old behavior: if vanilla recipe panel is open (left shifted), hide skills toggle.
    // This does NOT touch the recipe button.
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

    // ---- recipe button control (green book) ----

    private static void forceHideRecipeButtonIfSkillsOpen(State st) {
        if (st.recipeBtn == null) return;
        st.recipeBtn.visible = false;
        st.recipeBtn.active = false;
        st.recipeHiddenBySkills = true;
    }

    private static void restoreRecipeButtonIfWeHidIt(InventoryScreen inv, State st) {
        if (!st.recipeHiddenBySkills) return;

        // If quest panel is open, DO NOT restore here; let QuestPanelClient continue hiding/showing.
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
            if (child instanceof ImageButton btn) {
                if (btn.getWidth() == 20 && btn.getHeight() == 18) {
                    return btn;
                }
            }
        }
        return null;
    }

    // Detect quest panel open without referencing Boundless classes directly (no hard dependency).
    // Your debug shows QuestPanelClient$PanelBackground exists and is visible=true when quest panel is open.
    private static boolean isQuestPanelOpen(InventoryScreen inv) {
        for (var child : inv.children()) {
            if (child instanceof AbstractWidget w) {
                String n = child.getClass().getName();
                if (n.equals("net.revilodev.boundless.client.QuestPanelClient$PanelBackground") && w.visible) {
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

    // ---- layout ----

    private static int computeCenteredLeft(InventoryScreen inv) {
        int screenW = inv.width;
        int invW = inv.getXSize();
        int total = PANEL_W + 2 + invW;
        return (screenW - total) / 2 + PANEL_W + 2;
    }

    private static int computePanelX(InventoryScreen inv) {
        return inv.getGuiLeft() - PANEL_W - 2;
    }

    private static void setPanelChildBounds(InventoryScreen inv, State st) {
        int bgx = computePanelX(inv);
        int bgy = inv.getGuiTop();
        int innerLeft = bgx + INNER_PAD_X;
        int innerRight = bgx + PANEL_W - INNER_PAD_X;
        int innerTop = bgy + INNER_PAD_TOP;
        int innerBottom = bgy + PANEL_H - INNER_PAD_BOTTOM;

        int listW = SkillListWidget.gridWidth();
        int listX = bgx + (PANEL_W - listW) / 2;
        int listY = innerTop;
        int listH = SkillListWidget.preferredHeight();

        int detailsX = innerLeft;
        int detailsY = listY + listH + SECTION_GAP;
        int detailsW = innerRight - innerLeft;
        int detailsH = Math.max(20, innerBottom - detailsY);

        if (st.bg != null) st.bg.setBounds(bgx, bgy, PANEL_W, PANEL_H);
        if (st.list != null) st.list.setBounds(listX, listY, listW, listH);
        if (st.details != null) st.details.setBounds(detailsX, detailsY, detailsW, detailsH);
    }

    private static void reposition(InventoryScreen inv, State st) {
        if (st.btn != null) {
            int x = inv.getGuiLeft() + 145;
            int y = inv.getGuiTop() + 61;
            st.btn.setPosition(x, y);
        }
        setPanelChildBounds(inv, st);
    }

    // ---- leftPos reflection ----

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
        if (st.bg != null) {
            st.bg.visible = st.open;
            st.bg.active = st.open;
        }

        if (st.list != null) {
            st.list.visible = st.open;
            st.list.active = st.open;
        }

        if (st.details != null) {
            st.details.visible = st.open;
            st.details.active = st.open;
            st.details.backButton().visible = false;
            st.details.backButton().active = false;
            st.details.upgradeButton().visible = st.open;
            st.details.upgradeButton().active = st.open;
            st.details.downgradeButton().visible = st.open;
            st.details.downgradeButton().active = st.open;
        }
    }

    private static final class PanelBackground extends AbstractWidget {
        public PanelBackground(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty());
        }

        public void setBounds(int x, int y, int w, int h) {
            this.setX(x);
            this.setY(y);
            this.width = w;
            this.height = h;
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            RenderSystem.disableBlend();
            gg.blit(PANEL_TEX, getX(), getY(), 0, 0, width, height, width, height);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private static final class State {
        final InventoryScreen inv;
        SkillsToggleButton btn;
        PanelBackground bg;
        SkillListWidget list;
        SkillDetailsPanel details;

        ImageButton recipeBtn;
        boolean recipeHiddenBySkills;

        boolean open;
        Integer originalLeft;

        State(InventoryScreen inv) {
            this.inv = inv;
        }
    }
}
