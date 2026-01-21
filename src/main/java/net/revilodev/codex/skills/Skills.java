package net.revilodev.codex.skills;

import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public final class Skills {
    private Skills() {}

    private static final EnumMap<SkillId, SkillDefinition> DEFINITIONS = new EnumMap<>(SkillId.class);

    static {
        def(SkillId.SHARPNESS, Items.NETHERITE_SWORD, "Increases melee damage.", 50);
        def(SkillId.POWER, Items.BOW, "Increases projectile damage you deal.", 50);
        def(SkillId.CRIT_BONUS, Items.DIAMOND_SWORD, "Increases critical hit damage.", 50);
        def(SkillId.FIRE_RESISTANCE, Items.BLAZE_POWDER, "Reduces fire damage taken.", 50);
        def(SkillId.BLAST_RESISTANCE, Items.TNT, "Reduces explosion damage taken.", 50);
        def(SkillId.PROJECTILE_RESISTANCE, Items.ARROW, "Reduces projectile damage taken.", 50);
        def(SkillId.KNOCKBACK_RESISTANCE, Items.SHIELD, "Reduces knockback strength.", 50);

        def(SkillId.HEALTH, Items.GOLDEN_APPLE, "Increases max health.", 50);
        def(SkillId.REGENERATION, Items.GHAST_TEAR, "Grants passive regeneration.", 50);
        def(SkillId.SWIFTNESS, Items.SUGAR, "Increases movement speed.", 50);
        def(SkillId.DEFENSE, Items.IRON_CHESTPLATE, "Increases armor.", 50);
        def(SkillId.SATURATION, Items.COOKED_BEEF, "Improves food saturation over time.", 50);
        def(SkillId.LEAPING, Items.RABBIT_FOOT, "Increases jump height.", 50);

        def(SkillId.EFFICIENCY, Items.IRON_PICKAXE, "Increases mining speed.", 50);
        def(SkillId.CHOPPING, Items.IRON_AXE, "Increases chopping speed with axes.", 50);
        def(SkillId.FORAGING, Items.WHEAT, "Improves foraging utility.", 50);
        def(SkillId.FORTUNE, Items.EMERALD, "Improves overall luck while gathering.", 50);
    }

    private static void def(SkillId id, net.minecraft.world.item.Item icon, String desc, int max) {
        DEFINITIONS.put(id, new SkillDefinition(id, id.category(), id.title(), icon, desc, max));
    }

    public static SkillDefinition def(SkillId id) {
        return DEFINITIONS.get(id);
    }

    public static List<SkillDefinition> list(SkillCategory category) {
        List<SkillDefinition> out = new ArrayList<>();
        for (SkillId id : SkillId.values()) {
            if (id.category() == category) out.add(def(id));
        }
        return out;
    }
}
