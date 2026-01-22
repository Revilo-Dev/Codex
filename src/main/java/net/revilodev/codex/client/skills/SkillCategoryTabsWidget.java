package net.revilodev.codex.client.skills;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.skills.SkillRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class SkillCategoryTabsWidget extends AbstractWidget {
    private static final ResourceLocation TAB =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/tab.png");
    private static final ResourceLocation TAB_SELECTED =
            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/sprites/tab_selected.png");

    private static final int MAX_TABS = 5;

    private final Minecraft mc = Minecraft.getInstance();
    private final Consumer<String> onSelect;
    private final List<SkillRegistry.Category> categories = new ArrayList<>();
    private String selected = "";

    private int cellW = 26;
    private int cellH = 26;
    private int gap = 2;

    private Component pendingTooltip;
    private int pendingTooltipX;
    private int pendingTooltipY;

    public SkillCategoryTabsWidget(int x, int y, int w, int h, Consumer<String> onSelect) {
        super(x, y, w, h, Component.empty());
        this.onSelect = onSelect;
    }

    public void setBounds(int x, int y, int w, int h) {
        this.setX(x);
        this.setY(y);
        this.width = w;
        this.height = h;
    }

    public void setCategories(List<SkillRegistry.Category> list) {
        categories.clear();

        int count = 0;
        for (SkillRegistry.Category c : list) {
            if (c == null) continue;
            categories.add(c);
            count++;
            if (count >= MAX_TABS) break;
        }

        if (!categories.isEmpty()) {
            boolean hasSelected = false;
            for (SkillRegistry.Category c : categories) {
                if (c.id().equalsIgnoreCase(selected)) {
                    hasSelected = true;
                    break;
                }
            }
            if (!hasSelected) selected = categories.get(0).id();
        } else {
            selected = "";
        }
    }

    public void setSelected(String id) {
        this.selected = id == null ? "" : id;
    }

    public String selected() {
        return selected;
    }

    public void renderHoverTooltipOnTop(GuiGraphics gg) {
        if (pendingTooltip == null) return;

        gg.pose().pushPose();
        gg.pose().translate(0.0F, 0.0F, 500.0F);
        gg.renderTooltip(mc.font, pendingTooltip, pendingTooltipX, pendingTooltipY);
        gg.pose().popPose();

        pendingTooltip = null;
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        pendingTooltip = null;

        int x = getX();
        int y = getY();

        for (int i = 0; i < Math.min(categories.size(), MAX_TABS); i++) {
            SkillRegistry.Category c = categories.get(i);
            int top = y + i * (cellH + gap);

            boolean sel = !selected.isBlank() && c.id().equalsIgnoreCase(selected);
            ResourceLocation tex = sel ? TAB_SELECTED : TAB;

            gg.blit(tex, x, top, 0, 0, cellW, cellH, cellW, cellH);

            Item it = BuiltInRegistries.ITEM.getOptional(c.iconItem()).orElse(null);
            if (it != null) gg.renderItem(new ItemStack(it), x + 5, top + 5);

            boolean hover = mouseX >= x && mouseX < x + cellW && mouseY >= top && mouseY < top + cellH;
            if (hover) {
                pendingTooltip = Component.literal(c.name());
                pendingTooltipX = mouseX;
                pendingTooltipY = mouseY;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !active) return false;
        if (button != 0) return false;

        int x = getX();
        int y = getY();

        for (int i = 0; i < Math.min(categories.size(), MAX_TABS); i++) {
            int top = y + i * (cellH + gap);
            if (mouseX >= x && mouseX < x + cellW && mouseY >= top && mouseY < top + cellH) {
                String id = categories.get(i).id();
                selected = id;
                if (onSelect != null) onSelect.accept(id);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {}
}
