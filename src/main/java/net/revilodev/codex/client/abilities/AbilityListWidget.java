package net.revilodev.codex.client.abilities;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.abilities.AbilitiesAttachments;
import net.revilodev.codex.abilities.AbilityElement;
import net.revilodev.codex.abilities.AbilityDefinition;
import net.revilodev.codex.abilities.AbilityId;
import net.revilodev.codex.abilities.AbilityRegistry;
import net.revilodev.codex.abilities.PlayerAbilities;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class AbilityListWidget extends AbstractWidget {
    private static final ResourceLocation WIDGET_TEX =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/skill_widget.png");
    private static final ResourceLocation WIDGET_HOVER_TEX =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/skill_widget-hovered.png");
    private static final ResourceLocation WIDGET_DISABLED_TEX =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/skill_widget-disabled.png");
    private static final ResourceLocation WIDGET_PRIMARY_TEX =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/ability-primary.png");
    private static final ResourceLocation WIDGET_PRIMARY_DISABLED_TEX =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/ability-primary-disabled.png");
    private static final ResourceLocation WIDGET_PRIMARY_HOVER_TEX =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/ability-primary-hovered.png");
    private static final ResourceLocation LINK_TEX =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/link.png");
    private static final ResourceLocation LINK_DISABLED_TEX =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/link-disabled.png");

    private static final int HEADER_HEIGHT = 11;
    private static final int CELL_SIZE = 23;
    private static final int GAP = 1;
    private static final int VIEWPORT_W = 131;
    private static final int VIEWPORT_H = 143;
    private static final int LINK_WIDTH = 10;
    private static final int LINK_HEIGHT = 20;
    private static final int VIEWPORT_OFFSET_X = 18;
    private static final int VIEWPORT_OFFSET_Y = -1;
    private static final List<AbilityElement> COLUMN_ORDER = List.of(
            AbilityElement.FIRE,
            AbilityElement.ICE,
            AbilityElement.LIGHTNING,
            AbilityElement.POISON,
            AbilityElement.FORCE,
            AbilityElement.MAGIC,
            AbilityElement.WIND
    );

    private final Minecraft mc = Minecraft.getInstance();
    private final Consumer<AbilityDefinition> onClick;
    private final List<Node> nodes = new ArrayList<>();
    private AbilityId selected;
    private boolean headerVisible = true;
    private boolean showLocked = true;
    private int offsetX = 0;
    private int offsetY = 0;
    private boolean dragging = false;
    private int viewportExtraOffsetX = 0;
    private int viewportExtraWidth = 0;
    private int headerTextOffsetX = 0;

    public AbilityListWidget(int x, int y, int w, int h, Consumer<AbilityDefinition> onClick) {
        super(x, y, w, h, Component.empty());
        this.onClick = onClick;
        reloadAbilities();
    }

    public void reloadAbilities() {
        nodes.clear();
        List<AbilityDefinition> all = AbilityRegistry.all();
        PlayerAbilities abilities = mc.player == null ? null : mc.player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());

        for (int col = 0; col < COLUMN_ORDER.size(); col++) {
            AbilityElement element = COLUMN_ORDER.get(col);
            AbilityDefinition core = all.stream().filter(def -> def.element() == element && def.type() == net.revilodev.codex.abilities.AbilityNodeType.CORE).findFirst().orElse(null);
            if (core == null) continue;
            if (!showLocked && abilities != null && !abilities.unlocked(core.id())) continue;
            nodes.add(new Node(core, col, 0));

            AbilityId cursor = core.id();
            int row = 1;
            while (true) {
                AbilityDefinition next = null;
                for (AbilityDefinition def : all) {
                    if (def.required() == cursor) {
                        next = def;
                        break;
                    }
                }
                if (next == null) break;
                if (showLocked || abilities == null || abilities.unlocked(next.id())) {
                    nodes.add(new Node(next, col, row));
                }
                cursor = next.id();
                row++;
            }
        }

        if (!showLocked && selected != null && nodes.stream().noneMatch(n -> n.def.id() == selected)) {
            selected = null;
            if (onClick != null) onClick.accept(null);
        }
    }

    public void setBounds(int x, int y, int w, int h) {
        setX(x);
        setY(y);
        width = w;
        height = h;
    }

    public void setSelected(AbilityId selected) {
        this.selected = selected;
    }

    public void setHeaderVisible(boolean headerVisible) {
        this.headerVisible = headerVisible;
    }

    public void setShowLocked(boolean showLocked) {
        this.showLocked = showLocked;
        reloadAbilities();
    }

    public void setViewportTweaks(int extraOffsetX, int extraWidth) {
        this.viewportExtraOffsetX = extraOffsetX;
        this.viewportExtraWidth = extraWidth;
    }

    public void setHeaderTextOffsetX(int offset) {
        this.headerTextOffsetX = offset;
    }

    public boolean isOnAbilityNode(double mx, double my) {
        return nodeAt(mx, my) != null;
    }

    public static int gridWidth() {
        return 7 * CELL_SIZE + 6 * GAP;
    }

    public static int gridHeight() {
        int rows = 1;
        for (AbilityElement element : COLUMN_ORDER) {
            int count = 0;
            for (AbilityDefinition def : AbilityRegistry.all()) {
                if (def.element() == element) count++;
            }
            rows = Math.max(rows, count);
        }
        return rows * CELL_SIZE + Math.max(0, rows - 1) * GAP;
    }

    public static int preferredHeight() {
        return HEADER_HEIGHT + gridHeight();
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!visible || mc.player == null) return;
        PlayerAbilities abilities = mc.player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
        if (!showLocked) {
            reloadAbilities();
        }
        if (headerVisible) {
            drawScaledText(gg, "Ability Points: " + abilities.points(), getX() + 1 + headerTextOffsetX, getY() + 4, 0xC78CFF, 0.85F);
        }

        int viewportX = getX() + VIEWPORT_OFFSET_X + viewportExtraOffsetX;
        int viewportY = getY() + HEADER_HEIGHT + VIEWPORT_OFFSET_Y;
        int viewportW = Math.min(width, VIEWPORT_W + viewportExtraWidth);
        int viewportH = Math.min(height - HEADER_HEIGHT, VIEWPORT_H);
        int maxOffsetX = Math.max(0, gridWidth() - viewportW);
        int maxOffsetY = Math.max(0, gridHeight() - viewportH);
        offsetX = Math.max(0, Math.min(offsetX, maxOffsetX));
        offsetY = Math.max(0, Math.min(offsetY, maxOffsetY));

        int top = viewportY;
        RenderSystem.enableBlend();
        gg.enableScissor(viewportX, viewportY, viewportX + viewportW, viewportY + viewportH);
        for (Node node : nodes) {
            if (node.row <= 0 || node.def.required() == null) continue;
            int x = viewportX + node.col * (CELL_SIZE + GAP) - offsetX;
            int y = top + node.row * (CELL_SIZE + GAP) - offsetY;
            ResourceLocation tex = abilities.canUpgrade(node.def.id()) || abilities.unlocked(node.def.id()) ? LINK_TEX : LINK_DISABLED_TEX;
            int linkX = x + (CELL_SIZE - LINK_WIDTH) / 2;
            int linkY = y - ((LINK_HEIGHT - GAP) / 2);
            gg.blit(tex, linkX, linkY, 0, 0, LINK_WIDTH, LINK_HEIGHT, LINK_WIDTH, LINK_HEIGHT);
        }

        for (Node node : nodes) {
            int x = viewportX + node.col * (CELL_SIZE + GAP) - offsetX;
            int y = top + node.row * (CELL_SIZE + GAP) - offsetY;
            AbilityDefinition def = node.def;
            boolean hovered = mouseX >= x && mouseX <= x + CELL_SIZE && mouseY >= y && mouseY <= y + CELL_SIZE;
            int rank = abilities.rank(def.id());
            boolean unlocked = rank > 0;
            boolean maxed = rank >= def.maxRank();
            boolean primary = def.type() == net.revilodev.codex.abilities.AbilityNodeType.CORE;

            ResourceLocation tex;
            if (primary && !unlocked) {
                tex = WIDGET_PRIMARY_DISABLED_TEX;
            } else if (!primary && !abilities.canUpgrade(def.id()) && !unlocked) {
                tex = WIDGET_DISABLED_TEX;
            } else if (primary) {
                tex = (selected == def.id() || hovered) ? WIDGET_PRIMARY_HOVER_TEX : WIDGET_PRIMARY_TEX;
            } else {
                tex = (selected == def.id() || hovered) ? WIDGET_HOVER_TEX : WIDGET_TEX;
            }
            if (maxed && !primary) {
                tex = (selected == def.id() || hovered) ? WIDGET_PRIMARY_HOVER_TEX : WIDGET_PRIMARY_TEX;
            }
            drawScaledTile(gg, tex, x, y, CELL_SIZE, CELL_SIZE);
            gg.blit(def.iconTexture(), x + 3, y + 3, 0, 0, 16, 16, 16, 16);
        }
        gg.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!visible || !active || button != 0 || !isMouseOver(mx, my)) return false;
        Node node = nodeAt(mx, my);
        if (node == null) {
            selected = null;
            if (onClick != null) onClick.accept(null);
            return true;
        }
        selected = node.def.id();
        if (onClick != null) onClick.accept(node.def);
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {}

    private Node nodeAt(double mx, double my) {
        int viewportX = getX() + VIEWPORT_OFFSET_X + viewportExtraOffsetX;
        int top = getY() + HEADER_HEIGHT + VIEWPORT_OFFSET_Y;
        for (Node node : nodes) {
            int x = viewportX + node.col * (CELL_SIZE + GAP) - offsetX;
            int y = top + node.row * (CELL_SIZE + GAP) - offsetY;
            if (mx >= x && mx <= x + CELL_SIZE && my >= y && my <= y + CELL_SIZE) {
                return node;
            }
        }
        return null;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        if (!visible || !active) return false;
        if (!isMouseOver(mouseX, mouseY)) return false;
        offsetY = Math.max(0, offsetY - (int) Math.round(deltaY * 12.0D));
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!visible || !active || button != 0) return false;
        if (!dragging && !isMouseOver(mouseX, mouseY)) return false;
        dragging = true;
        offsetX = Math.max(0, offsetX - (int) Math.round(dragX));
        offsetY = Math.max(0, offsetY - (int) Math.round(dragY));
        return true;
    }

    public void endDrag() {
        dragging = false;
    }

    private void drawScaledTile(GuiGraphics gg, ResourceLocation tex, int x, int y, int w, int h) {
        gg.pose().pushPose();
        gg.pose().translate(x, y, 0.0F);
        gg.pose().scale(w / 26.0F, h / 26.0F, 1.0F);
        gg.blit(tex, 0, 0, 0, 0, 26, 26, 26, 26);
        gg.pose().popPose();
    }

    private void drawScaledText(GuiGraphics gg, String text, int x, int y, int color, float scale) {
        gg.pose().pushPose();
        gg.pose().translate(x, y, 0.0F);
        gg.pose().scale(scale, scale, 1.0F);
        gg.drawString(mc.font, text, 0, 0, color, false);
        gg.pose().popPose();
    }

    private record Node(AbilityDefinition def, int col, int row) {}
}
