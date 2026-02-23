package net.revilodev.codex.client.toast;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.revilodev.codex.skills.SkillCategory;

public final class SkillPointToast implements Toast {
    private static final Object GLOBAL_TOKEN = new Object();
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("codex", "textures/gui/sprites/skill_toast.png");
    private static final long DISPLAY_TIME_MS = 5000L;
    private final SkillCategory category;
    private final Object token;
    private final Component title;
    private Component subtitle;
    private final Item icon;
    private int points;
    private long lastChanged;
    private boolean changed;

    public SkillPointToast(SkillCategory category, int points, Component title, Component subtitle, Item icon) {
        this(category, category, points, title, subtitle, icon);
    }

    private SkillPointToast(Object token, SkillCategory category, int points, Component title, Component subtitle, Item icon) {
        this.token = token;
        this.category = category;
        this.title = title;
        this.subtitle = subtitle;
        this.icon = icon;
        this.points = points;
    }

    public static void show(SkillCategory category, int delta, int total) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        ToastComponent toasts = mc.getToasts();
        SkillPointToast existing = toasts.getToast(SkillPointToast.class, category);
        if (existing != null) {
            existing.addPoints(delta);
            return;
        }

        String deltaText = delta == 1 ? "+1" : ("+" + delta);
        toasts.addToast(new SkillPointToast(
                category,
                delta,
                Component.literal("Skill point earned"),
                Component.literal(category.title() + " " + deltaText),
                category.icon()
        ));
    }

    public static void showGlobal(int delta, int total) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        ToastComponent toasts = mc.getToasts();
        SkillPointToast existing = toasts.getToast(SkillPointToast.class, GLOBAL_TOKEN);
        if (existing != null) {
            existing.addPoints(delta);
            return;
        }

        String deltaText = delta == 1 ? "+1" : ("+" + delta);
        toasts.addToast(new SkillPointToast(
                GLOBAL_TOKEN,
                SkillCategory.COMBAT,
                delta,
                Component.literal("Skill point earned"),
                Component.literal("Global " + deltaText),
                SkillCategory.COMBAT.icon()
        ));
    }

    public Visibility render(GuiGraphics gg, ToastComponent component, long time) {
        if (changed) {
            lastChanged = time;
            changed = false;
        }
        gg.blit(TEXTURE, 0, 0, 0, 0, this.width(), this.height(), this.width(), this.height());
        if (icon != null) gg.renderItem(new ItemStack(icon), 6, 6);
        gg.drawString(Minecraft.getInstance().font, title, 30, 7, 0x242424, false);
        gg.drawString(Minecraft.getInstance().font, subtitle, 30, 18, 0x8f8f8f, false);
        return time - lastChanged >= DISPLAY_TIME_MS ? Visibility.HIDE : Visibility.SHOW;
    }

    public int width() { return 160; }
    public int height() { return 32; }

    @Override
    public Object getToken() {
        return token;
    }

    private void addPoints(int delta) {
        if (delta <= 0) return;
        points += delta;
        String deltaText = points == 1 ? "+1" : ("+" + points);
        subtitle = Component.literal(category.title() + " " + deltaText);
        changed = true;
    }
}
