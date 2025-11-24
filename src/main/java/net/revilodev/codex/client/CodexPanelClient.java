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
        st.btn = new CodexToggleButton(
                inv.getGuiLeft() + 145,
                inv.getGuiTop() + 61,
                BTN_TEX, BTN_TEX_HOVER,
                () -> toggle(st)
        );
        e.addListener(st.btn);

        // Background
        st.bg = new PanelBackground(0, 0, PANEL_W, PANEL_H);
        e.addListener(st.bg);

        // List
        st.list = new CodexListWidget(
                0, 0, 127, PANEL_H - 20,
                cat -> openCategory(st, cat),
                ch -> openDetails(st, ch)
        );
        st.list.showCategories(GuideData.allCategories());
        e.addListener(st.list);

        // Details
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
    // Render Events
    // ---------------------------------------------------------------------

    public static void onScreenRenderPre(ScreenEvent.Render.Pre e) {
        Screen s = e.getScreen();
        State st = STATES.get(s);
        if (st == null || !(s instanceof InventoryScreen inv)) return;

        st.btn.setTextures(BTN_TEX, BTN_TEX_HOVER);

        if (st.open)
            setLeft(inv, computeCenteredLeft(inv));

        reposition(inv, st);
        updateVisibility(st);

        handleRecipeBook(inv, st);
    }

    public static void onScreenRenderPost(ScreenEvent.Render.Post e) {}

    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre e) {
        State st = STATES.get(e.getScreen());
        if (st == null || !(st.inv instanceof InventoryScreen inv)) return;
        if (!st.open) return;

        double mx = e.getMouseX();
        double my = e.getMouseY();
        double dy = e.getScrollDeltaY();

        int px = computePanelX(inv) + 10;
        int py = inv.getGuiTop() + 10;
        int pw = 127;
        int ph = PANEL_H - 20;

        boolean used = false;

        if (st.list.visible &&
                mx >= px && mx <= px + pw && my >= py && my <= py + ph) {
            used = st.list.mouseScrolled(mx, my, dy);
        }

        if (st.details.visible &&
                mx >= px && mx <= px + pw && my >= py && my <= py + ph) {
            used = st.details.mouseScrolled(mx, my, dy) || used;
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
            if (st.originalLeft == null)
                st.originalLeft = getLeft(st.inv);
            setLeft(st.inv, computeCenteredLeft(st.inv));
        } else {
            if (st.originalLeft != null)
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

    private static void openDetails(State st, GuideData.Chapter ch) {
        st.details.setChapter(ch);
        st.showingDetails = true;
        updateVisibility(st);
    }

    private static void closeDetails(State st) {
        st.showingDetails = false;

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
    // Recipe book control (non-destructive)
    // ---------------------------------------------------------------------

    private static void handleRecipeBook(InventoryScreen inv, State st) {

        // If Codex panel is open → hide recipe book button + book panel
        if (st.open) {

            for (var child : inv.children()) {

                // Hide the recipe button (20x18 is the known size)
                if (child instanceof ImageButton btn &&
                        btn.getWidth() == 20 && btn.getHeight() == 18) {
                    btn.visible = false;
                }

                // Hide the recipe book GUI panel (the book sprite)
                if (child.getClass().getName().contains("RecipeBookComponent")) {
                    try {
                        child.getClass().getMethod("setVisible", boolean.class).invoke(child, false);
                    } catch (Exception ignored) {}
                }
            }

            return;
        }

        // Codex closed → show recipe UI normally
        for (var child : inv.children()) {

            if (child instanceof ImageButton btn &&
                    btn.getWidth() == 20 && btn.getHeight() == 18) {
                btn.visible = true;
            }

            if (child.getClass().getName().contains("RecipeBookComponent")) {
                try {
                    child.getClass().getMethod("setVisible", boolean.class).invoke(child, true);
                } catch (Exception ignored) {}
            }
        }
    }


    // ---------------------------------------------------------------------
    // Layout
    // ---------------------------------------------------------------------

    private static int computeCenteredLeft(InventoryScreen inv) {
        return (inv.width - (PANEL_W + 2 + inv.getXSize())) / 2 + PANEL_W + 2;
    }

    private static int computePanelX(InventoryScreen inv) {
        return inv.getGuiLeft() - PANEL_W - 2;
    }

    private static void reposition(InventoryScreen inv, State st) {
        st.btn.setX(inv.getGuiLeft() + 145);
        st.btn.setY(inv.getGuiTop() + 61);

        int bgx = computePanelX(inv);
        int bgy = inv.getGuiTop();

        int px = bgx + 10;
        int py = bgy + 10;
        int pw = 127;
        int ph = PANEL_H - 20;

        st.bg.setBounds(bgx, bgy, PANEL_W, PANEL_H);
        st.list.setBounds(px, py, pw, ph);
        st.details.setBounds(px, py, pw, ph);

        st.details.backButton().setPosition(px + 2,
                py + ph - st.details.backButton().getHeight() - 4);
    }

    // ---------------------------------------------------------------------
    // Reflection helpers
    // ---------------------------------------------------------------------

    private static Integer getLeft(InventoryScreen inv) {
        try {
            if (LEFT_FIELD == null)
                LEFT_FIELD = findLeftField(inv.getClass());
            return (Integer) LEFT_FIELD.get(inv);
        } catch (Throwable ignored) {
            return inv.getGuiLeft();
        }
    }

    private static void setLeft(InventoryScreen inv, int v) {
        try {
            if (LEFT_FIELD == null)
                LEFT_FIELD = findLeftField(inv.getClass());
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

        GuideData.Category currentCategory = null;
        Integer originalLeft = null;

        State(InventoryScreen inv) {
            this.inv = inv;
        }
    }

    // ---------------------------------------------------------------------
    // Panel Background
    // ---------------------------------------------------------------------

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
