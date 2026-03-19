package net.revilodev.codex.client.skills;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.skills.SkillBalance;
import net.revilodev.codex.skills.SkillDefinition;
import net.revilodev.codex.skills.SkillsAttachments;
import net.revilodev.codex.skills.SkillsNetwork;
import net.revilodev.codex.skills.logic.SkillLogic;

import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public final class SkillDetailsPanel extends AbstractWidget {
    private static final float SMALL_TEXT_SCALE = 0.62F;
    private static final float HEADER_TEXT_SCALE = 0.62F;
    private static final int BOTTOM_TEXT_PADDING = 8;
    private static final int CONTENT_TOP = 20;
    private static final int HEADER_ICON_SIZE = 12;

    private static final ResourceLocation TEX_UP =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/skill_upgrade_button.png");
    private static final ResourceLocation TEX_UP_HOVER =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/skill_upgrade_button_hover.png");
    private static final ResourceLocation TEX_UP_DISABLED =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/skill_upgrade_button_disabled.png");

    private static final ResourceLocation TEX_DOWN =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/skill_downgrade.png");
    private static final ResourceLocation TEX_DOWN_HOVER =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/skill_downgrade_hover.png");
    private static final ResourceLocation TEX_DOWN_DISABLED =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/skill_downgrade_disabled.png");

    private final Minecraft mc = Minecraft.getInstance();
    private SkillDefinition skill;
    private final UpgradeButton upgrade;
    private final DowngradeButton downgrade;
    private float scrollY = 0f;
    private int contentHeight = 0;

    public SkillDetailsPanel(int x, int y, int w, int h) {
        super(x, y, w, h, Component.empty());
        this.upgrade = new UpgradeButton(0, 0);
        this.downgrade = new DowngradeButton(0, 0);
        setBounds(x, y, w, h);
    }

    public AbstractButton upgradeButton() { return upgrade; }
    public AbstractButton downgradeButton() { return downgrade; }
    public boolean hasSkill() { return skill != null; }

    public void setBounds(int x, int y, int w, int h) {
        setX(x);
        setY(y);
        width = w;
        height = h;
        int by = y + h - 20;
        int total = upgrade.getWidth() + 2 + downgrade.getWidth();
        int start = x + (w - total) / 2;
        upgrade.setPosition(start, by);
        downgrade.setPosition(start + upgrade.getWidth() + 2, by);
    }

    public void setSkill(SkillDefinition s) {
        skill = s;
        scrollY = 0f;
    }

    public boolean isOnButtons(double mx, double my) {
        return (upgrade.visible && upgrade.isMouseOver(mx, my)) || (downgrade.visible && downgrade.isMouseOver(mx, my));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !active || button != 0) return false;
        if (upgrade.visible && upgrade.active && upgrade.isMouseOver(mouseX, mouseY)) {
            upgrade.onPress();
            return true;
        }
        if (downgrade.visible && downgrade.active && downgrade.isMouseOver(mouseX, mouseY)) {
            downgrade.onPress();
            return true;
        }
        return false;
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!visible || mc.player == null || skill == null) {
            upgrade.visible = false;
            downgrade.visible = false;
            return;
        }
        PlayerSkills ps = mc.player.getData(SkillsAttachments.PLAYER_SKILLS.get());
        int level = ps.level(skill.id());
        boolean unlocked = ps.canUnlock(skill.id());
        boolean canUp = unlocked
                && level < skill.maxLevel()
                && ps.points() > 0;
        boolean canDown = level > 0;

        int x = getX();
        int y = getY();
        int w = width;
        int h = height;

        gg.fill(x, y, x + w, y + h, 0xEE303234);
        gg.hLine(x, x + w, y, 0xAA5A5A5A);
        gg.blit(skill.icon(), x + 4, y + 4, 0, 0, HEADER_ICON_SIZE, HEADER_ICON_SIZE, HEADER_ICON_SIZE, HEADER_ICON_SIZE);
        drawScaledText(gg, skill.title(), x + 18, y + 5, 0xFFFFFF, HEADER_TEXT_SCALE);
        drawScaledText(gg, "level: " + level + "/" + skill.maxLevel(), x + 18, y + 11, 0xD0D0D0, HEADER_TEXT_SCALE);

        int viewportTop = y + CONTENT_TOP;
        int viewportBottom = upgrade.getY() - 10;
        int viewportHeight = Math.max(0, viewportBottom - viewportTop);
        contentHeight = measureContentHeight(skill, unlocked, level);
        int maxScroll = Math.max(0, contentHeight - viewportHeight + BOTTOM_TEXT_PADDING);
        scrollY = Mth.clamp(scrollY, 0f, maxScroll);

        gg.enableScissor(x + 2, viewportTop, x + w - 2, viewportBottom);
        int textY = viewportTop - Mth.floor(scrollY);
        textY = drawSmallWrapped(gg, skill.description(), x + 4, textY, w - 8, 0xE2E2E2) + 4;
        textY = drawSmallWrapped(gg, "effect: " + effectText(skill, level), x + 4, textY, w - 8, 0xA6D9FF) + 4;
        if (!unlocked) {
            drawSmallWrapped(gg, "requires: " + skill.parent().title() + " level 1", x + 4, textY, w - 8, 0xF0AAAA);
        }
        gg.disableScissor();

        upgrade.active = canUp;
        downgrade.active = canDown;
        upgrade.visible = true;
        downgrade.visible = true;
        upgrade.render(gg, mouseX, mouseY, partialTick);
        downgrade.render(gg, mouseX, mouseY, partialTick);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!visible || !active || skill == null) return false;
        int viewportTop = getY() + CONTENT_TOP;
        int viewportBottom = upgrade.getY() - 10;
        if (mouseX < getX() || mouseX > getX() + width) return false;
        if (mouseY < viewportTop || mouseY > viewportBottom) return false;
        int viewportHeight = Math.max(0, viewportBottom - viewportTop);
        int maxScroll = Math.max(0, contentHeight - viewportHeight + BOTTOM_TEXT_PADDING);
        if (maxScroll <= 0) return false;
        scrollY = Mth.clamp(scrollY - (float) (delta * 10), 0f, maxScroll);
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        return mouseScrolled(mouseX, mouseY, deltaY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {}

    private int drawSmallWrapped(GuiGraphics gg, String text, int x, int y, int width, int color) {
        int scaledWidth = Math.max(1, Mth.floor(width / SMALL_TEXT_SCALE));
        int yy = y;
        for (var line : mc.font.split(Component.literal(text), scaledWidth)) {
            gg.pose().pushPose();
            gg.pose().translate(x, yy, 0);
            gg.pose().scale(SMALL_TEXT_SCALE, SMALL_TEXT_SCALE, 1.0F);
            gg.drawString(mc.font, line, 0, 0, color, false);
            gg.pose().popPose();
            yy += Math.max(1, Mth.ceil(mc.font.lineHeight * SMALL_TEXT_SCALE));
        }
        return yy;
    }

    private void drawScaledText(GuiGraphics gg, String text, int x, int y, int color, float scale) {
        gg.pose().pushPose();
        gg.pose().translate(x, y, 0);
        gg.pose().scale(scale, scale, 1.0F);
        gg.drawString(mc.font, text, 0, 0, color, false);
        gg.pose().popPose();
    }

    private int measureContentHeight(SkillDefinition def, boolean unlocked, int level) {
        int scaledWidth = Math.max(1, Mth.floor((width - 8) / SMALL_TEXT_SCALE));
        int lines = mc.font.split(Component.literal(def.description()), scaledWidth).size();
        lines += mc.font.split(Component.literal("effect: " + effectText(def, Math.max(1, level))), scaledWidth).size();
        if (!unlocked) lines += mc.font.split(Component.literal("requires: " + def.parent().title() + " level 1"), scaledWidth).size();
        return lines * Math.max(1, Mth.ceil(mc.font.lineHeight * SMALL_TEXT_SCALE));
    }

    private String effectText(SkillDefinition def, int level) {
        if (level <= 0) {
            return def.description();
        }
        return switch (def.id()) {
            case STRENGTH -> "+" + fmt(SkillBalance.strengthDamage(level)) + " damage";
            case POWER -> "+" + fmt(SkillBalance.powerDamage(level)) + " power";
            case CRIT_POWER -> "+" + fmt(SkillBalance.critPowerDamage(level)) + " crit power";
            case HASTE -> "+" + fmt(SkillBalance.hasteBreakSpeed(level)) + " block break speed";
            case RESISTANCE -> "+" + fmt(SkillBalance.resistance(level) * 100.0D) + "% resistance";
            case FIRE_RESISTANCE -> "+" + fmt(SkillBalance.fireResistance(level) * 100.0D) + "% fire resistance";
            case PROJECTILE_RESISTANCE -> "+" + fmt(SkillBalance.projectileResistance(level) * 100.0D) + "% projectile resistance";
            case KNOCKBACK_RESISTANCE -> "+" + fmt(SkillBalance.knockbackResistance(level) * 100.0D) + "% knockback resistance";
            case AGILITY -> "+" + fmt(SkillBalance.agilitySpeed(level) * 100.0D) + "% speed";
            case LEAPING -> "+" + fmt(SkillBalance.leapingBonus(level) * 100.0D) + "% jump height";
            case VITALITY -> "+" + fmt(SkillBalance.vitalityHearts(level)) + " hearts";
            case REGENERATION -> "+" + fmt(SkillBalance.regenHeartsPerSecond(level) * 100.0D) + "% regen";
            case HEALTH_BOOST -> "+" + fmt(SkillBalance.lifeLeach(level) * 100.0D) + "% life steal";
            case CLEANSE -> "+" + SkillBalance.cleanseImmunities(level) + " negative effect immunity";
            case LUCK -> "+" + fmt(SkillBalance.luck(level)) + " luck";
            case LOOTING -> "+" + fmt(SkillBalance.lootingChance(level) * 100.0D) + "% looting";
            case FORTUNE -> "+" + SkillBalance.fortuneBonus(level) + " fortune";
        };
    }

    private String fmt(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) return Integer.toString((int) Math.rint(v));
        String s = String.format(Locale.ROOT, "%.2f", v);
        while (s.contains(".") && (s.endsWith("0") || s.endsWith("."))) s = s.substring(0, s.length() - 1);
        return s;
    }

    private final class UpgradeButton extends AbstractButton {
        UpgradeButton(int x, int y) { super(x, y, 58, 18, Component.literal("Upgrade")); }

        @Override
        public void onPress() {
            if (!active || skill == null) return;
            PacketDistributor.sendToServer(new SkillsNetwork.SkillActionPayload(skill.id().ordinal(), true));
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            ResourceLocation tex = !active ? TEX_UP_DISABLED : (isMouseOver(mouseX, mouseY) ? TEX_UP_HOVER : TEX_UP);
            gg.blit(tex, getX(), getY(), 0, 0, width, height, width, height);
            float scale = 0.70F;
            int sw = Math.max(1, Mth.ceil(mc.font.width(getMessage()) * scale));
            int sh = Math.max(1, Mth.ceil(mc.font.lineHeight * scale));
            int tx = getX() + (width - sw) / 2;
            int ty = getY() + (height - sh) / 2;
            drawScaledText(gg, getMessage().getString(), tx, ty, active ? 0xFFFFFF : 0x808080, scale);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private final class DowngradeButton extends AbstractButton {
        DowngradeButton(int x, int y) { super(x, y, 58, 18, Component.literal("Downgrade")); }

        @Override
        public void onPress() {
            if (!active || skill == null) return;
            PacketDistributor.sendToServer(new SkillsNetwork.SkillActionPayload(skill.id().ordinal(), false));
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            ResourceLocation tex = !active ? TEX_DOWN_DISABLED : (isMouseOver(mouseX, mouseY) ? TEX_DOWN_HOVER : TEX_DOWN);
            gg.blit(tex, getX(), getY(), 0, 0, width, height, width, height);
            float scale = 0.62F;
            int sw = Math.max(1, Mth.ceil(mc.font.width(getMessage()) * scale));
            int sh = Math.max(1, Mth.ceil(mc.font.lineHeight * scale));
            int tx = getX() + (width - sw) / 2;
            int ty = getY() + (height - sh) / 2;
            drawScaledText(gg, getMessage().getString(), tx, ty, active ? 0xFFFFFF : 0x808080, scale);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }
}
