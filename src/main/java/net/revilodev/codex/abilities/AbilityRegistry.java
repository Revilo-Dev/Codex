package net.revilodev.codex.abilities;

import java.util.EnumMap;
import java.util.List;

public final class AbilityRegistry {
    private static final EnumMap<AbilityId, AbilityDefinition> DEFINITIONS = new EnumMap<>(AbilityId.class);
    private static final List<AbilityDefinition> ALL;

    static {
        java.util.ArrayList<AbilityDefinition> defs = new java.util.ArrayList<>();
        for (AbilityId id : AbilityId.values()) {
            AbilityDefinition def = AbilityDefinition.fromId(id);
            DEFINITIONS.put(id, def);
            defs.add(def);
        }
        ALL = List.copyOf(defs);
    }

    private AbilityRegistry() {}

    public static AbilityDefinition def(AbilityId id) {
        return id == null ? null : DEFINITIONS.get(id);
    }

    public static List<AbilityDefinition> all() {
        return ALL;
    }
}
