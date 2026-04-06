package net.revilodev.codex.abilities;

import net.minecraft.resources.ResourceLocation;
import net.revilodev.codex.skills.SkillId;

public record AbilityDefinition(
        AbilityId id,
        String title,
        String description,
        ResourceLocation iconTexture,
        SkillId scalingSkill,
        int defaultMaxRank,
        int baseCooldownTicks
) {
    public static AbilityDefinition fromId(AbilityId id) {
        return new AbilityDefinition(id, id.title(), id.description(), id.iconTexture(), id.scalingSkill(), id.maxRank(), id.baseCooldownTicks());
    }

    public int maxRank() {
        return id.maxRank();
    }
}
