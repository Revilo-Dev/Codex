package net.revilodev.codex.skills;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Optional;

public record Category(String id, String name, ResourceLocation icon) {
    public Optional<Item> iconItem() {
        return BuiltInRegistries.ITEM.getOptional(icon);
    }

    public static Category of(SkillCategory c) {
        return new Category(c.id, c.title, c.icon);
    }
}
