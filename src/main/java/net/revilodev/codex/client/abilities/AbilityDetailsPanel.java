package net.revilodev.codex.client.abilities;

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
import net.revilodev.codex.abilities.AbilitiesAttachments;
import net.revilodev.codex.abilities.AbilitiesNetwork;
import net.revilodev.codex.abilities.AbilityDefinition;
import net.revilodev.codex.abilities.AbilityId;
import net.revilodev.codex.abilities.PlayerAbilities;
import net.revilodev.codex.abilities.logic.AbilityScaling;
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.skills.SkillsAttachments;

@OnlyIn(Dist.CLIENT)
public final class AbilityDetailsPanel extends AbstractWidget {
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
    private final UpgradeButton upgrade = new UpgradeButton(0, 0);
    private final DowngradeButton downgrade = new DowngradeButton(0, 0);
    private AbilityDefinition ability;
    private float scrollY;
    private int contentHeight;

    public AbilityDetailsPanel(int x, int y, int w, int h) {
        super(x, y, w, h, Component.empty());
        setBounds(x, y, w, h);
    }

    public AbstractButton upgradeButton() { return upgrade; }
    public AbstractButton downgradeButton() { return downgrade; }
    public boolean hasAbility() { return ability != null; }

    public void setBounds(int x, int y, int w, int h) {
        setX(x);
        setY(y);
        width = w;
        height = h;
        int bottomY = y + h - 20;
        int total = upgrade.getWidth() + 2 + downgrade.getWidth();
        int start = x + (w - total) / 2;
        upgrade.setPosition(start, bottomY);
        downgrade.setPosition(start + upgrade.getWidth() + 2, bottomY);
    }

    public void setAbility(AbilityDefinition ability) {
        this.ability = ability;
        this.scrollY = 0.0F;
    }

    public boolean isOnButtons(double mx, double my) {
        return (upgrade.visible && upgrade.isMouseOver(mx, my))
                || (downgrade.visible && downgrade.isMouseOver(mx, my));
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!visible || mc.player == null || ability == null) {
            upgrade.visible = false;
            downgrade.visible = false;
            return;
        }

        int x = getX();
        int y = getY();
        int w = width;

        gg.fill(x, y, x + w, y + height, 0xEE303234);
        gg.hLine(x, x + w, y, 0xAA5A5A5A);

        PlayerAbilities abilities = mc.player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
        PlayerSkills skills = mc.player.getData(SkillsAttachments.PLAYER_SKILLS.get());
        int level = abilities.rank(ability.id());
        boolean canUp = level < ability.maxRank() && abilities.points() > 0;
        boolean canDown = level > 0;

        gg.blit(ability.iconTexture(), x + 4, y + 4, 0, 0, 16, 16, 16, 16);
        drawScaledText(gg, ability.title(), x + 20, y + 5, 0xFFFFFF, 0.62F);
        drawScaledText(gg, "level: " + level + "/" + ability.maxRank(), x + 20, y + 11, 0xD0D0D0, 0.62F);
        drawRightScaledText(gg, "Keybind: " + bindLabel(ability.id()), x + w - 4, y + 5, 0xD0D0D0, 0.62F);

        int viewportTop = y + 20;
        int viewportBottom = y + height - 24;
        int viewportHeight = Math.max(0, viewportBottom - viewportTop);
        contentHeight = 86;
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        scrollY = Mth.clamp(scrollY, 0.0F, maxScroll);

        gg.enableScissor(x + 2, viewportTop, x + w - 2, viewportBottom);
        int textY = viewportTop - Mth.floor(scrollY);
        textY = drawSmallWrapped(gg, ability.description(), x + 4, textY, w - 8, 0xE2E2E2) + 4;
        textY = drawSmallWrapped(gg, "Cooldown: " + formatSeconds(AbilityScaling.cooldownTicks(ability.id(), Math.max(1, level), skills)), x + 4, textY, w - 8, 0xA6D9FF) + 4;
        drawSmallWrapped(gg, "Scaling: " + AbilityScaling.summary(ability.id(), Math.max(1, level), skills), x + 4, textY, w - 8, 0xA6D9FF);
        gg.disableScissor();

        upgrade.active = canUp;
        downgrade.active = canDown;
        upgrade.visible = true;
        downgrade.visible = true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !active || button != 0) return false;
        if (upgrade.visible && upgrade.isMouseOver(mouseX, mouseY)) { upgrade.onPress(); return true; }
        if (downgrade.visible && downgrade.isMouseOver(mouseX, mouseY)) { downgrade.onPress(); return true; }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        if (!visible || !active || ability == null) return false;
        int viewportTop = getY() + 20;
        int viewportBottom = getY() + height - 24;
        if (mouseX < getX() || mouseX > getX() + width || mouseY < viewportTop || mouseY > viewportBottom) return false;
        int viewportHeight = Math.max(0, viewportBottom - viewportTop);
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        if (maxScroll <= 0) return false;
        scrollY = Mth.clamp(scrollY - (float) (deltaY * 10.0D), 0.0F, maxScroll);
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        return mouseScrolled(mouseX, mouseY, deltaY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {}

    private int drawSmallWrapped(GuiGraphics gg, String text, int x, int y, int width, int color) {
        int scaledWidth = Math.max(1, Mth.floor(width / 0.62F));
        int yy = y;
        for (var line : mc.font.split(Component.literal(text), scaledWidth)) {
            gg.pose().pushPose();
            gg.pose().translate(x, yy, 0.0F);
            gg.pose().scale(0.62F, 0.62F, 1.0F);
            gg.drawString(mc.font, line, 0, 0, color, false);
            gg.pose().popPose();
            yy += Math.max(1, Mth.ceil(mc.font.lineHeight * 0.62F));
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

    private void drawRightScaledText(GuiGraphics gg, String text, int rightX, int y, int color, float scale) {
        int scaledWidth = Mth.ceil(mc.font.width(text) * scale);
        drawScaledText(gg, text, rightX - scaledWidth, y, color, scale);
    }

    private static String formatSeconds(int ticks) {
        return String.format(java.util.Locale.ROOT, "%.1fs", ticks / 20.0D);
    }

    private static String bindLabel(AbilityId id) {
        return AbilityKeybinds.keyName(id);
    }

    private final class UpgradeButton extends AbstractButton {
        UpgradeButton(int x, int y) { super(x, y, 58, 18, Component.literal("Upgrade")); }

        @Override
        public void onPress() {
            if (!active || ability == null) return;
            PacketDistributor.sendToServer(new AbilitiesNetwork.AbilityActionPayload(0, ability.id().ordinal()));
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            ResourceLocation tex = !active ? TEX_UP_DISABLED : (isMouseOver(mouseX, mouseY) ? TEX_UP_HOVER : TEX_UP);
            gg.blit(tex, getX(), getY(), 0, 0, width, height, width, height);
            drawScaledText(gg, getMessage().getString(), getX() + 14, getY() + 6, active ? 0xFFFFFF : 0x808080, 0.62F);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private final class DowngradeButton extends AbstractButton {
        DowngradeButton(int x, int y) { super(x, y, 58, 18, Component.literal("Downgrade")); }

        @Override
        public void onPress() {
            if (!active || ability == null) return;
            PacketDistributor.sendToServer(new AbilitiesNetwork.AbilityActionPayload(1, ability.id().ordinal()));
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            ResourceLocation tex = !active ? TEX_DOWN_DISABLED : (isMouseOver(mouseX, mouseY) ? TEX_DOWN_HOVER : TEX_DOWN);
            gg.blit(tex, getX(), getY(), 0, 0, width, height, width, height);
            drawScaledText(gg, getMessage().getString(), getX() + 10, getY() + 6, active ? 0xFFFFFF : 0x808080, 0.62F);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }
}
