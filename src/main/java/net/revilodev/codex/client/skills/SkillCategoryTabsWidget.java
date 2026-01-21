package net.revilodev.codex.client.skills;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.revilodev.codex.skills.SkillCategory;

import java.util.function.Consumer;

public final class SkillCategoryTabsWidget extends AbstractWidget {
    private final Consumer<SkillCategory> onSelect;
    private SkillCategory selected = SkillCategory.COMBAT;

    private Component pendingTooltip;
    private int pendingTooltipX;
    private int pendingTooltipY;

    public SkillCategoryTabsWidget(int x, int y, int w, int h, Consumer<SkillCategory> onSelect) {
        super(x, y, w, h, Component.empty());
        this.onSelect = onSelect;
    }

    public void setPosition(int x, int y) {
        setX(x);
        setY(y);
    }

    public void setSelected(SkillCategory c) {
        if (c != null) selected = c;
    }

    public void renderHoverTooltipOnTop(GuiGraphics g) {
        if (pendingTooltip == null) return;
        g.renderTooltip(Minecraft.getInstance().font, pendingTooltip, pendingTooltipX, pendingTooltipY);
        pendingTooltip = null;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        pendingTooltip = null;

        int tabW = Math.max(1, width / 3);
        int x = getX();
        int y = getY();

        SkillCategory[] cats = SkillCategory.values();
        for (int i = 0; i < 3; i++) {
            SkillCategory c = cats[i];
            int tx = x + i * tabW;

            int bg = (c == selected) ? 0xFF2A2A2A : 0xFF1A1A1A;
            g.fill(tx, y, tx + tabW - 1, y + height, bg);

            g.renderItem(new ItemStack(c.icon()), tx + 6, y + 3);

            boolean hover = mouseX >= tx && mouseX < tx + tabW && mouseY >= y && mouseY < y + height;
            if (hover) {
                pendingTooltip = Component.literal(c.title());
                pendingTooltipX = mouseX;
                pendingTooltipY = mouseY;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !active) return false;
        if (button != 0) return false;
        if (!isMouseOver(mouseX, mouseY)) return false;

        int tabW = Math.max(1, width / 3);
        int relX = (int) mouseX - getX();
        int idx = Math.max(0, Math.min(2, relX / tabW));

        SkillCategory c = SkillCategory.values()[idx];
        selected = c;
        if (onSelect != null) onSelect.accept(c);
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {}
}
