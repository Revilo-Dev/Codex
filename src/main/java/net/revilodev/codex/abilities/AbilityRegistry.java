package net.revilodev.codex.abilities;

import java.util.EnumMap;
import java.util.List;

public final class AbilityRegistry {
    private static final EnumMap<AbilityId, AbilityDefinition> DEFINITIONS = new EnumMap<>(AbilityId.class);
    private static final List<AbilityDefinition> ALL;
    private static final List<AbilityId> DISPLAY_ORDER = List.of(
            AbilityId.FIRE, AbilityId.FIRE_BURST, AbilityId.FIRE_NOVA, AbilityId.FIRE_IMPLODE, AbilityId.FIRE_STORM,
            AbilityId.ICE, AbilityId.ICE_BURST, AbilityId.ICE_NOVA, AbilityId.ICE_PIERCE, AbilityId.ICE_IMPLODE, AbilityId.ICE_GLACIER, AbilityId.ICE_STORM,
            AbilityId.LIGHTNING, AbilityId.LIGHTNING_STRIKE, AbilityId.LIGHTNING_ZAP, AbilityId.LIGHTNING_NOVA, AbilityId.LIGHTNING_IMPLODE, AbilityId.LIGHTNING_STORM,
            AbilityId.POISON, AbilityId.POISON_BURST, AbilityId.POISON_NOVA, AbilityId.POISON_IMPLODE,
            AbilityId.FORCE, AbilityId.FORCE_AEGIS, AbilityId.FORCE_BURST, AbilityId.FORCE_RAMPAGE,
            AbilityId.MAGIC, AbilityId.MAGIC_HEAL, AbilityId.MAGIC_CLEANSE,
            AbilityId.WIND, AbilityId.WIND_DASH, AbilityId.WIND_LEAP, AbilityId.WIND_LUNGE
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
