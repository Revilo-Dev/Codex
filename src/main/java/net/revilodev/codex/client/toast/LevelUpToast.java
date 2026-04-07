package net.revilodev.codex.client.toast;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class LevelUpToast {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("codex", "textures/gui/sprites/skill_toast.png");
    private static final long DISPLAY_TIME_MS = 5000L;
    private static final long SLIDE_TIME_MS = 350L;
    private static final int BOX_WIDTH = 160;
    private static final int BOX_HEIGHT = 32;
    private static final int RIGHT_MARGIN = 8;
    private static final int TOP_MARGIN = 8;
    private static final int TITLE_COLOR = 0x000000;
    private static final int SKILL_COLOR = 0x1F4E8C;
    private static final int ABILITY_COLOR = 0x7A3DB8;

    private static Component title = Component.empty();
    private static Component skillLine = Component.empty();
    private static Component abilityLine = Component.empty();
    private static long shownAt = -1L;

    private LevelUpToast() {}

    public static void show(int oldLevel, int newLevel, int skillPointsGained, int abilityPointsGained) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        title = Component.literal("Leveled Up " + oldLevel + " -> " + newLevel);
        skillLine = Component.literal("+" + Math.max(0, skillPointsGained) + " skill point");
        abilityLine = abilityPointsGained > 0
                ? Component.literal(" | +" + abilityPointsGained + " Ability point")
                : Component.empty();
        shownAt = Util.getMillis();
    }

    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options.hideGui || shownAt < 0L) return;

        long elapsed = Util.getMillis() - shownAt;
        if (elapsed >= DISPLAY_TIME_MS) {
            shownAt = -1L;
            return;
        }

        GuiGraphics gg = event.getGuiGraphics();
        Font font = mc.font;
        long visibleFor = DISPLAY_TIME_MS;
        float slide = toastSlide(elapsed, visibleFor);
        int hiddenX = gg.guiWidth();
        int shownX = gg.guiWidth() - BOX_WIDTH - RIGHT_MARGIN;
        int x = Math.round(hiddenX + (shownX - hiddenX) * slide);
        int y = TOP_MARGIN;

        gg.pose().pushPose();
        gg.pose().translate(0.0F, 0.0F, 800.0F);
        gg.blit(TEXTURE, x, y, 0, 0, BOX_WIDTH, BOX_HEIGHT, BOX_WIDTH, BOX_HEIGHT);

        gg.drawString(font, title, x + 8, y + 7, TITLE_COLOR, false);
        gg.drawString(font, skillLine, x + 8, y + 18, SKILL_COLOR, false);
        if (!abilityLine.getString().isEmpty()) {
            gg.drawString(font, abilityLine, x + 8 + font.width(skillLine), y + 18, ABILITY_COLOR, false);
        }
        gg.pose().popPose();
    }

    private static float toastSlide(long elapsed, long visibleFor) {
        if (elapsed <= SLIDE_TIME_MS) {
            return easeOut(elapsed / (float) SLIDE_TIME_MS);
        }
        if (elapsed >= visibleFor - SLIDE_TIME_MS) {
            return easeOut(Math.max(0.0F, (visibleFor - elapsed) / (float) SLIDE_TIME_MS));
        }
        return 1.0F;
    }

    private static float easeOut(float value) {
        float clamped = Math.max(0.0F, Math.min(1.0F, value));
        float inv = 1.0F - clamped;
        return 1.0F - inv * inv * inv;
    }
}
