package net.revilodev.codex.skills;

import java.util.List;

public final class Skills {
    private Skills() {}

    public static SkillDefinition def(SkillId id) {
        return SkillRegistry.def(id);
    }

    public static List<SkillDefinition> list(SkillCategory category) {
        return SkillRegistry.skillsFor(category);
    }
}
