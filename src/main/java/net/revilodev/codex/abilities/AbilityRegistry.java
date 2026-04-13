package net.revilodev.codex.abilities;

import java.util.EnumMap;
import java.util.List;

public final class AbilityRegistry {
    private static final EnumMap<AbilityId, AbilityDefinition> DEFINITIONS = new EnumMap<>(AbilityId.class);
    private static final List<AbilityDefinition> ALL;
    private static final List<AbilityId> DISPLAY_ORDER = List.of(
            AbilityId.LEAP,
            AbilityId.DASH,
            AbilityId.LUNGE,
            AbilityId.HEAL,
            AbilityId.CLEANSE,
            AbilityId.WARCRY,
            AbilityId.CLEAVE,
            AbilityId.EXECUTION,
            AbilityId.OVERPOWER,
            AbilityId.GUARD,
            AbilityId.SCAVENGER,
            AbilityId.BLAZE,
            AbilityId.BLAST,
            AbilityId.GLACIER,
            AbilityId.SMITE
    );

    static {
        for (AbilityId id : AbilityId.values()) {
            AbilityDefinition def = AbilityDefinition.fromId(id);
            DEFINITIONS.put(id, def);
        }
        java.util.ArrayList<AbilityDefinition> defs = new java.util.ArrayList<>();
        for (AbilityId id : DISPLAY_ORDER) {
            AbilityDefinition def = DEFINITIONS.get(id);
            if (def != null) defs.add(def);
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
