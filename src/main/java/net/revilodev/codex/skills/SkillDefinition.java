package net.revilodev.codex.skills;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Optional;

public record SkillDefinition(
        SkillId id,
        SkillCategory category,
        boolean primary,
        SkillId parent,
        String title,
        ResourceLocation icon,
        String description,
        int defaultMaxLevel
) {
    public boolean secondary() {
        return !primary;
    }

    public int maxLevel() {
        return id.maxLevel();
    }

    public Optional<Item> iconItem() {
        return BuiltInRegistries.ITEM.getOptional(icon);
    }
}
