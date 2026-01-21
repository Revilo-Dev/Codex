package net.revilodev.codex.client.skills;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.skills.SkillCategory;
import net.revilodev.codex.skills.SkillDefinition;
import net.revilodev.codex.skills.SkillsAttachments;
import net.revilodev.codex.skills.SkillsNetwork;

import java.util.function.Supplier;

public final class SkillDetailsPanel extends AbstractWidget {
    private final Supplier<SkillCategory> category;
    private SkillDefinition selected;

    public SkillDetailsPanel(int x, int y, int w, int h, Supplier<SkillCategory> category, Supplier<SkillDefinition> ignored) {
        super(x, y, w, h, Component.empty());
        this.category = category;
    }

    public void setPosition(int x, int y) {
        setX(x);
        setY(y);
    }

    public void setSelected(SkillDefinition def) {
        this.selected = def;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF121212);

        var mc = Minecraft.getInstance();
        var font = mc.font;

        if (selected == null) {
            g.drawString(font, Component.literal("Select a skill"), getX() + 6, getY() + 6, 0xA0A0A0, false);
            return;
        }

        var player = mc.player;
        PlayerSkills skills = player == null ? null : player.getData(SkillsAttachments.PLAYER_SKILLS.get());

        int lvl = skills == null ? 0 : skills.level(selected.id());
        int pts = skills == null ? 0 : skills.points(category.get());

        g.drawString(font, Component.literal(selected.title()), getX() + 6, getY() + 4, 0xFFFFFF, false);
        g.drawString(font, Component.literal(selected.description()), getX() + 6, getY() + 14, 0xB0B0B0, false);
        g.drawString(font, Component.literal("Lv " + lvl + " / " + selected.maxLevel()), getX() + 6, getY() + 24, 0xA0A0A0, false);
        g.drawString(font, Component.literal(category.get().title() + " Points: " + pts), getX() + 6, getY() + 34, 0xA0A0A0, false);

        int upX = getX() + width - 52;
        int upY = getY() + 6;
        int dnX = upX + 24;

        boolean canUp = pts > 0 && lvl < selected.maxLevel();
        boolean canDn = lvl > 0;

        g.fill(upX, upY, upX + 20, upY + 12, canUp ? 0xFF2A2A2A : 0xFF1A1A1A);
        g.fill(dnX, upY, dnX + 20, upY + 12, canDn ? 0xFF2A2A2A : 0xFF1A1A1A);

        g.drawString(font, Component.literal("+"), upX + 7, upY + 2, canUp ? 0xFFFFFF : 0x707070, false);
        g.drawString(font, Component.literal("-"), dnX + 7, upY + 2, canDn ? 0xFFFFFF : 0x707070, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !active) return false;
        if (button != 0) return false;
        if (selected == null) return false;

        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return false;

        PlayerSkills skills = player.getData(SkillsAttachments.PLAYER_SKILLS.get());
        int lvl = skills.level(selected.id());
        int pts = skills.points(category.get());

        int upX = getX() + width - 52;
        int upY = getY() + 6;
        int dnX = upX + 24;

        if (mouseX >= upX && mouseX < upX + 20 && mouseY >= upY && mouseY < upY + 12) {
            if (pts > 0 && lvl < selected.maxLevel()) {
                PacketDistributor.sendToServer(new SkillsNetwork.SkillActionPayload(selected.id().ordinal(), true));
                return true;
            }
            return false;
        }

        if (mouseX >= dnX && mouseX < dnX + 20 && mouseY >= upY && mouseY < upY + 12) {
            if (lvl > 0) {
                PacketDistributor.sendToServer(new SkillsNetwork.SkillActionPayload(selected.id().ordinal(), false));
                return true;
            }
            return false;
        }

        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {}
}
