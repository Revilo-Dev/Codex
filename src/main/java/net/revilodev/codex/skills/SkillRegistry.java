package net.revilodev.codex.skills;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

public final class SkillRegistry {
    private SkillRegistry() {}

    private static final EnumMap<SkillId, SkillDefinition> DEFINITIONS = new EnumMap<>(SkillId.class);
    private static final EnumMap<SkillCategory, List<SkillDefinition>> BY_CATEGORY = new EnumMap<>(SkillCategory.class);
    private static final List<SkillDefinition> PRIMARY_SKILLS;
    private static final EnumMap<SkillId, List<SkillDefinition>> SECONDARY_BY_PARENT = new EnumMap<>(SkillId.class);

    static {
        for (SkillCategory c : SkillCategory.values()) BY_CATEGORY.put(c, new java.util.ArrayList<>());
        for (SkillId id : SkillId.values()) SECONDARY_BY_PARENT.put(id, Collections.emptyList());

        List<SkillDefinition> primaries = new java.util.ArrayList<>();
        EnumMap<SkillId, java.util.ArrayList<SkillDefinition>> secondaryLists = new EnumMap<>(SkillId.class);
        for (SkillId id : SkillId.values()) {
            SkillDefinition def = new SkillDefinition(id, id.category(), id.primary(), id.parent(), id.title(), id.icon(), id.description(), id.defaultMaxLevel());
            DEFINITIONS.put(id, def);
            BY_CATEGORY.get(id.category()).add(def);
            if (def.primary()) {
                primaries.add(def);
            } else if (def.parent() != null) {
                secondaryLists.computeIfAbsent(def.parent(), ignored -> new java.util.ArrayList<>()).add(def);
            }
        }

        for (SkillCategory category : SkillCategory.values()) {
            BY_CATEGORY.put(category, List.copyOf(BY_CATEGORY.get(category)));
        }
        PRIMARY_SKILLS = List.copyOf(primaries);
        for (SkillId id : SkillId.values()) {
            List<SkillDefinition> children = secondaryLists.get(id);
            SECONDARY_BY_PARENT.put(id, children == null ? Collections.emptyList() : List.copyOf(children));
        }
    }

    public static SkillDefinition def(SkillId id) {
        return id == null ? null : DEFINITIONS.get(id);
    }

    public static List<SkillDefinition> skillsFor(SkillCategory c) {
        List<SkillDefinition> list = BY_CATEGORY.get(c);
        return list == null ? Collections.emptyList() : list;
    }

    public static List<SkillDefinition> primarySkills() {
        return PRIMARY_SKILLS;
    }

    public static List<SkillDefinition> secondarySkillsFor(SkillId parent) {
        if (parent == null) return Collections.emptyList();
        return SECONDARY_BY_PARENT.getOrDefault(parent, Collections.emptyList());
    }
}
