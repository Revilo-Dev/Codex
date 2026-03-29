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
    private static final int BOX_WIDTH = 160;
    private static final int BOX_HEIGHT = 32;
    private static final int TOP_MARGIN = 8;
    private static final float TITLE_SCALE = 1.15F;
    private static final int TITLE_COLOR = 0xF4C542;
    private static final int SUBTITLE_COLOR = 0x4A90E2;

    private static Component title = Component.empty();
    private static Component subtitle = Component.empty();
    private static long shownAt = -1L;

    private LevelUpToast() {}

    public static void show(int newLevel, int levelsGained) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        title = Component.translatable("toast.codex.level_up");
        int pointsGained = Math.max(1, levelsGained);
        String suffix = pointsGained == 1 ? " skill point" : " skill points";
        subtitle = Component.literal("+" + pointsGained + suffix);
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
        int x = (gg.guiWidth() - BOX_WIDTH) / 2;
        int y = TOP_MARGIN;
        int centerX = x + BOX_WIDTH / 2;

        gg.pose().pushPose();
        gg.pose().translate(0.0F, 0.0F, 800.0F);
        gg.blit(TEXTURE, x, y, 0, 0, BOX_WIDTH, BOX_HEIGHT, BOX_WIDTH, BOX_HEIGHT);

        gg.pose().pushPose();
        gg.pose().translate(centerX, y + 6, 0.0F);
        gg.pose().scale(TITLE_SCALE, TITLE_SCALE, 1.0F);
        gg.drawString(font, title, -font.width(title) / 2, 0, TITLE_COLOR, false);
        gg.pose().popPose();

        gg.drawCenteredString(font, subtitle, centerX, y + 20, SUBTITLE_COLOR);
        gg.pose().popPose();
    }
}
