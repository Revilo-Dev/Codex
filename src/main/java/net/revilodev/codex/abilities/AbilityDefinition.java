package net.revilodev.codex.abilities;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.revilodev.codex.skills.SkillId;

public record AbilityDefinition(
        AbilityId id,
        String title,
        String description,
        Item iconItem,
        SkillId scalingSkill,
        int maxRank,
        int baseCooldownTicks
) {
    public static AbilityDefinition fromId(AbilityId id) {
        Item icon = BuiltInRegistries.ITEM.getOptional(id.iconItem()).orElse(Items.NETHER_STAR);
        return new AbilityDefinition(id, id.title(), id.description(), icon, id.scalingSkill(), id.maxRank(), id.baseCooldownTicks());
    }
}
