package net.revilodev.codex.client;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum PanelTab {
    SKILLS("Skills", Items.IRON_SWORD),
    ABILITIES("Abilities", Items.FIRE_CHARGE);

    private final String title;
    private final Item icon;

    PanelTab(String title, Item icon) {
        this.title = title;
        this.icon = icon;
    }

    public String title() {
        return title;
    }

    public Item icon() {
        return icon;
    }
}
