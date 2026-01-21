package net.revilodev.codex.skills;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

public final class SkillRegistry {
    private SkillRegistry() {}

    private static final EnumMap<SkillCategory, List<SkillDef>> BY_CATEGORY = new EnumMap<>(SkillCategory.class);

    static {
        for (SkillCategory c : SkillCategory.values()) BY_CATEGORY.put(c, new ArrayList<>());
        for (SkillId id : SkillId.values()) BY_CATEGORY.get(id.category()).add(new SkillDef(id));
    }

    public static List<Category> categoriesOrdered() {
        List<Category> out = new ArrayList<>();
        out.add(Category.of(SkillCategory.COMBAT));
        out.add(Category.of(SkillCategory.SURVIVAL));
        out.add(Category.of(SkillCategory.UTILITY));
        return out;
    }

    public static List<SkillDef> skillsFor(String categoryId) {
        return skillsFor(SkillCategory.byId(categoryId));
    }

    public static List<SkillDef> skillsFor(SkillCategory c) {
        List<SkillDef> list = BY_CATEGORY.get(c);
        return list == null ? Collections.emptyList() : list;
    }

    public static SkillDef def(SkillId id) {
        return id == null ? null : new SkillDef(id);
    }
}
