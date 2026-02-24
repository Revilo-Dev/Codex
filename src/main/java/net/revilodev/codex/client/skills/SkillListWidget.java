package net.revilodev.codex.client.skills;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.skills.SkillDefinition;
import net.revilodev.codex.skills.SkillsAttachments;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class SkillListWidget extends AbstractWidget {

    private static final ResourceLocation ROW_TEX =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/skill_widget.png");
    private static final ResourceLocation ROW_TEX_DISABLED =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/skill_widget_disabled.png");
    private static final int TEX_SIZE = 27;

    public static final int GRID_COLS = 5;
    public static final int GRID_ROWS = 2;
    public static final int CELL_SIZE = 25;
    public static final int GAP = 0;
    public static final int HEADER_HEIGHT = 14;
    private static final int SCROLLBAR_GAP = 2;
    private static final int SCROLLBAR_H = 2;
    private static final float SMALL_TEXT_SCALE = 0.75F;

    private final Minecraft mc = Minecraft.getInstance();
    private final List<SkillDefinition> skills = new ArrayList<>();
    private final Consumer<SkillDefinition> onClick;

    private float scrollX = 0;
    private boolean dragScrollActive = false;
    private double lastDragMouseX = 0.0;

    public SkillListWidget(int x, int y, int w, int h, Consumer<SkillDefinition> onClick) {
        super(x, y, w, h, Component.empty());
        this.onClick = onClick;
    }

    public void setSkills(Iterable<SkillDefinition> defs) {
        skills.clear();
        for (SkillDefinition d : defs) if (d != null) skills.add(d);
        scrollX = 0;
    }

    public void setBounds(int x, int y, int w, int h) {
        this.setX(x);
        this.setY(y);
        this.width = w;
        this.height = h;
    }

    public static int gridWidth() {
        return GRID_COLS * CELL_SIZE + (GRID_COLS - 1) * GAP;
    }

    public static int gridHeight() {
        return GRID_ROWS * CELL_SIZE + (GRID_ROWS - 1) * GAP;
    }

    public static int preferredHeight() {
        return HEADER_HEIGHT + gridHeight() + SCROLLBAR_GAP + SCROLLBAR_H;
    }

    private int totalPoints() {
        if (mc.player == null) return 0;
        PlayerSkills ps = mc.player.getData(SkillsAttachments.PLAYER_SKILLS.get());
        return ps.points();
    }

    private int contentWidth() {
        int cols = (skills.size() + GRID_ROWS - 1) / GRID_ROWS;
        if (cols <= 0) return 0;
        return cols * (CELL_SIZE + GAP) - GAP;
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float pt) {
        if (!visible || mc.player == null) return;

        int x = getX();
        int y = getY();

        int pts = totalPoints();
        int ptsColor = (pts <= 0) ? 0xA0A0A0 : 0x55AAFF;

        String pointsLabel = "Points: " + pts;
        gg.drawString(mc.font, pointsLabel, x + 2, y + 4, ptsColor, false);

        int listTop = y + HEADER_HEIGHT;
        int listH = Math.max(0, height - HEADER_HEIGHT);
        int listW = width;
        int gridH = gridHeight();

        int contentW = contentWidth();
        if (contentW > listW) {
            float max = contentW - listW;
            scrollX = Mth.clamp(scrollX, 0f, max);
        } else {
            scrollX = 0f;
        }

        RenderSystem.enableBlend();
        gg.enableScissor(x, listTop, x + width, listTop + gridH);

        int xOff = x - Mth.floor(scrollX);

        PlayerSkills ps = mc.player.getData(SkillsAttachments.PLAYER_SKILLS.get());

        for (int i = 0; i < skills.size(); i++) {
            SkillDefinition d = skills.get(i);
            int col = i / GRID_ROWS;
            int row = i % GRID_ROWS;

            int cellX = xOff + col * (CELL_SIZE + GAP);
            int top = listTop + row * (CELL_SIZE + GAP);

            if (cellX > x + listW) continue;
            if (cellX + CELL_SIZE < x) continue;
            if (top > listTop + gridH) continue;
            if (top + CELL_SIZE < listTop) continue;

            boolean hover = mouseX >= cellX && mouseX < cellX + CELL_SIZE && mouseY >= top && mouseY < top + CELL_SIZE;

            int lvl = ps.level(d.id());
            boolean canUp = pts > 0 && lvl < d.maxLevel();

            ResourceLocation tex = (!canUp && lvl == 0) ? ROW_TEX_DISABLED : ROW_TEX;
            drawScaledTile(gg, tex, cellX, top);

            if (hover) {
                gg.fill(cellX + 1, top + 1, cellX + CELL_SIZE - 1, top + CELL_SIZE - 1, 0x30FFFFFF);
            }

            Item iconIt = d.iconItem().orElse(null);
            if (iconIt != null) {
                int iconX = cellX + (CELL_SIZE - 16) / 2;
                int iconY = top + (CELL_SIZE - 16) / 2;
                gg.renderItem(new ItemStack(iconIt), iconX, iconY);
            }

            String lvTxt = "Lv" + lvl;
            int lvW = Math.max(1, Mth.ceil(mc.font.width(lvTxt) * SMALL_TEXT_SCALE));
            int badgePad = 1;
            int badgeW = Math.min(CELL_SIZE - 2, lvW + badgePad * 2);
            int badgeX = cellX + CELL_SIZE - badgeW - 1;
            int badgeH = Math.max(6, Mth.ceil(mc.font.lineHeight * SMALL_TEXT_SCALE));
            int badgeY = top + CELL_SIZE - badgeH - 1;
            gg.fill(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, 0xFFC6C6C6);

            gg.pose().pushPose();
            gg.pose().translate(badgeX + badgePad, badgeY + 1, 0.0F);
            gg.pose().scale(SMALL_TEXT_SCALE, SMALL_TEXT_SCALE, 1.0F);
            gg.drawString(mc.font, lvTxt, 0, 0, 0x202020, false);
            gg.pose().popPose();
        }

        gg.disableScissor();

        int barY = listTop + gridH + SCROLLBAR_GAP;
        int trackColor = 0xFF4A4A4A;
        int thumbColor = 0xFFC6C6C6;
        gg.fill(x, barY, x + listW, barY + SCROLLBAR_H, trackColor);
        if (contentW > 0) {
            if (contentW <= listW) {
                gg.fill(x, barY, x + listW, barY + SCROLLBAR_H, thumbColor);
            } else {
                float ratio = (float) listW / (float) contentW;
                int thumbW = Math.max(12, Mth.floor(listW * ratio));
                float maxScroll = contentW - listW;
                float t = maxScroll <= 0 ? 0f : scrollX / maxScroll;
                int thumbX = x + Mth.floor((listW - thumbW) * t);
                gg.fill(thumbX, barY, thumbX + thumbW, barY + SCROLLBAR_H, thumbColor);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mxD, double myD, int button) {
        if (!visible || !active || button != 0) return false;
        if (!isMouseOver(mxD, myD)) return false;
        if (mc.player == null) return false;

        dragScrollActive = false;
        lastDragMouseX = mxD;

        int x = getX();
        int y = getY();
        int listTop = y + HEADER_HEIGHT;
        int listH = Math.max(0, height - HEADER_HEIGHT);
        int gridH = Math.min(listH, gridHeight());
        int content = contentWidth();
        boolean canScroll = content > width;

        int mx = (int) mxD;
        int my = (int) myD;
        if (my >= listTop && my <= listTop + gridH && canScroll) {
            dragScrollActive = true;
        }
        if (my < listTop || my > listTop + gridH) return false;

        int localX = (int) (mx - x + scrollX);
        int localY = (int) (my - listTop);

        int cellSpan = CELL_SIZE + GAP;

        int col = localX / cellSpan;
        int row = localY / cellSpan;

        if (row < 0 || row >= GRID_ROWS) return false;
        if (localX < 0 || localY < 0) return false;

        int inCellX = localX % cellSpan;
        int inCellY = localY % cellSpan;
        if (inCellX >= CELL_SIZE || inCellY >= CELL_SIZE) return false;

        int idx = col * GRID_ROWS + row;
        if (idx < 0 || idx >= skills.size()) return false;

        SkillDefinition clicked = skills.get(idx);
        if (onClick != null) onClick.accept(clicked);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!visible || !active || button != 0) return false;
        if (!dragScrollActive) return false;

        int content = contentWidth();
        if (content <= width) return false;

        float max = content - width;
        scrollX = Mth.clamp(scrollX - (float) dragX, 0f, max);
        lastDragMouseX = mouseX;
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragScrollActive = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!visible || !active) return false;

        int y = getY();
        int listTop = y + HEADER_HEIGHT;
        int listH = Math.max(0, height - HEADER_HEIGHT);
        int gridH = Math.min(listH, gridHeight());

        if (mouseX < getX() || mouseX > getX() + width) return false;
        if (mouseY < listTop || mouseY > listTop + gridH) return false;

        int content = contentWidth();
        if (content <= width) return false;

        float max = content - width;
        scrollX = Mth.clamp(scrollX - (float) (delta * 12), 0f, max);
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        double d = deltaX != 0.0 ? deltaX : deltaY;
        return mouseScrolled(mouseX, mouseY, d);
    }

    private void drawScaledTile(GuiGraphics gg, ResourceLocation tex, int x, int y) {
        float scale = CELL_SIZE / (float) TEX_SIZE;
        gg.pose().pushPose();
        gg.pose().translate(x, y, 0.0F);
        gg.pose().scale(scale, scale, 1.0F);
        gg.blit(tex, 0, 0, 0, 0, TEX_SIZE, TEX_SIZE, TEX_SIZE, TEX_SIZE);
        gg.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput n) {}
}
