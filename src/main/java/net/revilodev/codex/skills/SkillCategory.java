package net.revilodev.codex.skills;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Locale;

public enum SkillCategory {
    STRENGTH("strength", "Strength", ResourceLocation.parse("minecraft:iron_sword")),
    RESISTANCE("resistance", "Resistance", ResourceLocation.parse("minecraft:shield")),
    AGILITY("agility", "Agility", ResourceLocation.parse("minecraft:rabbit_foot")),
    VITALITY("vitality", "Vitality", ResourceLocation.parse("minecraft:golden_apple")),
    LUCK("luck", "Luck", ResourceLocation.parse("minecraft:emerald"));


    public final String id;
    public final String title;
    public final ResourceLocation icon;

    SkillCategory(String id, String title, ResourceLocation icon) {
        this.id = id;
        this.title = title;
        this.icon = icon;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public ResourceLocation iconId() {
        return icon;
    }

    public Item icon() {
        return BuiltInRegistries.ITEM.getOptional(icon).orElse(Items.BOOK);
    }

    public static SkillCategory byId(String id) {
        if (id == null) return STRENGTH;
        String s = id.toLowerCase(Locale.ROOT);
        for (SkillCategory c : values()) if (c.id.equals(s)) return c;
        return STRENGTH;
    }
}
