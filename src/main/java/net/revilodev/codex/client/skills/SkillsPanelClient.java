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
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.skills.SkillDefinition;
import net.revilodev.codex.skills.SkillRegistry;
import net.revilodev.codex.client.skills.SkillsToggleButton;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

    private static final Map<Screen, State> STATES = new WeakHashMap<>();
    private static Field LEFT_FIELD;
    private static boolean lastOpen = false;

    // recipe book component (to actually hide the panel, not just the button)
    private static Field RECIPE_BOOK_FIELD;
    private static Method RECIPE_SET_VISIBLE;
    private static Field RECIPE_VISIBLE_FIELD;

    private SkillsPanelClient() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SkillsPanelClient::onScreenInit);
        NeoForge.EVENT_BUS.addListener(SkillsPanelClient::onScreenClosing);
        NeoForge.EVENT_BUS.addListener(SkillsPanelClient::onScreenRenderPre);
        NeoForge.EVENT_BUS.addListener(SkillsPanelClient::onScreenRenderPost);
        NeoForge.EVENT_BUS.addListener(SkillsPanelClient::onMouseScrolled);
    }

    public static void onScreenInit(ScreenEvent.Init.Post e) {
        Screen s = e.getScreen();
        if (!(s instanceof InventoryScreen inv)) return;

        State st = new State(inv);
        STATES.put(s, st);

        // keep the previously working offset (+20 right), only move down +1px
        int btnX = inv.getGuiLeft() + 145;
        int btnY = inv.getGuiTop() + 61;

        SkillsToggleButton btn = new SkillsToggleButton(btnX, btnY, BTN_TEX, BTN_TEX_HOVER, () -> toggle(st));
        st.btn = btn;

        st.bg = new PanelBackground(0, 0, PANEL_W, PANEL_H);
        e.addListener(st.bg);

        st.list = new SkillListWidget(0, 0, 127, PANEL_H - 20, def -> openDetails(st, def));
        st.list.setSkills(allSkills());
        st.list.setCategory(st.selectedCategory);
        e.addListener(st.list);

        st.details = new SkillDetailsPanel(0, 0, 127, PANEL_H - 20, () -> closeDetails(st));
        e.addListener(st.details);
        e.addListener(st.details.backButton());
        e.addListener(st.details.upgradeButton());
        e.addListener(st.details.downgradeButton());

        st.tabs = new SkillCategoryTabsWidget(0, 0, 26, PANEL_H, id -> {
            st.selectedCategory = id;
            if (st.list != null) st.list.setCategory(id);
        });
        st.tabs.setCategories(SkillRegistry.categoriesOrdered());
        st.tabs.setSelected(st.selectedCategory);
        e.addListener(st.tabs);

        e.addListener(btn);

        reposition(inv, st);

        if (lastOpen) {
            st.open = true;
            st.originalLeft = getLeft(inv);
            setLeft(inv, computeCenteredLeft(inv));
            updateVisibility(st);
            handleRecipeButtonRules(inv, st); // ensures recipe book is hidden immediately
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
        handleRecipeButtonRules(inv, st);
    }

    public static void onScreenRenderPost(ScreenEvent.Render.Post e) {
        State st = STATES.get(e.getScreen());
        if (st == null) return;
        if (st.tabs != null && st.tabs.visible) st.tabs.renderHoverTooltipOnTop(e.getGuiGraphics());
    }

    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre e) {
        Screen s = e.getScreen();
        State st = STATES.get(s);
        if (st == null || !(s instanceof InventoryScreen inv)) return;
        if (!st.open) return;

        int px = computePanelX(inv) + 10;
        int py = inv.getGuiTop() + 10;
        int pw = 127;
        int ph = PANEL_H - 20;

        double mx = e.getMouseX();
        double my = e.getMouseY();
        boolean used = false;

        if (st.list != null && st.list.visible) {
            if (mx >= px && mx <= px + pw && my >= py && my <= py + ph) {
                double dY = e.getScrollDeltaY();
                used = st.list.mouseScrolled(mx, my, dY) || st.list.mouseScrolled(mx, my, 0.0, dY);
            }
        }

        if (st.details != null && st.details.visible) {
            if (mx >= px && mx <= px + pw && my >= py && my <= py + ph) {
                double dY = e.getScrollDeltaY();
                used = st.details.mouseScrolled(mx, my, dY) || st.details.mouseScrolled(mx, my, 0.0, dY) || used;
            }
        }

        if (used) e.setCanceled(true);
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

    private static void toggle(State st) {
        st.open = !st.open;
        lastOpen = st.open;

        if (st.open) {
            // CLOSE + HIDE recipe book while skills panel is open
            setRecipeBookVisible(st.inv, false);

            if (st.originalLeft == null) st.originalLeft = getLeft(st.inv);
            setLeft(st.inv, computeCenteredLeft(st.inv));
        } else if (st.originalLeft != null) {
            setLeft(st.inv, st.originalLeft);
        }

        reposition(st.inv, st);
        updateVisibility(st);
        handleRecipeButtonRules(st.inv, st);
    }

    private static int computeCenteredLeft(InventoryScreen inv) {
        int screenW = inv.width;
        int invW = inv.getXSize();
        int total = PANEL_W + 2 + invW;
        return (screenW - total) / 2 + PANEL_W + 2;
    }

    private static int computePanelX(InventoryScreen inv) {
        return inv.getGuiLeft() - PANEL_W - 2;
    }

    private static int computeTabsX(InventoryScreen inv) {
        return computePanelX(inv) - 23;
    }

    private static void setPanelChildBounds(InventoryScreen inv, State st) {
        int bgx = computePanelX(inv);
        int bgy = inv.getGuiTop();
        int px = bgx + 10;
        int py = bgy + 10;
        int pw = 127;
        int ph = PANEL_H - 20;

        if (st.bg != null) st.bg.setBounds(bgx, bgy, PANEL_W, PANEL_H);

        if (st.list != null) st.list.setBounds(px, py, pw, ph);

        if (st.details != null) {
            st.details.setBounds(px, py, pw, ph);

            st.details.backButton().setPosition(px + 2, py + ph - st.details.backButton().getHeight() - 4);
            st.details.upgradeButton().setPosition(
                    px + (pw - st.details.upgradeButton().getWidth()) / 2,
                    py + ph - st.details.upgradeButton().getHeight() - 4
            );
            st.details.downgradeButton().setPosition(
                    px + pw - st.details.downgradeButton().getWidth() - 2,
                    py + ph - st.details.downgradeButton().getHeight() - 4
            );
        }

        if (st.tabs != null) st.tabs.setBounds(computeTabsX(inv), bgy + 4, 26, PANEL_H - 8);
    }

    private static void reposition(InventoryScreen inv, State st) {
        if (st.btn != null) {
            int x = inv.getGuiLeft() + 145;
            int y = inv.getGuiTop() + 61;
            st.btn.setPosition(x, y);
        }
        setPanelChildBounds(inv, st);
    }

    // same rules as your working QuestPanelClient, plus: actually hide the recipe book panel when skills panel is open
    private static void handleRecipeButtonRules(InventoryScreen inv, State st) {
        if (st.open) {
            if (st.btn != null) st.btn.visible = true;

            // hide the vanilla recipe toggle button
            toggleRecipeButtonVisibility(inv, false);

            // hide the actual recipe book panel (this is what prevents overlap)
            setRecipeBookVisible(inv, false);

        } else if (isRecipePanelOpen(inv)) {
            // recipe book is open -> hide skills toggle (as requested)
            if (st.btn != null) st.btn.visible = false;
            toggleRecipeButtonVisibility(inv, true);
        } else {
            if (st.btn != null) st.btn.visible = true;
            toggleRecipeButtonVisibility(inv, true);
        }
    }

    private static void toggleRecipeButtonVisibility(InventoryScreen inv, boolean visible) {
        for (var child : inv.children()) {
            if (child instanceof ImageButton btn) {
                if (btn.getWidth() == 20 && btn.getHeight() == 18) {
                    btn.visible = visible;
                }
            }
        }
    }

    private static boolean isRecipePanelOpen(InventoryScreen inv) {
        int centeredLeft = (inv.width - inv.getXSize()) / 2;
        return inv.getGuiLeft() > centeredLeft + 10;
    }

    // ---- recipe book panel visibility (minimal + safe reflection) ----

    private static void setRecipeBookVisible(InventoryScreen inv, boolean visible) {
        try {
            Object comp = getRecipeBookComponent(inv);
            if (comp == null) return;

            if (RECIPE_SET_VISIBLE == null) {
                RECIPE_SET_VISIBLE = findMethod(comp.getClass(), "setVisible", boolean.class);
            }
            if (RECIPE_SET_VISIBLE != null) {
                RECIPE_SET_VISIBLE.invoke(comp, visible);
                return;
            }

            if (RECIPE_VISIBLE_FIELD == null) {
                RECIPE_VISIBLE_FIELD = findBooleanField(comp.getClass(), "visible");
                if (RECIPE_VISIBLE_FIELD != null) RECIPE_VISIBLE_FIELD.setAccessible(true);
            }
            if (RECIPE_VISIBLE_FIELD != null) {
                RECIPE_VISIBLE_FIELD.setBoolean(comp, visible);
            }
        } catch (Throwable ignored) {}
    }

    private static Object getRecipeBookComponent(InventoryScreen inv) {
        try {
            if (RECIPE_BOOK_FIELD == null) {
                RECIPE_BOOK_FIELD = findRecipeBookField(inv.getClass());
                if (RECIPE_BOOK_FIELD != null) RECIPE_BOOK_FIELD.setAccessible(true);
            }
            return RECIPE_BOOK_FIELD != null ? RECIPE_BOOK_FIELD.get(inv) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Field findRecipeBookField(Class<?> c) {
        Class<?> cur = c;
        while (cur != null) {
            for (Field f : cur.getDeclaredFields()) {
                String tn = f.getType().getName();
                if (tn.endsWith("RecipeBookComponent") || tn.contains("RecipeBookComponent")) {
                    return f;
                }
            }
            cur = cur.getSuperclass();
        }
        return null;
    }

    private static Method findMethod(Class<?> c, String name, Class<?>... params) {
        Class<?> cur = c;
        while (cur != null) {
            try {
                Method m = cur.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {}
            cur = cur.getSuperclass();
        }
        return null;
    }

    private static Field findBooleanField(Class<?> c, String name) {
        Class<?> cur = c;
        while (cur != null) {
            try {
                Field f = cur.getDeclaredField(name);
                if (f.getType() == boolean.class) return f;
            } catch (NoSuchFieldException ignored) {}
            cur = cur.getSuperclass();
        }
        return null;
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

    private static void openDetails(State st, SkillDefinition def) {
        if (st.details == null || def == null) return;
        st.details.setSkill(def);
        st.showingDetails = true;
        updateVisibility(st);
    }

    private static void closeDetails(State st) {
        st.showingDetails = false;
        updateVisibility(st);
    }

    private static void updateVisibility(State st) {
        boolean listVisible = st.open && !st.showingDetails;
        boolean detailsVisible = st.open && st.showingDetails;

        if (st.bg != null) {
            st.bg.visible = st.open;
            st.bg.active = st.open;
        }

        if (st.list != null) {
            st.list.visible = listVisible;
            st.list.active = listVisible;
        }

        if (st.details != null) {
            st.details.visible = detailsVisible;
            st.details.active = detailsVisible;

            st.details.backButton().visible = detailsVisible;
            st.details.backButton().active = detailsVisible;

            st.details.upgradeButton().visible = detailsVisible;
            st.details.upgradeButton().active = detailsVisible;

            st.details.downgradeButton().visible = detailsVisible;
            st.details.downgradeButton().active = detailsVisible;
        }

        if (st.tabs != null) {
            st.tabs.visible = st.open;
            st.tabs.active = st.open;
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
        SkillCategoryTabsWidget tabs;

        boolean showingDetails;
        boolean open;
        Integer originalLeft;
        String selectedCategory = "combat";

        State(InventoryScreen inv) {
            this.inv = inv;
        }
    }
}
