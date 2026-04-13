package net.revilodev.codex.client.abilities;

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
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/skill_widget-primary.png");
    private static final ResourceLocation WIDGET_PRIMARY_HOVER_TEX =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/skill_widget_primary-hovered.png");

    private static final int HEADER_HEIGHT = 11;
    private static final int CELL_SIZE = 23;
    private static final int GAP = 3;

    private final Minecraft mc = Minecraft.getInstance();
    private final Consumer<AbilityDefinition> onClick;
    private final List<Node> nodes = new ArrayList<>();
    private AbilityId selected;
    private boolean headerVisible = true;
    private boolean showLocked = true;
    private LineArea hoveredLine;

    public AbilityListWidget(int x, int y, int w, int h, Consumer<AbilityDefinition> onClick) {
        super(x, y, w, h, Component.empty());
        this.onClick = onClick;
        reloadAbilities();
    }

    public void reloadAbilities() {
        nodes.clear();
        List<AbilityDefinition> visibleAbilities = AbilityRegistry.all();
        if (!showLocked && mc.player != null) {
            PlayerAbilities abilities = mc.player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
            visibleAbilities = visibleAbilities.stream()
                    .filter(def -> abilities.unlocked(def.id()))
                    .toList();
            if (selected != null && visibleAbilities.stream().noneMatch(def -> def.id() == selected)) {
                selected = null;
                if (onClick != null) onClick.accept(null);
            }
        }
        for (int i = 0; i < visibleAbilities.size(); i++) {
            nodes.add(new Node(visibleAbilities.get(i), i % 5, i / 5));
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

    public boolean isOnAbilityNode(double mx, double my) {
        return nodeAt(mx, my) != null;
    }

    public static int gridWidth() {
        return 5 * CELL_SIZE + 4 * GAP;
    }

    public static int gridHeight() {
        int rows = Math.max(1, (int) Math.ceil(AbilityRegistry.all().size() / 5.0D));
        return rows * CELL_SIZE + Math.max(0, rows - 1) * GAP;
    }

    public static int preferredHeight() {
        return HEADER_HEIGHT + gridHeight();
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!visible || mc.player == null) return;
        PlayerAbilities abilities = mc.player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
        hoveredLine = null;
        if (!showLocked) {
            reloadAbilities();
        }
        if (headerVisible) {
            drawScaledText(gg, "Ability Points: " + abilities.points(), getX() + 1, getY() + 4, 0xC78CFF, 0.85F);
        }

        if (selected == AbilityId.BLAZE || selected == AbilityId.SCAVENGER) {
            drawIncompatibilityLine(gg, mouseX, mouseY, AbilityId.SCAVENGER, AbilityId.BLAZE);
        }
        if (selected == AbilityId.EXECUTION || selected == AbilityId.OVERPOWER) {
            drawIncompatibilityLine(gg, mouseX, mouseY, AbilityId.EXECUTION, AbilityId.OVERPOWER);
        }
        if (selected == AbilityId.LUNGE) {
            drawRequiredLine(gg, mouseX, mouseY);
        }

        int top = getY() + HEADER_HEIGHT;
        for (Node node : nodes) {
            int x = getX() + node.col * (CELL_SIZE + GAP);
            int y = top + node.row * (CELL_SIZE + GAP);
            AbilityDefinition def = node.def;
            boolean hovered = mouseX >= x && mouseX <= x + CELL_SIZE && mouseY >= y && mouseY <= y + CELL_SIZE;
            int rank = abilities.rank(def.id());
            boolean unlocked = rank > 0;
            boolean maxed = rank >= def.maxRank();

            ResourceLocation tex = unlocked || selected == def.id() || hovered ? WIDGET_HOVER_TEX : WIDGET_DISABLED_TEX;
            if (maxed) {
                tex = (selected == def.id() || hovered) ? WIDGET_PRIMARY_HOVER_TEX : WIDGET_PRIMARY_TEX;
            } else if (unlocked && !hovered && selected != def.id()) {
                tex = WIDGET_TEX;
            }
            drawScaledTile(gg, tex, x, y, CELL_SIZE, CELL_SIZE);
            gg.blit(def.iconTexture(), x + 3, y + 3, 0, 0, 16, 16, 16, 16);
        }

        if (hoveredLine != null) {
            gg.renderTooltip(mc.font, Component.literal(hoveredLine.tooltip()), mouseX, mouseY);
        }
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
        int top = getY() + HEADER_HEIGHT;
        for (Node node : nodes) {
            int x = getX() + node.col * (CELL_SIZE + GAP);
            int y = top + node.row * (CELL_SIZE + GAP);
            if (mx >= x && mx <= x + CELL_SIZE && my >= y && my <= y + CELL_SIZE) {
                return node;
            }
        }
        return null;
    }

    private Node nodeFor(AbilityId id) {
        if (id == null) return null;
        for (Node node : nodes) {
            if (node.def.id() == id) return node;
        }
        return null;
    }

    private void drawIncompatibilityLine(GuiGraphics gg, int mouseX, int mouseY, AbilityId leftId, AbilityId rightId) {
        Node toxic = nodeFor(leftId);
        Node blaze = nodeFor(rightId);
        if (toxic == null || blaze == null) return;

        int top = getY() + HEADER_HEIGHT;
        int toxicX = getX() + toxic.col * (CELL_SIZE + GAP);
        int toxicY = top + toxic.row * (CELL_SIZE + GAP);
        int blazeX = getX() + blaze.col * (CELL_SIZE + GAP);
        int blazeY = top + blaze.row * (CELL_SIZE + GAP);
        int y = Math.max(toxicY, blazeY) + CELL_SIZE + 1 - 13;
        drawBetweenNodesMarker(gg, toxicX, blazeX, y, 0xFFE05353, mouseX, mouseY, "incompatible");
    }

    private void drawRequiredLine(GuiGraphics gg, int mouseX, int mouseY) {
        Node leap = nodeFor(AbilityId.LEAP);
        Node lunge = nodeFor(AbilityId.LUNGE);
        if (leap == null || lunge == null) return;

        int top = getY() + HEADER_HEIGHT;
        int leapX = getX() + leap.col * (CELL_SIZE + GAP);
        int leapY = top + leap.row * (CELL_SIZE + GAP);
        int lungeX = getX() + lunge.col * (CELL_SIZE + GAP);
        int lungeY = top + lunge.row * (CELL_SIZE + GAP);
        int y = Math.max(leapY, lungeY) + CELL_SIZE + 1 - 14;
        drawBetweenNodesMarker(gg, leapX + 15, lungeX + 15, y, 0xFF4BE36A, mouseX, mouseY, "Required dependency");
    }

    private void drawBetweenNodesMarker(GuiGraphics gg, int leftNodeX, int rightNodeX, int y, int color, int mouseX, int mouseY, String tooltip) {
        int c1 = leftNodeX + (CELL_SIZE / 2);
        int c2 = rightNodeX + (CELL_SIZE / 2);
        int midpoint = (c1 + c2) / 2;
        int x1 = midpoint - 7;
        int x2 = x1 + 14;
        drawOutlinedBar(gg, x1, x2, y, color);
        updateHoveredLine(mouseX, mouseY, x1, x2, y, tooltip);
    }

    private void drawOutlinedBar(GuiGraphics gg, int x1, int x2, int y, int color) {
        gg.hLine(x1 - 1, x2 + 1, y - 1, 0xFF000000);
        gg.hLine(x1 - 1, x2 + 1, y + 2, 0xFF000000);
        gg.vLine(x1 - 1, y - 1, y + 2, 0xFF000000);
        gg.vLine(x2 + 1, y - 1, y + 2, 0xFF000000);
        gg.hLine(x1, x2, y, color);
        gg.hLine(x1, x2, y + 1, color);
    }

    private void updateHoveredLine(int mouseX, int mouseY, int x1, int x2, int y, String tooltip) {
        if (mouseX >= x1 - 1 && mouseX <= x2 + 1 && mouseY >= y - 1 && mouseY <= y + 2) {
            hoveredLine = new LineArea(tooltip);
        }
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
    private record LineArea(String tooltip) {}
}
