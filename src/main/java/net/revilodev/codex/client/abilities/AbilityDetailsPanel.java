package net.revilodev.codex.client.abilities;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.abilities.AbilitiesAttachments;
import net.revilodev.codex.abilities.AbilitiesNetwork;
import net.revilodev.codex.abilities.AbilityDefinition;
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

    private static final int LOADOUT_CELL = 18;
    private static final int LOADOUT_GAP = 4;

    private final Minecraft mc = Minecraft.getInstance();
    private final UpgradeButton upgrade = new UpgradeButton(0, 0);
    private final DowngradeButton downgrade = new DowngradeButton(0, 0);
    private final BindButton bind = new BindButton(0, 0);
    private AbilityDefinition ability;
    private float scrollY;
    private int contentHeight;
    private int loadoutX;
    private int loadoutY;

    public AbilityDetailsPanel(int x, int y, int w, int h) {
        super(x, y, w, h, Component.empty());
        setBounds(x, y, w, h);
    }

    public AbstractButton upgradeButton() { return upgrade; }
    public AbstractButton downgradeButton() { return downgrade; }
    public AbstractButton bindButton() { return bind; }
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
        bind.setPosition(x + w - bind.getWidth() - 4, y + 4);
        int loadoutWidth = 5 * LOADOUT_CELL + 4 * LOADOUT_GAP;
        loadoutX = x + (w - loadoutWidth) / 2;
        loadoutY = bottomY - 26;
    }

    public void setAbility(AbilityDefinition ability) {
        this.ability = ability;
        this.scrollY = 0.0F;
    }

    public boolean isOnButtons(double mx, double my) {
        return (upgrade.visible && upgrade.isMouseOver(mx, my))
                || (downgrade.visible && downgrade.isMouseOver(mx, my))
                || (bind.visible && bind.isMouseOver(mx, my))
                || loadoutSlotAt(mx, my) != 0;
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!visible || mc.player == null || ability == null) {
            upgrade.visible = false;
            downgrade.visible = false;
            bind.visible = false;
            return;
        }

        PlayerAbilities abilities = mc.player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
        PlayerSkills skills = mc.player.getData(SkillsAttachments.PLAYER_SKILLS.get());
        int level = abilities.rank(ability.id());
        boolean canUp = level < ability.maxRank() && abilities.points() > 0;
        boolean canDown = level > 0;
        int assignedSlot = abilities.assignedSlot(ability.id());

        int x = getX();
        int y = getY();
        int w = width;

        gg.fill(x, y, x + w, y + height, 0xEE303234);
        gg.hLine(x, x + w, y, 0xAA5A5A5A);
        gg.renderItem(new ItemStack(ability.iconItem()), x + 4, y + 4);
        drawScaledText(gg, ability.title(), x + 20, y + 5, 0xFFFFFF, 0.62F);
        drawScaledText(gg, "level: " + level + "/" + ability.maxRank(), x + 20, y + 11, 0xD0D0D0, 0.62F);

        int viewportTop = y + 20;
        int viewportBottom = loadoutY - 8;
        int viewportHeight = Math.max(0, viewportBottom - viewportTop);
        contentHeight = 72;
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        scrollY = Mth.clamp(scrollY, 0.0F, maxScroll);

        gg.enableScissor(x + 2, viewportTop, x + w - 2, viewportBottom);
        int textY = viewportTop - Mth.floor(scrollY);
        textY = drawSmallWrapped(gg, ability.description(), x + 4, textY, w - 8, 0xE2E2E2) + 4;
        textY = drawSmallWrapped(gg, "Cooldown: " + formatSeconds(AbilityScaling.cooldownTicks(ability.id(), Math.max(1, level), skills)), x + 4, textY, w - 8, 0xA6D9FF) + 4;
        textY = drawSmallWrapped(gg, "Scaling: " + AbilityScaling.summary(ability.id(), Math.max(1, level), skills), x + 4, textY, w - 8, 0xA6D9FF) + 4;
        drawSmallWrapped(gg, "Keybind: " + bindLabel(assignedSlot), x + 4, textY, w - 8, 0xD0D0D0);
        gg.disableScissor();

        drawLoadout(gg, mouseX, mouseY, abilities);

        upgrade.active = canUp;
        downgrade.active = canDown;
        bind.active = level > 0;
        upgrade.visible = true;
        downgrade.visible = true;
        bind.visible = true;
        upgrade.render(gg, mouseX, mouseY, partialTick);
        downgrade.render(gg, mouseX, mouseY, partialTick);
        bind.render(gg, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !active || button != 0) return false;
        if (upgrade.visible && upgrade.isMouseOver(mouseX, mouseY)) { upgrade.onPress(); return true; }
        if (downgrade.visible && downgrade.isMouseOver(mouseX, mouseY)) { downgrade.onPress(); return true; }
        if (bind.visible && bind.isMouseOver(mouseX, mouseY)) { bind.onPress(); return true; }

        int slot = loadoutSlotAt(mouseX, mouseY);
        if (slot != 0 && ability != null && mc.player != null) {
            PlayerAbilities abilities = mc.player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
            if (abilities.rank(ability.id()) > 0) {
                PacketDistributor.sendToServer(new AbilitiesNetwork.AbilityActionPayload(2, ability.id().ordinal(), slot));
                return true;
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        if (!visible || !active || ability == null) return false;
        int viewportTop = getY() + 20;
        int viewportBottom = loadoutY - 8;
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

    private void drawLoadout(GuiGraphics gg, int mouseX, int mouseY, PlayerAbilities abilities) {
        drawScaledText(gg, "Loadout", loadoutX, loadoutY - 8, 0xD0D0D0, 0.62F);
        for (int slot = 1; slot <= 5; slot++) {
            int x = loadoutX + (slot - 1) * (LOADOUT_CELL + LOADOUT_GAP);
            int y = loadoutY;
            boolean hovered = mouseX >= x && mouseX <= x + LOADOUT_CELL && mouseY >= y && mouseY <= y + LOADOUT_CELL;
            int bg = hovered ? 0xCC5A5E63 : 0xCC3D4043;
            gg.fill(x, y, x + LOADOUT_CELL, y + LOADOUT_CELL, bg);
            gg.fill(x - 1, y - 1, x + LOADOUT_CELL + 1, y, 0x805A5A5A);
            gg.fill(x - 1, y + LOADOUT_CELL, x + LOADOUT_CELL + 1, y + LOADOUT_CELL + 1, 0x805A5A5A);
            gg.fill(x - 1, y, x, y + LOADOUT_CELL, 0x805A5A5A);
            gg.fill(x + LOADOUT_CELL, y, x + LOADOUT_CELL + 1, y + LOADOUT_CELL, 0x805A5A5A);
            var assigned = abilities.slot(slot);
            if (assigned != null) {
                var def = net.revilodev.codex.abilities.AbilityRegistry.def(assigned);
                if (def != null) gg.renderItem(new ItemStack(def.iconItem()), x + 1, y + 1);
            } else {
                gg.drawCenteredString(mc.font, Integer.toString(slot), x + LOADOUT_CELL / 2, y + 5, 0x909090);
            }
        }
    }

    private int loadoutSlotAt(double mx, double my) {
        for (int slot = 1; slot <= 5; slot++) {
            int x = loadoutX + (slot - 1) * (LOADOUT_CELL + LOADOUT_GAP);
            int y = loadoutY;
            if (mx >= x && mx <= x + LOADOUT_CELL && my >= y && my <= y + LOADOUT_CELL) return slot;
        }
        return 0;
    }

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

    private static String formatSeconds(int ticks) {
        return String.format(java.util.Locale.ROOT, "%.1fs", ticks / 20.0D);
    }

    private static String bindLabel(int slot) {
        return slot >= 1 && slot <= 5 ? AbilityKeybinds.slotKeyName(slot) : "Unbound";
    }

    private final class UpgradeButton extends AbstractButton {
        UpgradeButton(int x, int y) { super(x, y, 58, 18, Component.literal("Upgrade")); }

        @Override
        public void onPress() {
            if (!active || ability == null) return;
            PacketDistributor.sendToServer(new AbilitiesNetwork.AbilityActionPayload(0, ability.id().ordinal(), 0));
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
            PacketDistributor.sendToServer(new AbilitiesNetwork.AbilityActionPayload(1, ability.id().ordinal(), 0));
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            ResourceLocation tex = !active ? TEX_DOWN_DISABLED : (isMouseOver(mouseX, mouseY) ? TEX_DOWN_HOVER : TEX_DOWN);
            gg.blit(tex, getX(), getY(), 0, 0, width, height, width, height);
            drawScaledText(gg, getMessage().getString(), getX() + 9, getY() + 6, active ? 0xFFFFFF : 0x808080, 0.62F);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private final class BindButton extends AbstractButton {
        BindButton(int x, int y) {
            super(x, y, 44, 14, Component.literal("Bind"));
        }

        @Override
        public void onPress() {
            if (!active || ability == null || mc.player == null) return;
            PlayerAbilities abilities = mc.player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
            int current = abilities.assignedSlot(ability.id());
            int next = current + 1;
            if (next > 5) next = 0;
            if (next == 0) {
                PacketDistributor.sendToServer(new AbilitiesNetwork.AbilityActionPayload(3, ability.id().ordinal(), 0));
            } else {
                PacketDistributor.sendToServer(new AbilitiesNetwork.AbilityActionPayload(2, ability.id().ordinal(), next));
            }
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            if (ability != null && mc.player != null) {
                int slot = mc.player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get()).assignedSlot(ability.id());
                setMessage(Component.literal(bindLabel(slot)));
            }
            int bg = !active ? 0x66484848 : (isMouseOver(mouseX, mouseY) ? 0xFF7A7A7A : 0xFF616161);
            gg.fill(getX(), getY(), getX() + width, getY() + height, bg);
            gg.hLine(getX(), getX() + width, getY(), 0xFFA8A8A8);
            gg.drawCenteredString(mc.font, getMessage(), getX() + width / 2, getY() + 3, active ? 0xFFFFFF : 0x9A9A9A);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }
}
