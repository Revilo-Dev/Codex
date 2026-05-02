package net.revilodev.codex.client.abilities;

import net.minecraft.client.KeyMapping;
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

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class AbilityDetailsPanel extends AbstractWidget {
    private static final float SMALL_TEXT_SCALE = 0.62F;
    private static final float HEADER_TEXT_SCALE = 0.62F;
    private static final int CONTENT_TOP = 23;
    private static final int CONTENT_BOTTOM_PADDING = 24;
    private static final int HEADER_ICON_SIZE = 12;
    private static final int SMALL_LINE_STEP = 5;
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
    private final SelectButton select = new SelectButton(0, 0);
    private AbilityDefinition ability;
    private float scrollY;
    private int contentHeight;
    private int keybindLeft;
    private int keybindTop;
    private int keybindRight;
    private int keybindBottom;
    private int contentTopOffset = 0;

    public AbilityDetailsPanel(int x, int y, int w, int h) {
        super(x, y, w, h, Component.empty());
        setBounds(x, y, w, h);
    }

    public AbstractButton upgradeButton() { return upgrade; }
    public AbstractButton downgradeButton() { return downgrade; }
    public AbstractButton selectButton() { return select; }
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
        select.setPosition(x + (w - select.getWidth()) / 2, bottomY);
    }

    public void setAbility(AbilityDefinition ability) {
        this.ability = ability;
        this.scrollY = 0.0F;
        if (ability == null) {
            upgrade.visible = false;
            downgrade.visible = false;
            select.visible = false;
            select.active = false;
        }
    }

    public void setContentTopOffset(int contentTopOffset) {
        this.contentTopOffset = contentTopOffset;
    }

    public boolean isOnButtons(double mx, double my) {
        return (upgrade.visible && upgrade.isMouseOver(mx, my))
                || (downgrade.visible && downgrade.isMouseOver(mx, my))
                || (select.visible && select.isMouseOver(mx, my));
    }

    public boolean containsPoint(double mx, double my) {
        return mx >= getX() && mx <= getX() + width && my >= getY() && my <= getY() + height;
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

        gg.fill(x, y, x + w, y + height, 0xFF303234);
        gg.hLine(x, x + w, y, 0xAA5A5A5A);

        PlayerAbilities abilities = mc.player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
        PlayerSkills skills = mc.player.getData(SkillsAttachments.PLAYER_SKILLS.get());
        int level = abilities.rank(ability.id());
        int displayLevel = ability.type() == net.revilodev.codex.abilities.AbilityNodeType.SPECIALIZATION
                ? abilities.rank(ability.id().core())
                : level;
        boolean canUp = abilities.canUpgrade(ability.id());
        boolean canDown = abilities.canDowngrade(ability.id());
        boolean specialization = ability.type() == net.revilodev.codex.abilities.AbilityNodeType.SPECIALIZATION;

        gg.blit(ability.iconTexture(), x + 3, y + 4, 0, 0, HEADER_ICON_SIZE, HEADER_ICON_SIZE, HEADER_ICON_SIZE, HEADER_ICON_SIZE);
        drawScaledText(gg, ability.title(), x + 17, y + 5, 0xFFFFFF, HEADER_TEXT_SCALE);
        drawScaledText(gg, "level: " + displayLevel + "/" + ability.id().core().maxRank(), x + 17, y + 11, 0xD0D0D0, HEADER_TEXT_SCALE);

        List<AbilityId> abilityConflicts = AbilityKeybinds.conflictingAbilities(ability.id());
        List<KeyMapping> keyConflicts = AbilityKeybinds.conflictingNonAbilityMappings(ability.id());
        String keyText = ability.type() == net.revilodev.codex.abilities.AbilityNodeType.CORE ? "" : ("Keybind: " + bindLabel(ability.id()));

        float keyScale = 0.62F;
        int keyWidth = Mth.ceil(mc.font.width(keyText) * keyScale);
        int keyX = x + w - 4 - keyWidth;
        int keyY = y + 5;
        keybindLeft = keyX;
        keybindTop = keyY;
        keybindRight = keyX + keyWidth;
        keybindBottom = keyY + Mth.ceil(mc.font.lineHeight * keyScale);
        boolean keyHovered = !keyText.isEmpty() && isOverKeybind(mouseX, mouseY);

        int keyColor = 0xD0D0D0;
        if (!abilityConflicts.isEmpty()) keyColor = 0xFF6A6A;
        else if (!keyConflicts.isEmpty()) keyColor = 0xF0D15C;
        else if (keyHovered) keyColor = 0xFFFFFF;
        if (!keyText.isEmpty()) {
            drawScaledText(gg, keyText, keyX, keyY, keyColor, keyScale);
        }

        int viewportTop = y + CONTENT_TOP + contentTopOffset;
        int viewportBottom = y + height - CONTENT_BOTTOM_PADDING;
        int viewportHeight = Math.max(0, viewportBottom - viewportTop);
        contentHeight = measureContentHeight(ability, Math.max(1, level), skills);
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        scrollY = Mth.clamp(scrollY, 0.0F, maxScroll);

        gg.enableScissor(x + 2, viewportTop, x + w - 2, viewportBottom);
        int textY = viewportTop - Mth.floor(scrollY);
        textY = drawSmallWrapped(gg, ability.description(), x + 4, textY, w - 8, 0xE2E2E2) + 3;
        textY = drawSmallWrapped(gg, "Cooldown: " + formatSeconds(AbilityScaling.cooldownTicks(ability.id(), Math.max(1, displayLevel), skills)), x + 4, textY, w - 8, 0xF0D15C) + 3;
        drawSmallWrapped(gg, "Scaling: " + AbilityScaling.summary(ability.id(), Math.max(1, displayLevel), skills), x + 4, textY, w - 8, 0xA6D9FF);
        gg.disableScissor();

        if (keyHovered) {
            drawKeybindTooltip(gg, mouseX, mouseY, abilityConflicts, keyConflicts);
        }

        upgrade.active = canUp;
        downgrade.active = canDown;
        upgrade.visible = !specialization;
        downgrade.visible = !specialization;
        select.visible = specialization;
        select.active = specialization && abilities.unlocked(ability.id());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !active || button != 0) return false;
        if (upgrade.visible && upgrade.isMouseOver(mouseX, mouseY)) { upgrade.onPress(); return true; }
        if (downgrade.visible && downgrade.isMouseOver(mouseX, mouseY)) { downgrade.onPress(); return true; }
        if (select.visible && select.isMouseOver(mouseX, mouseY)) { select.onPress(); return true; }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        if (!visible || !active || ability == null) return false;
        int viewportTop = getY() + CONTENT_TOP + contentTopOffset;
        int viewportBottom = getY() + height - CONTENT_BOTTOM_PADDING;
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

    private int measureContentHeight(AbilityDefinition ability, int level, PlayerSkills skills) {
        int scaledWidth = Math.max(1, Mth.floor((width - 8) / SMALL_TEXT_SCALE));
        int lines = mc.font.split(Component.literal(ability.description()), scaledWidth).size();
        lines += mc.font.split(Component.literal("Cooldown: " + formatSeconds(AbilityScaling.cooldownTicks(ability.id(), level, skills))), scaledWidth).size();
        lines += mc.font.split(Component.literal("Scaling: " + AbilityScaling.summary(ability.id(), level, skills)), scaledWidth).size();
        return lines * SMALL_LINE_STEP;
    }

    private int drawSmallWrapped(GuiGraphics gg, String text, int x, int y, int width, int color) {
        int scaledWidth = Math.max(1, Mth.floor(width / SMALL_TEXT_SCALE));
        int yy = y;
        for (var line : mc.font.split(Component.literal(text), scaledWidth)) {
            gg.pose().pushPose();
            gg.pose().translate(x, yy, 0.0F);
            gg.pose().scale(SMALL_TEXT_SCALE, SMALL_TEXT_SCALE, 1.0F);
            gg.drawString(mc.font, line, 0, 0, color, false);
            gg.pose().popPose();
            yy += SMALL_LINE_STEP;
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

    private static String formatSeconds(int ticks) {
        return String.format(java.util.Locale.ROOT, "%.1fs", ticks / 20.0D);
    }

    private static String bindLabel(AbilityId id) {
        return AbilityKeybinds.keyName(id);
    }

    private boolean isOverKeybind(double mouseX, double mouseY) {
        return mouseX >= keybindLeft && mouseX <= keybindRight && mouseY >= keybindTop && mouseY <= keybindBottom;
    }

    private void drawKeybindTooltip(GuiGraphics gg, int mouseX, int mouseY, List<AbilityId> abilityConflicts, List<KeyMapping> keyConflicts) {
        List<Component> lines = new ArrayList<>();
        for (AbilityId id : abilityConflicts) {
            lines.add(Component.literal("Conflicts with ability: " + id.title()));
        }
        for (KeyMapping mapping : keyConflicts) {
            lines.add(Component.literal("Conflicts with: " + Component.translatable(mapping.getName()).getString()));
        }
        if (lines.isEmpty()) return;
        gg.renderTooltip(mc.font, lines, java.util.Optional.empty(), mouseX, mouseY);
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
            drawScaledText(gg, getMessage().getString(), getX() + 13, getY() + 6, active ? 0xFFFFFF : 0x808080, 0.62F);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private final class SelectButton extends AbstractButton {
        SelectButton(int x, int y) { super(x, y, 58, 18, Component.literal("Select")); }

        @Override
        public void onPress() {
            if (!active || ability == null) return;
            PacketDistributor.sendToServer(new AbilitiesNetwork.AbilityActionPayload(2, ability.id().ordinal()));
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            ResourceLocation tex = !active ? TEX_UP_DISABLED : (isMouseOver(mouseX, mouseY) ? TEX_UP_HOVER : TEX_UP);
            gg.blit(tex, getX(), getY(), 0, 0, width, height, width, height);
            drawScaledText(gg, getMessage().getString(), getX() + 17, getY() + 6, active ? 0xFFFFFF : 0x808080, 0.62F);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }
}
