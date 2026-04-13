package net.revilodev.codex.client.abilities;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class AbilityHudOverlay {
    private static final int SLOT_SIZE = 20;
    private static final int SLOT_STEP = 22;
    private static final int GRID_COLUMNS = 5;
    private static final long FAIL_FLASH_MS = 350L;
    private static final Map<AbilityId, FailureState> FAILURES = new EnumMap<>(AbilityId.class);

    private AbilityHudOverlay() {}

    public static void notifyFailedUse(AbilityId id, AbilityKeybinds.AbilityUseFail reason) {
        if (id == null) return;
        FailureState state = FAILURES.computeIfAbsent(id, ignored -> new FailureState());
        state.reason = reason;
        state.untilMs = System.currentTimeMillis() + FAIL_FLASH_MS;
    }

    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !AbilityConfig.hudEnabled()) return;

        PlayerAbilities abilities = mc.player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
        PlayerSkills skills = mc.player.getData(SkillsAttachments.PLAYER_SKILLS.get());
        GuiGraphics gg = event.getGuiGraphics();

        boolean showGrid = Screen.hasAltDown();
        List<AbilityId> displayed = showGrid
                ? AbilityRegistry.all().stream().map(AbilityDefinition::id).filter(abilities::unlocked).toList()
                : abilities.recentAbilities();
        if (displayed.isEmpty()) return;

        int baseX = 8;
        if (showGrid) {
            int rows = (int) Math.ceil(displayed.size() / (double) GRID_COLUMNS);
            int baseY = gg.guiHeight() - 8 - (rows * SLOT_STEP);
            for (int i = 0; i < displayed.size(); i++) {
                int col = i % GRID_COLUMNS;
                int row = i / GRID_COLUMNS;
                drawAbility(gg, mc.font, baseX + col * SLOT_STEP, baseY + row * SLOT_STEP, displayed.get(i), abilities, skills, displayed.get(i) == AbilityKeybinds.altSelection());
            }
        } else {
            int baseY = gg.guiHeight() - 28;
            for (int i = 0; i < displayed.size(); i++) {
                drawAbility(gg, mc.font, baseX + i * SLOT_STEP, baseY, displayed.get(i), abilities, skills, false);
            }
        }
    }

    private static void drawAbility(GuiGraphics gg, Font font, int x, int y, AbilityId id, PlayerAbilities abilities, PlayerSkills skills, boolean selected) {
        if (id == null) return;
        FailureState failure = activeFailure(id);
        int shakeX = 0;
        if (failure != null) {
            double progress = (failure.untilMs - System.currentTimeMillis()) / (double) FAIL_FLASH_MS;
            progress = Math.max(0.0D, Math.min(1.0D, progress));
            shakeX = (int) Math.round(Math.sin(progress * Math.PI * 4.0D) * 1.5D * progress);
        }
        int drawX = x + shakeX;
        int borderColor = selected ? 0xFFB67CFF : 0x80383838;
        int fillColor = failure != null ? 0xC0321212 : 0xB0101010;

        gg.fill(drawX, y, drawX + SLOT_SIZE, y + SLOT_SIZE, fillColor);
        gg.fill(drawX - 1, y - 1, drawX + SLOT_SIZE + 1, y, borderColor);
        gg.fill(drawX - 1, y + SLOT_SIZE, drawX + SLOT_SIZE + 1, y + SLOT_SIZE + 1, borderColor);
        gg.fill(drawX - 1, y, drawX, y + SLOT_SIZE, borderColor);
        gg.fill(drawX + SLOT_SIZE, y, drawX + SLOT_SIZE + 1, y + SLOT_SIZE, borderColor);

        AbilityDefinition def = AbilityRegistry.def(id);
        if (def != null) gg.blit(def.iconTexture(), drawX + 2, y + 2, 0, 0, 16, 16, 16, 16);

        int remaining = abilities.cooldownTicks(id);
        if (remaining > 0) {
            int rank = Math.max(1, abilities.rank(id));
            int max = Math.max(1, AbilityScaling.cooldownTicks(id, rank, skills));
            int overlay = (int) Math.ceil(16.0D * remaining / (double) max);
            gg.fill(drawX + 2, y + 18 - overlay, drawX + 18, y + 18, 0xA0000000);
            if (AbilityConfig.hudTimerText()) {
                String text = remaining > 20 ? Integer.toString((int) Math.ceil(remaining / 20.0D)) : String.format(java.util.Locale.ROOT, "%.1f", remaining / 20.0D);
                gg.drawCenteredString(font, text, drawX + 10, y + 6, 0xFFFFFF);
            }
        }

        String keybind = compactKeybind(AbilityKeybinds.keyName(id));
        int labelWidth = Math.max(1, font.width(keybind));
        int labelX = drawX + SLOT_SIZE - labelWidth - 1;
        int labelY = y + SLOT_SIZE - font.lineHeight + 1;
        int textColor = failure != null ? 0xFF7878 : (abilities.unlocked(id) ? 0xFFF2D2 : 0x909090);
        drawOutlinedText(gg, font, keybind, labelX, labelY, textColor, 0xFF000000);
    }

    private static String compactKeybind(String keybind) {
        if (keybind == null || keybind.isBlank()) return "?";
        String cleaned = keybind.replace("key.keyboard.", "")
                .replace("KEY.", "")
                .replace("NumPad-", "N")
                .replace("NUMPAD", "N")
                .replace("Button ", "M");
        if (cleaned.length() <= 3) return cleaned.toUpperCase(java.util.Locale.ROOT);

        String[] parts = cleaned.split("\\s+");
        if (parts.length > 1) {
            StringBuilder out = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty()) out.append(Character.toUpperCase(part.charAt(0)));
            }
            if (!out.isEmpty()) return out.toString();
        }
        return cleaned.substring(0, Math.min(3, cleaned.length())).toUpperCase(java.util.Locale.ROOT);
    }

    private static void drawOutlinedText(GuiGraphics gg, Font font, String text, int x, int y, int color, int outlineColor) {
        gg.drawString(font, text, x - 1, y, outlineColor, false);
        gg.drawString(font, text, x + 1, y, outlineColor, false);
        gg.drawString(font, text, x, y - 1, outlineColor, false);
        gg.drawString(font, text, x, y + 1, outlineColor, false);
        gg.drawString(font, text, x, y, color, false);
    }

    private static FailureState activeFailure(AbilityId id) {
        FailureState state = FAILURES.get(id);
        if (state == null) return null;
        if (System.currentTimeMillis() > state.untilMs) {
            FAILURES.remove(id);
            return null;
        }
        return state;
    }

    private static final class FailureState {
        AbilityKeybinds.AbilityUseFail reason;
        long untilMs;
    }
}
