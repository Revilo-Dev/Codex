package net.revilodev.codex.client.skills;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.skills.SkillBalance;
import net.revilodev.codex.skills.SkillCategory;
import net.revilodev.codex.skills.SkillDefinition;
import net.revilodev.codex.skills.SkillsAttachments;
import net.revilodev.codex.skills.SkillsNetwork;

@OnlyIn(Dist.CLIENT)
public final class SkillDetailsPanel extends AbstractWidget {

    private static final int HEADER_HEIGHT = 28;
    private static final int BOTTOM_PADDING = 28;

    private static final ResourceLocation TEX_BACK =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/skill_back_button.png");
    private static final ResourceLocation TEX_BACK_HOVER =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/skill_back_button_hover.png");

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

    private final BackButton back;
    private final UpgradeButton upgrade;
    private final DowngradeButton downgrade;
    private final Runnable onBack;

    private float scrollY = 0f;
    private int measuredContentHeight = 0;

    public SkillDetailsPanel(int x, int y, int w, int h, Runnable onBack) {
        super(x, y, w, h, Component.empty());
        this.onBack = onBack;

        this.back = new BackButton(getX(), getY(), () -> {
            if (this.onBack != null) this.onBack.run();
        });
        this.back.visible = false;
        this.back.active = false;

        this.upgrade = new UpgradeButton(getX(), getY());
        this.upgrade.visible = false;
        this.upgrade.active = false;

        this.downgrade = new DowngradeButton(getX(), getY());
        this.downgrade.visible = false;
        this.downgrade.active = false;

        setBounds(x, y, w, h);
    }

    public AbstractButton backButton() { return back; }
    public AbstractButton upgradeButton() { return upgrade; }
    public AbstractButton downgradeButton() { return downgrade; }

    public void setBounds(int x, int y, int w, int h) {
        this.setX(x);
        this.setY(y);
        this.width = w;
        this.height = h;

        int cy = y + h - upgrade.getHeight() - 2;
        int cxCenter = x + (w - upgrade.getWidth()) / 2;

        back.setPosition(x + 2, cy);
        upgrade.setPosition(cxCenter, cy);
        downgrade.setPosition(x + w - downgrade.getWidth() - 2, cy);
    }

    public void setSkill(SkillDefinition s) {
        this.skill = s;
        this.scrollY = 0f;
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!this.visible || skill == null || mc.player == null) return;

        int x = this.getX();
        int y = this.getY();
        int w = this.width;

        PlayerSkills ps = mc.player.getData(SkillsAttachments.PLAYER_SKILLS.get());
        SkillCategory cat = skill.category();

        int lvl = ps.level(skill.id());
        int pts = ps.points(cat);

        int contentTop = y + HEADER_HEIGHT;
        int contentBottom = upgrade.getY() - 6;
        int viewportH = Math.max(0, contentBottom - contentTop);

        measuredContentHeight = measureContentHeight(w, lvl);
        int maxScroll = Math.max(0, measuredContentHeight + BOTTOM_PADDING - viewportH);
        scrollY = Mth.clamp(scrollY, 0f, maxScroll);

        Item icon = skill.iconItem().orElse(null);
        if (icon != null) gg.renderItem(new ItemStack(icon), x + 4, y + 4);

        int nameWidth = w - 32;
        gg.drawWordWrap(mc.font, Component.literal(skill.title()), x + 26, y + 6, nameWidth, 0xFFFFFF);

        String sub = "Level " + lvl + " / " + skill.maxLevel();
        gg.drawString(mc.font, sub, x + 26, y + 18, 0xA0A0A0, false);

        gg.enableScissor(x, contentTop, x + w, contentBottom);

        int curY = contentTop + 4 - Mth.floor(scrollY);

        if (skill.description() != null && !skill.description().isBlank()) {
            gg.drawWordWrap(mc.font, Component.literal(skill.description()), x + 4, curY, w - 8, 0xCFCFCF);
            curY += mc.font.wordWrapHeight(skill.description(), w - 8) + 8;
        }

        String eff = effectLine(skill, lvl);
        if (!eff.isBlank()) {
            gg.drawString(mc.font, "Effect:", x + 4, curY, 0x55AAFF, false);
            curY += mc.font.lineHeight + 2;

            gg.drawWordWrap(mc.font, Component.literal(eff), x + 4, curY, w - 8, 0xCFCFCF);
            curY += mc.font.wordWrapHeight(eff, w - 8) + 6;
        }

        gg.disableScissor();

        // Points: centered just above the upgrade button
        int ptsColor = (pts <= 0) ? 0xA0A0A0 : 0x55AAFF;
        String ptsTxt = "Points: " + pts;
        int ptsW = mc.font.width(ptsTxt);
        int ptsX = x + (w - ptsW) / 2;
        int ptsY = upgrade.getY() - mc.font.lineHeight - 2;
        ptsY = Math.max(ptsY, y + HEADER_HEIGHT + 2);
        gg.drawString(mc.font, ptsTxt, ptsX, ptsY, ptsColor, false);

        boolean canUp = pts > 0 && lvl < skill.maxLevel();
        boolean canDown = lvl > 0;

        upgrade.active = canUp;
        downgrade.active = canDown;

        back.visible = true;
        back.active = true;

        upgrade.visible = true;
        downgrade.visible = true;

        back.render(gg, mouseX, mouseY, partialTick);
        upgrade.render(gg, mouseX, mouseY, partialTick);
        downgrade.render(gg, mouseX, mouseY, partialTick);
    }

    private String effectLine(SkillDefinition def, int level) {
        if (def == null) return "";
        if (level <= 0) return "No bonuses active.";
        String id = def.id().name();

        return switch (id) {
            case "SHARPNESS" -> "+" + fmt(level * SkillBalance.SHARPNESS_DAMAGE_PER_LEVEL) + " melee damage";
            case "POWER" -> "+" + fmt(level * SkillBalance.POWER_DAMAGE_PER_LEVEL) + " ranged damage";
            case "CRIT_BONUS" -> "+" + fmt(level * SkillBalance.CRIT_BONUS_PCT_PER_LEVEL) + "% critical damage";
            case "FIRE_RESISTANCE" -> "-" + fmt(level * SkillBalance.FIRE_RES_PCT_PER_LEVEL) + "% fire damage";
            case "BLAST_RESISTANCE" -> "-" + fmt(level * SkillBalance.BLAST_RES_PCT_PER_LEVEL) + "% explosion damage";
            case "PROJECTILE_RESISTANCE" -> "-" + fmt(level * SkillBalance.PROJECTILE_RES_PCT_PER_LEVEL) + "% projectile damage";
            case "KNOCKBACK_RESISTANCE" -> "-" + fmt(level * SkillBalance.KNOCKBACK_RES_PCT_PER_LEVEL) + "% knockback";
            case "HEALTH" -> "+" + fmt(level * SkillBalance.HEALTH_POINTS_PER_LEVEL) + " max health";
            case "REGENERATION" -> "+" + fmt(level * SkillBalance.REGEN_PCT_PER_LEVEL) + "% regen";
            case "SWIFTNESS" -> "+" + fmt(level * SkillBalance.SWIFTNESS_PCT_PER_LEVEL) + "% move speed";
            case "DEFENSE" -> "+" + fmt(level * SkillBalance.DEFENSE_POINTS_PER_LEVEL) + " armor";
            case "SATURATION" -> "+" + fmt(level * SkillBalance.SATURATION_PCT_PER_LEVEL) + "% hunger sustain";
            case "LEAPING" -> "+" + fmt(level * SkillBalance.LEAPING_PCT_PER_LEVEL) + "% jump height";
            case "EFFICIENCY" -> "+" + fmt(level * SkillBalance.EFFICIENCY_PCT_PER_LEVEL) + "% mining speed";
            case "CHOPPING" -> "+" + fmt(level * SkillBalance.CHOPPING_PCT_PER_LEVEL) + "% chopping speed";
            case "FORAGING" -> "+" + fmt(level * SkillBalance.FORAGING_PCT_PER_LEVEL) + "% foraging drops";
            case "FORTUNE" -> "+" + fmt(level * SkillBalance.FORTUNE_PCT_PER_LEVEL) + "% chest loot";
            case "LOOTING" -> "+" + fmt(level * SkillBalance.LOOTING_PCT_PER_LEVEL) + "% mob loot";
            default -> "Bonus (level " + level + ")";
        };
    }

    private String fmt(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) return Integer.toString((int) Math.rint(v));
        String s = String.format(java.util.Locale.ROOT, "%.2f", v);
        while (s.contains(".") && (s.endsWith("0") || s.endsWith("."))) s = s.substring(0, s.length() - 1);
        return s;
    }

    private int measureContentHeight(int panelWidth, int lvl) {
        int w = panelWidth;
        int y = 0;

        if (skill.description() != null && !skill.description().isBlank()) {
            y += mc.font.wordWrapHeight(skill.description(), w - 8) + 8;
        }

        String eff = effectLine(skill, lvl);
        if (!eff.isBlank()) {
            y += mc.font.lineHeight + 2;
            y += mc.font.wordWrapHeight(eff, w - 8) + 6;
        }

        return y;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!this.visible || !this.active) return false;

        int contentTop = this.getY() + HEADER_HEIGHT;
        int contentBottom = upgrade.getY() - 6;

        if (mouseX < this.getX() || mouseX > this.getX() + this.width) return false;
        if (mouseY < contentTop || mouseY > contentBottom) return false;

        int viewportH = Math.max(0, contentBottom - contentTop);
        int maxScroll = Math.max(0, measuredContentHeight + BOTTOM_PADDING - viewportH);
        if (maxScroll <= 0) return false;

        scrollY = Mth.clamp(scrollY - (float) (delta * 12), 0f, maxScroll);
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        return mouseScrolled(mouseX, mouseY, deltaY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.active || button != 0) return false;
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {}

    private final class BackButton extends AbstractButton {
        private final Runnable onPress;

        public BackButton(int x, int y, Runnable onPress) {
            super(x, y, 24, 20, Component.empty());
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isMouseOver(mouseX, mouseY);
            ResourceLocation tex = hovered ? TEX_BACK_HOVER : TEX_BACK;
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private final class UpgradeButton extends AbstractButton {
        public UpgradeButton(int x, int y) {
            super(x, y, 68, 20, Component.literal("Upgrade"));
        }

        @Override
        public void onPress() {
            if (!this.active || skill == null) return;
            PacketDistributor.sendToServer(new SkillsNetwork.SkillActionPayload(skill.id().ordinal(), true));
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.active && this.isMouseOver(mouseX, mouseY);
            ResourceLocation tex = !this.active ? TEX_UP_DISABLED : (hovered ? TEX_UP_HOVER : TEX_UP);
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);

            int textW = mc.font.width(getMessage());
            int textX = getX() + (this.width - textW) / 2 + 2;
            int textY = getY() + (this.height - mc.font.lineHeight) / 2 + 1;
            int color = this.active ? 0xFFFFFF : 0x808080;
            gg.drawString(mc.font, getMessage(), textX, textY, color, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private final class DowngradeButton extends AbstractButton {
        public DowngradeButton(int x, int y) {
            super(x, y, 24, 20, Component.empty());
        }

        @Override
        public void onPress() {
            if (!this.active || skill == null) return;
            PacketDistributor.sendToServer(new SkillsNetwork.SkillActionPayload(skill.id().ordinal(), false));
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isMouseOver(mouseX, mouseY);
            ResourceLocation tex = !this.active ? TEX_DOWN_DISABLED : (hovered ? TEX_DOWN_HOVER : TEX_DOWN);
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }
}
