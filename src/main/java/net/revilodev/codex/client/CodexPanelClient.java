package net.revilodev.codex.client;

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
import net.revilodev.codex.data.GuideData;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class CodexPanelClient {

    private static final ResourceLocation BTN_TEX =
            ResourceLocation.fromNamespaceAndPath("codex", "textures/gui/sprites/book_button.png");
    private static final ResourceLocation BTN_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath("codex", "textures/gui/sprites/book_button_hovered.png");

    private static final ResourceLocation PANEL_TEX =
            ResourceLocation.fromNamespaceAndPath("codex", "textures/gui/panel.png");

    private static final int PANEL_W = 147;
    private static final int PANEL_H = 166;

    private static final Map<Screen, State> STATES = new WeakHashMap<>();
    private static Field LEFT_FIELD;
    private static boolean LAST_OPEN = false;

    private CodexPanelClient() {}

    // ---------------------------------------------------------------------
    // Initialization
    // ---------------------------------------------------------------------

    public static void onScreenInit(ScreenEvent.Init.Post e) {
        Screen s = e.getScreen();
        if (!(s instanceof InventoryScreen inv)) return;

        GuideData.loadClient(false);

        State st = new State(inv);
        STATES.put(s, st);

        // Toggle button
        int btnX = inv.getGuiLeft() + 145;
        int btnY = inv.getGuiTop() + 61;
        st.btn = new CodexToggleButton(btnX, btnY, BTN_TEX, BTN_TEX_HOVER, () -> toggle(st));
        e.addListener(st.btn);

        // Background
        st.bg = new PanelBackground(0, 0, PANEL_W, PANEL_H);
        e.addListener(st.bg);

        // CATEGORY + CHAPTER LIST
        st.list = new CodexListWidget(
                0, 0, 127, PANEL_H - 20,
                category -> openCategory(st, category),
                chapter -> openDetails(st, chapter)
        );
        st.list.showCategories(GuideData.allCategories());
        e.addListener(st.list);

        // DETAILS PANEL
        st.details = new CodexDetailsPanel(0, 0, 127, PANEL_H - 20, () -> closeDetails(st));
        e.addListener(st.details);
        e.addListener(st.details.backButton());

        reposition(inv, st);

        if (LAST_OPEN) {
            st.open = true;
            st.originalLeft = getLeft(inv);
            setLeft(inv, computeCenteredLeft(inv));
            updateVisibility(st);
        }
    }

    public static void onScreenClosing(ScreenEvent.Closing e) {
        State st = STATES.remove(e.getScreen());
        if (st != null && st.open && st.originalLeft != null) {
            setLeft(st.inv, st.originalLeft);
        }
    }

    // ---------------------------------------------------------------------
    // Render
    // ---------------------------------------------------------------------

    public static void onScreenRenderPre(ScreenEvent.Render.Pre e) {
        Screen s = e.getScreen();
        State st = STATES.get(s);
        if (st == null || !(s instanceof InventoryScreen inv)) return;

        if (st.btn != null) st.btn.setTextures(BTN_TEX, BTN_TEX_HOVER);

        if (st.open) setLeft(inv, computeCenteredLeft(inv));

        reposition(inv, st);
        updateVisibility(st);
        handleRecipeButtonRules(inv, st);
    }

    public static void onScreenRenderPost(ScreenEvent.Render.Post e) {}

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
        double dy = e.getScrollDeltaY();

        boolean used = false;

        if (st.list.visible) {
            if (mx >= px && mx <= px + pw && my >= py && my <= py + ph) {
                used = st.list.mouseScrolled(mx, my, dy) || st.list.mouseScrolled(mx, my, 0.0, dy);
            }
        }
        if (st.details.visible) {
            if (mx >= px && mx <= px + pw && my >= py && my <= py + ph) {
                used = st.details.mouseScrolled(mx, my, dy) || st.details.mouseScrolled(mx, my, 0.0, dy) || used;
            }
        }

        if (used) e.setCanceled(true);
    }

    // ---------------------------------------------------------------------
    // Behavior
    // ---------------------------------------------------------------------

    private static void toggle(State st) {
        st.open = !st.open;
        LAST_OPEN = st.open;

        if (st.open) {
            if (st.originalLeft == null) st.originalLeft = getLeft(st.inv);
            setLeft(st.inv, computeCenteredLeft(st.inv));
        } else if (st.originalLeft != null) {
            setLeft(st.inv, st.originalLeft);
        }

        updateVisibility(st);
    }

    private static void openCategory(State st, GuideData.Category cat) {
        st.currentCategory = cat;
        st.showingChapters = true;
        st.list.showChapters(GuideData.chaptersInCategory(cat.id));
        updateVisibility(st);
    }

    private static void openDetails(State st, GuideData.Chapter chapter) {
        st.details.setChapter(chapter);
        st.showingDetails = true;
        updateVisibility(st);
    }

    private static void closeDetails(State st) {
        st.showingDetails = false;

        // Return to chapters list
        if (st.currentCategory != null) {
            st.list.showChapters(GuideData.chaptersInCategory(st.currentCategory.id));
            st.showingChapters = true;
        }

        updateVisibility(st);
    }

    private static void updateVisibility(State st) {
        boolean listVisible = st.open && !st.showingDetails;
        boolean detailsVisible = st.open && st.showingDetails;

        st.bg.visible = st.open;

        st.list.visible = listVisible;
        st.list.active = listVisible;

        st.details.visible = detailsVisible;
        st.details.active = detailsVisible;
        st.details.backButton().visible = detailsVisible;
        st.details.backButton().active = detailsVisible;
    }

    // ---------------------------------------------------------------------
    // Layout
    // ---------------------------------------------------------------------

    private static int computeCenteredLeft(InventoryScreen inv) {
        int screenW = inv.width;
        int invW = inv.getXSize();
        int totalW = PANEL_W + 2 + invW;
        return (screenW - totalW) / 2 + PANEL_W + 2;
    }

    private static int computePanelX(InventoryScreen inv) {
        return inv.getGuiLeft() - PANEL_W - 2;
    }

    private static void setPanelChildBounds(InventoryScreen inv, State st) {
        int bgx = computePanelX(inv);
        int bgy = inv.getGuiTop();

        int px = bgx + 10;
        int py = bgy + 10;
        int pw = 127;
        int ph = PANEL_H - 20;

        st.bg.setBounds(bgx, bgy, PANEL_W, PANEL_H);
        st.list.setBounds(px, py, pw, ph);
        st.details.setBounds(px, py, pw, ph);

        st.details.backButton().setPosition(px + 2, py + ph - st.details.backButton().getHeight() - 4);
    }

    private static void reposition(InventoryScreen inv, State st) {
        if (st.btn != null) {
            st.btn.setPosition(inv.getGuiLeft() + 145, inv.getGuiTop() + 61);
        }
        setPanelChildBounds(inv, st);
    }

    // ---------------------------------------------------------------------
    // Recipe book button hiding
    // ---------------------------------------------------------------------

    private static void handleRecipeButtonRules(InventoryScreen inv, State st) {
        if (st.open) {
            toggleRecipeButtons(inv, false);
        } else if (isRecipePanelOpen(inv)) {
            toggleRecipeButtons(inv, true);
            if (st.btn != null) st.btn.visible = false;
        } else {
            toggleRecipeButtons(inv, true);
            if (st.btn != null) st.btn.visible = true;
        }
    }

    private static void toggleRecipeButtons(InventoryScreen inv, boolean visible) {
        for (var child : inv.children()) {
            if (child instanceof ImageButton btn) {
                if (btn.getWidth() == 20 && btn.getHeight() == 18) btn.visible = visible;
            }
        }
    }

    private static boolean isRecipePanelOpen(InventoryScreen inv) {
        int mid = (inv.width - inv.getXSize()) / 2;
        return inv.getGuiLeft() > mid + 10;
    }

    // ---------------------------------------------------------------------
    // Reflection helpers
    // ---------------------------------------------------------------------

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
        while (c != null) {
            try {
                Field f = c.getDeclaredField("leftPos");
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException("leftPos");
    }

    // ---------------------------------------------------------------------
    // Internal State
    // ---------------------------------------------------------------------

    private static final class State {
        final InventoryScreen inv;

        CodexToggleButton btn;
        PanelBackground bg;
        CodexListWidget list;
        CodexDetailsPanel details;

        boolean open = false;
        boolean showingDetails = false;
        boolean showingChapters = false;

        GuideData.Category currentCategory;
        Integer originalLeft;

        State(InventoryScreen inv) {
            this.inv = inv;
        }
    }

    // ---------------------------------------------------------------------
    // Background Widget
    // ---------------------------------------------------------------------

    private static final class PanelBackground extends AbstractWidget {

        public PanelBackground(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty());
        }

        public void setBounds(int x, int y, int w, int h) {
            setX(x);
            setY(y);
            width = w;
            height = h;
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float pt) {
            RenderSystem.disableBlend();
            gg.blit(PANEL_TEX, getX(), getY(), 0, 0, width, height, width, height);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int b) { return false; }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput n) {}
    }
}
