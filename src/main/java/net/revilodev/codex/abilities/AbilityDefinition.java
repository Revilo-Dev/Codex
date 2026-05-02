package net.revilodev.codex.abilities;

import net.minecraft.resources.ResourceLocation;

public record AbilityDefinition(
        AbilityId id,
        String title,
        String description,
        ResourceLocation iconTexture,
        AbilityElement element,
        AbilitySpecialization specialization,
        AbilityNodeType type,
        AbilityId required,
        int defaultMaxRank,
        int baseCooldownTicks
) {
    public static AbilityDefinition fromId(AbilityId id) {
        return new AbilityDefinition(id, id.title(), id.description(), id.iconTexture(), id.element(), id.specialization(), id.type(), id.required(), id.defaultMaxRank(), id.baseCooldownTicks());
    }

    public int maxRank() {
        return id.maxRank();
    }
}
