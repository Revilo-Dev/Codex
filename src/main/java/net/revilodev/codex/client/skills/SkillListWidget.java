package net.revilodev.codex.client.skills;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.skills.SkillDefinition;
import net.revilodev.codex.skills.SkillsAttachments;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class SkillListWidget extends AbstractWidget {
    private static final int ROW_H = 20;

    private final Consumer<SkillDefinition> onSelect;
    private final List<SkillDefinition> entries = new ArrayList<>();
    private int scroll = 0;

    public SkillListWidget(int x, int y, int w, int h, Consumer<SkillDefinition> onSelect) {
        super(x, y, w, h, Component.empty());
        this.onSelect = onSelect;
    }

    public void setPosition(int x, int y) {
        setX(x);
        setY(y);
    }

    public void setEntries(List<SkillDefinition> defs) {
        entries.clear();
        if (defs != null) entries.addAll(defs);
        scroll = 0;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF121212);

        int maxVisible = Math.max(1, height / ROW_H);
        int start = Math.max(0, Math.min(entries.size(), scroll));
        int end = Math.min(entries.size(), start + maxVisible);

        var font = Minecraft.getInstance().font;
        var player = Minecraft.getInstance().player;

        PlayerSkills skills = null;
        if (player != null) skills = player.getData(SkillsAttachments.PLAYER_SKILLS.get());

        for (int i = start; i < end; i++) {
            int rowY = getY() + (i - start) * ROW_H;
            g.fill(getX() + 2, rowY + 2, getX() + width - 2, rowY + ROW_H - 2, 0xFF1D1D1D);

            SkillDefinition def = entries.get(i);
            def.iconItem().ifPresent(it -> g.renderItem(new ItemStack(it), getX() + 5, rowY + 2));

            int lvl = 0;
            if (skills != null) lvl = skills.level(def.id());

            g.drawString(font, Component.literal(def.title()), getX() + 26, rowY + 3, 0xFFFFFF, false);
            g.drawString(font, Component.literal("Lv " + lvl), getX() + 26, rowY + 12, 0xA0A0A0, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !active) return false;
        if (button != 0) return false;
        if (!isMouseOver(mouseX, mouseY)) return false;

        int idx = (int) ((mouseY - getY()) / ROW_H);
        int i = scroll + idx;
        if (i >= 0 && i < entries.size()) {
            if (onSelect != null) onSelect.accept(entries.get(i));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!visible || !active) return false;
        if (!isMouseOver(mouseX, mouseY)) return false;

        int maxVisible = Math.max(1, height / ROW_H);
        int maxScroll = Math.max(0, entries.size() - maxVisible);

        if (scrollY < 0) scroll = Math.min(maxScroll, scroll + 1);
        if (scrollY > 0) scroll = Math.max(0, scroll - 1);
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {}
}
