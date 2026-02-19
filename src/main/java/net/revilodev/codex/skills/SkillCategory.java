package net.revilodev.codex.skills;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Locale;

public enum SkillCategory {
    UTILITY("utility", "Gathering", ResourceLocation.parse("minecraft:iron_pickaxe")),
    COMBAT("combat", "Combat", ResourceLocation.parse("minecraft:iron_sword")),
    SURVIVAL("survival", "Survival", ResourceLocation.parse("minecraft:iron_chestplate"));


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
        if (id == null) return COMBAT;
        String s = id.toLowerCase(Locale.ROOT);
        for (SkillCategory c : values()) if (c.id.equals(s)) return c;
        return COMBAT;
    }
}
