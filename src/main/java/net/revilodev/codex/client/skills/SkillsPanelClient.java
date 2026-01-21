package net.revilodev.codex.client.skills;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.client.gui.components.Button;
import net.revilodev.codex.skills.SkillCategory;
import net.revilodev.codex.skills.SkillDefinition;
import net.revilodev.codex.skills.SkillRegistry;

import java.lang.reflect.Field;
import java.util.WeakHashMap;

public final class SkillsPanelClient {
    private SkillsPanelClient() {}

    private static final int PANEL_W = 170;
    private static final int PANEL_H = 166;

    private static final WeakHashMap<InventoryScreen, State> STATES = new WeakHashMap<>();

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SkillsPanelClient::onScreenInit);
        NeoForge.EVENT_BUS.addListener(SkillsPanelClient::onScreenRenderPre);
        NeoForge.EVENT_BUS.addListener(SkillsPanelClient::onScreenRenderPost);
        NeoForge.EVENT_BUS.addListener(SkillsPanelClient::onScreenClose);
        NeoForge.EVENT_BUS.addListener(SkillsPanelClient::onMouseScrolled);
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;

        State state = new State(screen);
        STATES.put(screen, state);

        int left = getLeftPos(screen);
        int top = getTopPos(screen);

        state.open = false;

        state.toggle = Button.builder(Component.literal("Skills"), b -> {
            state.open = !state.open;
            layout(screen, state);
        }).bounds(left + 205, top + 4, 44, 16).build();

        state.tabs = new SkillCategoryTabsWidget(0, 0, PANEL_W - 12, 22, cat -> {
            state.category = cat;
            state.selected = null;
            state.list.setEntries(SkillRegistry.skillsFor(cat));
            state.details.setSelected(null);
        });

        state.list = new SkillListWidget(0, 0, PANEL_W - 12, 88, def -> {
            state.selected = def;
            state.details.setSelected(def);
        });

        state.details = new SkillDetailsPanel(0, 0, PANEL_W - 12, 52, () -> state.category, () -> state.selected);

        state.list.setEntries(SkillRegistry.skillsFor(state.category));
        state.tabs.setSelected(state.category);

        event.addListener(state.toggle);
        event.addListener(state.tabs);
        event.addListener(state.list);
        event.addListener(state.details);

        layout(screen, state);
    }

    private static void onScreenClose(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof InventoryScreen screen) STATES.remove(screen);
    }

    private static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        State state = STATES.get(screen);
        if (state == null || !state.open) return;

        if (state.list.isMouseOver(event.getMouseX(), event.getMouseY())) {
            if (state.list.mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDeltaX(), event.getScrollDeltaY())) {
                event.setCanceled(true);
            }
        }
    }

    private static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        State state = STATES.get(screen);
        if (state == null) return;
        layout(screen, state);
    }

    private static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        State state = STATES.get(screen);
        if (state == null || !state.open) return;

        int left = getLeftPos(screen);
        int top = getTopPos(screen);
        int px = left - PANEL_W;
        int py = top;

        event.getGuiGraphics().fill(px, py, px + PANEL_W, py + PANEL_H, 0xAA000000);
        event.getGuiGraphics().drawString(Minecraft.getInstance().font, Component.literal("Skills"), px + 8, py + 8, 0xFFFFFF, false);

        state.tabs.renderHoverTooltipOnTop(event.getGuiGraphics());
    }

    private static void layout(InventoryScreen screen, State state) {
        int invW = screen.getXSize();
        int invH = screen.getYSize();

        int baseLeftClosed = (screen.width - invW) / 2;
        int leftOpen = (screen.width - (invW + PANEL_W)) / 2 + PANEL_W;
        int top = (screen.height - invH) / 2;

        int left = state.open ? leftOpen : baseLeftClosed;
        setLeftPos(screen, left);
        setTopPos(screen, top);

        state.toggle.setX(left + 205);
        state.toggle.setY(top + 4);

        int px = left - PANEL_W;
        int py = top;

        state.tabs.setPosition(px + 6, py + 18);
        state.list.setPosition(px + 6, py + 44);
        state.details.setPosition(px + 6, py + 134);

        state.toggle.visible = true;
        state.tabs.visible = state.open;
        state.list.visible = state.open;
        state.details.visible = state.open;

        state.tabs.active = state.open;
        state.list.active = state.open;
        state.details.active = state.open;
    }

    private static int getLeftPos(AbstractContainerScreen<?> s) {
        try {
            Field f = AbstractContainerScreen.class.getDeclaredField("leftPos");
            f.setAccessible(true);
            return (int) f.get(s);
        } catch (Throwable t) {
            return 0;
        }
    }

    private static int getTopPos(AbstractContainerScreen<?> s) {
        try {
            Field f = AbstractContainerScreen.class.getDeclaredField("topPos");
            f.setAccessible(true);
            return (int) f.get(s);
        } catch (Throwable t) {
            return 0;
        }
    }

    private static void setLeftPos(AbstractContainerScreen<?> s, int v) {
        try {
            Field f = AbstractContainerScreen.class.getDeclaredField("leftPos");
            f.setAccessible(true);
            f.set(s, v);
        } catch (Throwable ignored) {}
    }

    private static void setTopPos(AbstractContainerScreen<?> s, int v) {
        try {
            Field f = AbstractContainerScreen.class.getDeclaredField("topPos");
            f.setAccessible(true);
            f.set(s, v);
        } catch (Throwable ignored) {}
    }

    private static final class State {
        final InventoryScreen screen;
        boolean open;
        SkillCategory category = SkillCategory.COMBAT;
        SkillDefinition selected;

        Button toggle;
        SkillCategoryTabsWidget tabs;
        SkillListWidget list;
        SkillDetailsPanel details;

        State(InventoryScreen screen) {
            this.screen = screen;
        }
    }
}
