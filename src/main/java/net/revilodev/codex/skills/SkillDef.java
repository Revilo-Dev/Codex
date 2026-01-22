package net.revilodev.codex.skills;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Optional;

public record SkillDef(
        SkillId id,
        SkillCategory category,
        String title,
        ResourceLocation icon,
        String description,
        int maxLevel
) {
    public SkillDef(SkillId id) {
        this(id, id.category(), id.title(), id.icon(), id.description(), id.maxLevel());
    }

    public Optional<Item> iconItem() {
        return BuiltInRegistries.ITEM.getOptional(icon);
    }
}
