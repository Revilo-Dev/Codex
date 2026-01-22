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
import net.revilodev.codex.skills.SkillCategory;
import net.revilodev.codex.skills.SkillDefinition;
import net.revilodev.codex.skills.SkillsAttachments;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class SkillListWidget extends AbstractWidget {

    private static final ResourceLocation ROW_TEX =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/skill_widget.png");
    private static final ResourceLocation ROW_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/skill_widget_hover.png");
    private static final ResourceLocation ROW_TEX_DISABLED =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/skill_widget_disabled.png");

    private final Minecraft mc = Minecraft.getInstance();
    private final List<SkillDefinition> skills = new ArrayList<>();
    private final Consumer<SkillDefinition> onClick;

    private float scrollY = 0;
    private final int rowH = 27;
    private final int pad = 2;

    private final int headerH = 14;

    private String categoryId = "combat";

    public SkillListWidget(int x, int y, int w, int h, Consumer<SkillDefinition> onClick) {
        super(x, y, w, h, Component.empty());
        this.onClick = onClick;
    }

    public void setSkills(Iterable<SkillDefinition> defs) {
        skills.clear();
        for (SkillDefinition d : defs) if (d != null) skills.add(d);
        scrollY = 0;
    }

    public void setCategory(String catId) {
        this.categoryId = catId == null ? "combat" : catId;
        scrollY = 0;
    }

    public void setBounds(int x, int y, int w, int h) {
        this.setX(x);
        this.setY(y);
        this.width = w;
        this.height = h;
    }

    private SkillCategory category() {
        return SkillCategory.byId(categoryId);
    }

    private int points() {
        if (mc.player == null) return 0;
        PlayerSkills ps = mc.player.getData(SkillsAttachments.PLAYER_SKILLS.get());
        return ps.points(category());
    }

    private boolean canUpgradeAny() {
        if (mc.player == null) return false;
        PlayerSkills ps = mc.player.getData(SkillsAttachments.PLAYER_SKILLS.get());
        if (ps.points(category()) <= 0) return false;

        for (SkillDefinition d : skills) {
            if (d.category() != category()) continue;
            int lvl = ps.level(d.id());
            if (lvl < d.maxLevel()) return true;
        }
        return false;
    }

    private List<SkillDefinition> visibleList() {
        SkillCategory c = category();
        List<SkillDefinition> out = new ArrayList<>();
        for (SkillDefinition d : skills) {
            if (d.category() == c) out.add(d);
        }
        return out;
    }

    private int contentHeight() {
        int rows = visibleList().size();
        return rows * (rowH + pad);
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float pt) {
        if (!visible || mc.player == null) return;

        int x = getX();
        int y = getY();

        int pts = points();
        int ptsColor = (pts <= 0) ? 0xA0A0A0 : 0x55AAFF;

        String left = category().title();
        gg.drawString(mc.font, left, x + 2, y + 2, 0xFFFFFF, false);

        String right = "Points: " + pts;
        int rw = mc.font.width(right);
        gg.drawString(mc.font, right, x + width - rw - 2, y + 2, ptsColor, false);

        int listTop = y + headerH;
        int listH = Math.max(0, height - headerH);

        int content = contentHeight();
        if (content > listH) {
            float max = content - listH;
            scrollY = Mth.clamp(scrollY, 0f, max);
        } else {
            scrollY = 0f;
        }

        RenderSystem.enableBlend();
        gg.enableScissor(x, listTop, x + width, y + height);

        int yOff = listTop - Mth.floor(scrollY);
        int drawn = 0;

        List<SkillDefinition> list = visibleList();
        PlayerSkills ps = mc.player.getData(SkillsAttachments.PLAYER_SKILLS.get());

        boolean upgradePossible = pts > 0;

        for (int i = 0; i < list.size(); i++) {
            SkillDefinition d = list.get(i);
            int top = yOff + drawn * (rowH + pad);
            drawn++;

            if (top > listTop + listH) break;
            if (top + rowH < listTop) continue;

            boolean hover = mouseX >= x && mouseX < x + width && mouseY >= top && mouseY < top + rowH;

            int lvl = ps.level(d.id());
            boolean canUp = upgradePossible && lvl < d.maxLevel();

            ResourceLocation tex = !canUp && pts <= 0 ? ROW_TEX_DISABLED : (hover ? ROW_TEX_HOVER : ROW_TEX);
            gg.blit(tex, x, top, 0, 0, 127, 27, 127, 27);

            Item iconIt = d.iconItem().orElse(null);
            if (iconIt != null) gg.renderItem(new ItemStack(iconIt), x + 6, top + 5);

            String name = d.title();
            int maxW = width - 58;
            if (mc.font.width(name) > maxW) {
                name = mc.font.plainSubstrByWidth(name, maxW - mc.font.width("...")) + "...";
            }

            int nameColor = (canUp || lvl > 0) ? 0xFFFFFF : 0xA0A0A0;
            gg.drawString(mc.font, name, x + 30, top + 9, nameColor, false);

            String lvTxt = "Lv " + lvl;
            int lvW = mc.font.width(lvTxt);
            int lvColor = (lvl > 0) ? 0x55AAFF : 0xA0A0A0;
            gg.drawString(mc.font, lvTxt, x + width - lvW - 6, top + 9, lvColor, false);
        }

        gg.disableScissor();

        if (content > listH) {
            float maxScroll = content - listH;
            float ratio = (float) listH / (float) content;
            int barH = Math.max(12, (int) (listH * ratio));
            float scrollRatio = maxScroll <= 0 ? 0f : scrollY / maxScroll;
            int barY = listTop + (int) ((listH - barH) * scrollRatio);
            gg.fill(x + width + 4, barY, x + width + 6, barY + barH, 0xFF808080);
        }
    }

    @Override
    public boolean mouseClicked(double mxD, double myD, int button) {
        if (!visible || !active || button != 0) return false;
        if (!isMouseOver(mxD, myD)) return false;
        if (mc.player == null) return false;

        int x = getX();
        int y = getY();
        int listTop = y + headerH;
        int listH = Math.max(0, height - headerH);

        int mx = (int) mxD;
        int my = (int) myD;
        if (my < listTop || my > listTop + listH) return false;

        int localY = (int) (my - listTop + scrollY);
        int idx = localY / (rowH + pad);

        List<SkillDefinition> list = visibleList();
        if (idx < 0 || idx >= list.size()) return false;

        SkillDefinition clicked = list.get(idx);
        if (onClick != null) onClick.accept(clicked);
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!visible || !active) return false;

        int y = getY();
        int listTop = y + headerH;
        int listH = Math.max(0, height - headerH);

        if (mouseX < getX() || mouseX > getX() + width) return false;
        if (mouseY < listTop || mouseY > listTop + listH) return false;

        int content = contentHeight();
        if (content <= listH) return false;

        float max = content - listH;
        scrollY = Mth.clamp(scrollY - (float) (delta * 12), 0f, max);
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        return mouseScrolled(mouseX, mouseY, deltaY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput n) {}
}
