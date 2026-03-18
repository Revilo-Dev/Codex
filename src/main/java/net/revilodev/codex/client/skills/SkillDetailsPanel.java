package net.revilodev.codex.client.skills;

import com.revilo.levelup.api.LevelUpApi;
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
    private static final float SMALL_TEXT_SCALE = 0.72F;
    private static final int BOTTOM_TEXT_PADDING = 8;
    private static final int CONTENT_TOP = 24;

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
    private final NoopButton back;
    private float scrollY = 0f;
    private int contentHeight = 0;

    public SkillDetailsPanel(int x, int y, int w, int h, Runnable onBack) {
        super(x, y, w, h, Component.empty());
        this.upgrade = new UpgradeButton(0, 0);
        this.downgrade = new DowngradeButton(0, 0);
        this.back = new NoopButton(0, 0);
        setBounds(x, y, w, h);
    }

    public AbstractButton backButton() { return back; }
    public AbstractButton upgradeButton() { return upgrade; }
    public AbstractButton downgradeButton() { return downgrade; }
    public void setShowBackButton(boolean showBackButton) {}
    public boolean hasSkill() { return skill != null; }

    public void setBounds(int x, int y, int w, int h) {
        setX(x);
        setY(y);
        width = w;
        height = h;
        int by = y + h - 22;
        int total = upgrade.getWidth() + 6 + downgrade.getWidth();
        int start = x + (w - total) / 2;
        upgrade.setPosition(start, by);
        downgrade.setPosition(start + upgrade.getWidth() + 6, by);
        back.setPosition(x, y);
    }

    public void setSkill(SkillDefinition s) {
        skill = s;
        scrollY = 0f;
    }

    public boolean isOnButtons(double mx, double my) {
        return (upgrade.visible && upgrade.isMouseOver(mx, my)) || (downgrade.visible && downgrade.isMouseOver(mx, my));
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
        int reqLevel = SkillLogic.requiredLevelForNextRank(skill.id(), level);
        boolean unlocked = ps.canUnlock(skill.id());
        boolean canUp = unlocked
                && level < skill.maxLevel()
                && ps.points() > 0
                && LevelUpApi.getLevel(mc.player) >= reqLevel
                && LevelUpApi.meetsLevelRequirement(mc.player, reqLevel);
        boolean canDown = level > 0;

        int x = getX();
        int y = getY();
        int w = width;
        int h = height;

        gg.fill(x, y, x + w, y + h, 0xBA303234);
        gg.hLine(x, x + w, y, 0xAA5A5A5A);
        gg.blit(skill.icon(), x + 4, y + 3, 0, 0, 18, 18, 18, 18);
        gg.drawString(mc.font, skill.title(), x + 24, y + 4, 0xFFFFFF, false);
        gg.drawString(mc.font, "level: " + level + "/" + skill.maxLevel(), x + 24, y + 13, 0xD0D0D0, false);

        int viewportTop = y + CONTENT_TOP;
        int viewportBottom = upgrade.getY() - 5;
        int viewportHeight = Math.max(0, viewportBottom - viewportTop);
        contentHeight = measureContentHeight(skill, unlocked, level);
        int maxScroll = Math.max(0, contentHeight - viewportHeight + BOTTOM_TEXT_PADDING);
        scrollY = Mth.clamp(scrollY, 0f, maxScroll);

        gg.enableScissor(x + 2, viewportTop, x + w - 2, viewportBottom);
        int textY = viewportTop - Mth.floor(scrollY);
        textY = drawSmallWrapped(gg, "description: " + skill.description(), x + 4, textY, w - 8, 0xE2E2E2) + 4;
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
        int viewportBottom = upgrade.getY() - 5;
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

    private int measureContentHeight(SkillDefinition def, boolean unlocked, int level) {
        int scaledWidth = Math.max(1, Mth.floor((width - 8) / SMALL_TEXT_SCALE));
        int lines = mc.font.split(Component.literal("description: " + def.description()), scaledWidth).size();
        lines += mc.font.split(Component.literal("effect: " + effectText(def, Math.max(1, level))), scaledWidth).size();
        if (!unlocked) lines += mc.font.split(Component.literal("requires: " + def.parent().title() + " level 1"), scaledWidth).size();
        return lines * Math.max(1, Mth.ceil(mc.font.lineHeight * SMALL_TEXT_SCALE));
    }

    private String effectText(SkillDefinition def, int level) {
        if (level <= 0) return "No bonuses active.";
        return switch (def.id()) {
            case POWER -> "+" + fmt(SkillBalance.powerDamage(level)) + " melee damage";
            case CRIT_POWER -> "+" + fmt(SkillBalance.critPowerMultiplier(level) * 100.0D) + "% crit damage";
            case HASTE -> "+" + fmt(SkillBalance.hasteAttackSpeed(level) * 100.0D) + "% attack speed";
            case FIRE_RESISTANCE -> "-" + fmt(SkillBalance.fireResistance(level) * 100.0D) + "% fire/lava damage";
            case PROJECTILE_RESISTANCE -> "-" + fmt(SkillBalance.projectileResistance(level) * 100.0D) + "% projectile damage";
            case KNOCKBACK_RESISTANCE -> "+" + fmt(SkillBalance.knockbackResistance(level) * 100.0D) + "% knockback resistance";
            case LEAPING -> "+" + fmt(SkillBalance.leapingBonus(level) * 100.0D) + "% jump height";
            case REGENERATION -> "+" + fmt(SkillBalance.regenHeartsPerSecond(level)) + " hearts/sec";
            case HEALTH_BOOST -> "+" + fmt(SkillBalance.healthBoostHearts(level)) + " hearts";
            case CLEANSE -> fmt(SkillBalance.cleanseChance(level) * 100.0D) + "% cleanse chance";
            case LOOTING -> fmt(SkillBalance.lootingChance(level) * 100.0D) + "% extra mob drops";
            case FORTUNE -> fmt(SkillBalance.fortuneChance(level) * 100.0D) + "% extra block drops";
            default -> "Unlocks secondary skills.";
        };
    }

    private String fmt(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) return Integer.toString((int) Math.rint(v));
        String s = String.format(Locale.ROOT, "%.2f", v);
        while (s.contains(".") && (s.endsWith("0") || s.endsWith("."))) s = s.substring(0, s.length() - 1);
        return s;
    }

    private final class UpgradeButton extends AbstractButton {
        UpgradeButton(int x, int y) { super(x, y, 68, 20, Component.literal("Upgrade")); }

        @Override
        public void onPress() {
            if (!active || skill == null) return;
            PacketDistributor.sendToServer(new SkillsNetwork.SkillActionPayload(skill.id().ordinal(), true));
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            ResourceLocation tex = !active ? TEX_UP_DISABLED : (isMouseOver(mouseX, mouseY) ? TEX_UP_HOVER : TEX_UP);
            gg.blit(tex, getX(), getY(), 0, 0, width, height, width, height);
            int tx = getX() + (width - mc.font.width(getMessage())) / 2 + 2;
            int ty = getY() + (height - mc.font.lineHeight) / 2 + 1;
            gg.drawString(mc.font, getMessage(), tx, ty, active ? 0xFFFFFF : 0x808080, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private final class DowngradeButton extends AbstractButton {
        DowngradeButton(int x, int y) { super(x, y, 68, 20, Component.literal("Downgrade")); }

        @Override
        public void onPress() {
            if (!active || skill == null) return;
            PacketDistributor.sendToServer(new SkillsNetwork.SkillActionPayload(skill.id().ordinal(), false));
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            ResourceLocation tex = !active ? TEX_DOWN_DISABLED : (isMouseOver(mouseX, mouseY) ? TEX_DOWN_HOVER : TEX_DOWN);
            gg.blit(tex, getX(), getY(), 0, 0, width, height, width, height);
            int tx = getX() + (width - mc.font.width(getMessage())) / 2 + 2;
            int ty = getY() + (height - mc.font.lineHeight) / 2 + 1;
            gg.drawString(mc.font, getMessage(), tx, ty, active ? 0xFFFFFF : 0x808080, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private static final class NoopButton extends AbstractButton {
        NoopButton(int x, int y) { super(x, y, 1, 1, Component.empty()); visible = false; active = false; }
        @Override public void onPress() {}
        @Override protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {}
        @Override protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }
}
