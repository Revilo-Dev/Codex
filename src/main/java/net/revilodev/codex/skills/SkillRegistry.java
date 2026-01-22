package net.revilodev.codex.skills;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

public final class SkillRegistry {
    private SkillRegistry() {}

    private static final EnumMap<SkillId, SkillDefinition> DEFINITIONS = new EnumMap<>(SkillId.class);
    private static final EnumMap<SkillCategory, List<SkillDefinition>> BY_CATEGORY = new EnumMap<>(SkillCategory.class);

    static {
        for (SkillCategory c : SkillCategory.values()) BY_CATEGORY.put(c, new ArrayList<>());
        for (SkillId id : SkillId.values()) {
            SkillDefinition def = new SkillDefinition(id, id.category(), id.title(), id.icon(), id.description(), id.maxLevel());
            DEFINITIONS.put(id, def);
            BY_CATEGORY.get(id.category()).add(def);
        }
    }

    public static SkillDefinition def(SkillId id) {
        return id == null ? null : DEFINITIONS.get(id);
    }

    public static SkillDef defLegacy(SkillId id) {
        return id == null ? null : new SkillDef(id);
    }

    public static List<SkillDefinition> skillsFor(String categoryId) {
        return skillsFor(SkillCategory.byId(categoryId));
    }

    public static List<SkillDefinition> skillsFor(SkillCategory c) {
        List<SkillDefinition> list = BY_CATEGORY.get(c);
        return list == null ? Collections.emptyList() : list;
    }

    public static List<Category> categoriesOrdered() {
        List<Category> out = new ArrayList<>();
        out.add(Category.of(SkillCategory.COMBAT));
        out.add(Category.of(SkillCategory.SURVIVAL));
        out.add(Category.of(SkillCategory.UTILITY));
        return out;
    }

    public record Category(String id, String name, ResourceLocation iconItem) {
        public static Category of(SkillCategory c) {
            return new Category(c.id, c.title, c.icon);
        }
    }
}
