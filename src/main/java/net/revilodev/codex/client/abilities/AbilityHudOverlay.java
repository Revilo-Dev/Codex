package net.revilodev.codex.client.abilities;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.revilodev.codex.abilities.AbilitiesAttachments;
import net.revilodev.codex.abilities.AbilityConfig;
import net.revilodev.codex.abilities.AbilityDefinition;
import net.revilodev.codex.abilities.AbilityId;
import net.revilodev.codex.abilities.AbilityRegistry;
import net.revilodev.codex.abilities.PlayerAbilities;
import net.revilodev.codex.abilities.logic.AbilityScaling;
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.skills.SkillsAttachments;

@OnlyIn(Dist.CLIENT)
public final class AbilityHudOverlay {
    private AbilityHudOverlay() {}

    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !AbilityConfig.hudEnabled()) return;

        PlayerAbilities abilities = mc.player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
        PlayerSkills skills = mc.player.getData(SkillsAttachments.PLAYER_SKILLS.get());
        GuiGraphics gg = event.getGuiGraphics();

        int x = 8;
        int y = gg.guiHeight() - 28;
        for (int slot = 1; slot <= AbilityConfig.maxSlots(); slot++) {
            drawSlot(gg, mc.font, x + (slot - 1) * 22, y, slot, abilities, skills);
        }
    }

    private static void drawSlot(GuiGraphics gg, Font font, int x, int y, int slot, PlayerAbilities abilities, PlayerSkills skills) {
        AbilityId id = abilities.slot(slot);
        gg.fill(x, y, x + 20, y + 20, 0xB0101010);
        gg.fill(x - 1, y - 1, x + 21, y, 0x80383838);
        gg.fill(x - 1, y + 20, x + 21, y + 21, 0x80383838);
        gg.fill(x - 1, y, x, y + 20, 0x80383838);
        gg.fill(x + 20, y, x + 21, y + 20, 0x80383838);

        if (id == null) return;

        AbilityDefinition def = AbilityRegistry.def(id);
        if (def != null) gg.renderItem(new ItemStack(def.iconItem()), x + 2, y + 2);

        int remaining = abilities.cooldownTicks(id);
        if (remaining > 0) {
            int rank = Math.max(1, abilities.rank(id));
            int max = Math.max(1, AbilityScaling.cooldownTicks(id, rank, skills));
            int overlay = (int) Math.ceil(16.0D * remaining / (double) max);
            gg.fill(x + 2, y + 18 - overlay, x + 18, y + 18, 0xA0000000);
            if (AbilityConfig.hudTimerText()) {
                String text = remaining > 20 ? Integer.toString((int) Math.ceil(remaining / 20.0D)) : String.format(java.util.Locale.ROOT, "%.1f", remaining / 20.0D);
                gg.drawCenteredString(font, text, x + 10, y + 6, 0xFFFFFF);
            }
        }
    }
}
