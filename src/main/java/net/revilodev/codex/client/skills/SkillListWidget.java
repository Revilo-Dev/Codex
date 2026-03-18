package net.revilodev.codex.client.skills;

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
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.skills.SkillDefinition;
import net.revilodev.codex.skills.SkillId;
import net.revilodev.codex.skills.SkillRegistry;
import net.revilodev.codex.skills.SkillsAttachments;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class SkillListWidget extends AbstractWidget {
    private static final ResourceLocation WIDGET_TEX =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/skill_widget.png");
    private static final ResourceLocation WIDGET_DISABLED_TEX =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/skill_widget-disabled.png");
    private static final ResourceLocation WIDGET_PRIMARY_TEX =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/skill_widget-primary.png");

    public static final int HEADER_HEIGHT = 12;
    public static final int CELL_SIZE = 18;
    public static final int GAP = 3;
    private static final int ICON_SIZE = 18;

    private final Minecraft mc = Minecraft.getInstance();
    private final Consumer<SkillDefinition> onClick;
    private final List<Node> nodes = new ArrayList<>();
    private SkillId selected;

    public SkillListWidget(int x, int y, int w, int h, Consumer<SkillDefinition> onClick) {
        super(x, y, w, h, Component.empty());
        this.onClick = onClick;
    }

    public void setSkills(Iterable<SkillDefinition> defs) {
        nodes.clear();
        List<SkillDefinition> primaries = SkillRegistry.primarySkills();
        for (int col = 0; col < primaries.size(); col++) {
            SkillDefinition primary = primaries.get(col);
            nodes.add(new Node(primary, col, 0));
            List<SkillDefinition> children = SkillRegistry.secondarySkillsFor(primary.id());
            for (int row = 0; row < children.size(); row++) {
                nodes.add(new Node(children.get(row), col, row + 1));
            }
        }
    }

    public void setBounds(int x, int y, int w, int h) {
        setX(x);
        setY(y);
        width = w;
        height = h;
    }

    public void setSelected(SkillId selected) {
        this.selected = selected;
    }

    public boolean isOnSkillNode(double mx, double my) {
        return nodeAt(mx, my) != null;
    }

    public static int gridWidth() {
        return 5 * CELL_SIZE + 4 * GAP;
    }

    public static int gridHeight() {
        return 4 * CELL_SIZE + 3 * GAP;
    }

    public static int preferredHeight() {
        return HEADER_HEIGHT + gridHeight();
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float pt) {
        if (!visible || mc.player == null) return;
        PlayerSkills ps = mc.player.getData(SkillsAttachments.PLAYER_SKILLS.get());
        gg.drawString(mc.font, "Points: " + ps.points(), getX() + 1, getY() + 2, 0x6AB2FF, false);

        int top = getY() + HEADER_HEIGHT;
        RenderSystem.enableBlend();
        for (Node node : nodes) {
            int x = getX() + node.col * (CELL_SIZE + GAP);
            int y = top + node.row * (CELL_SIZE + GAP);
            SkillDefinition def = node.def;

            boolean unlocked = def.primary() || ps.canUnlock(def.id());
            ResourceLocation tex = def.primary() ? WIDGET_PRIMARY_TEX : (unlocked ? WIDGET_TEX : WIDGET_DISABLED_TEX);
            drawScaledTile(gg, tex, x, y, CELL_SIZE, CELL_SIZE);
            gg.blit(def.icon(), x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

            if (selected == def.id()) {
                gg.fill(x + 1, y + 1, x + CELL_SIZE - 1, y + CELL_SIZE - 1, 0x40FFFFFF);
            } else if (mouseX >= x && mouseX <= x + CELL_SIZE && mouseY >= y && mouseY <= y + CELL_SIZE) {
                gg.fill(x + 1, y + 1, x + CELL_SIZE - 1, y + CELL_SIZE - 1, 0x25FFFFFF);
            }
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        return false;
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

    private record Node(SkillDefinition def, int col, int row) {}
}
