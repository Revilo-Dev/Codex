package net.revilodev.codex.client.abilities;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
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

    private static final int HEADER_HEIGHT = 11;
    private static final int CELL_SIZE = 23;
    private static final int GAP = 3;

    private final Minecraft mc = Minecraft.getInstance();
    private final Consumer<AbilityDefinition> onClick;
    private final List<Node> nodes = new ArrayList<>();
    private AbilityId selected;

    public AbilityListWidget(int x, int y, int w, int h, Consumer<AbilityDefinition> onClick) {
        super(x, y, w, h, Component.empty());
        this.onClick = onClick;
        reloadAbilities();
    }

    public void reloadAbilities() {
        nodes.clear();
        List<AbilityDefinition> all = AbilityRegistry.all();
        for (int i = 0; i < all.size(); i++) {
            nodes.add(new Node(all.get(i), i % 5, i / 5));
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

    public boolean isOnAbilityNode(double mx, double my) {
        return nodeAt(mx, my) != null;
    }

    public static int gridWidth() {
        return 5 * CELL_SIZE + 4 * GAP;
    }

    public static int gridHeight() {
        return 2 * CELL_SIZE + GAP;
    }

    public static int preferredHeight() {
        return HEADER_HEIGHT + gridHeight();
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!visible || mc.player == null) return;
        PlayerAbilities abilities = mc.player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
        drawScaledText(gg, "Ability Points: " + abilities.points(), getX() + 1, getY() + 4, 0xC78CFF, 0.85F);

        int top = getY() + HEADER_HEIGHT;
        for (Node node : nodes) {
            int x = getX() + node.col * (CELL_SIZE + GAP);
            int y = top + node.row * (CELL_SIZE + GAP);
            AbilityDefinition def = node.def;
            boolean hovered = mouseX >= x && mouseX <= x + CELL_SIZE && mouseY >= y && mouseY <= y + CELL_SIZE;
            boolean unlocked = abilities.rank(def.id()) > 0;

            ResourceLocation tex = unlocked || selected == def.id() || hovered ? WIDGET_HOVER_TEX : WIDGET_DISABLED_TEX;
            if (unlocked && !hovered && selected != def.id()) tex = WIDGET_TEX;
            drawScaledTile(gg, tex, x, y, CELL_SIZE, CELL_SIZE);
            gg.renderItem(new ItemStack(def.iconItem()), x + 3, y + 3);
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
