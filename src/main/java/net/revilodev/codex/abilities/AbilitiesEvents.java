package net.revilodev.codex.abilities;

import net.revilodev.codex.abilities.logic.AbilityLevelIntegrationEvents;
import net.revilodev.codex.abilities.logic.AbilityPowerEnchantmentEvents;
import net.revilodev.codex.abilities.logic.AbilitySyncEvents;

public final class AbilitiesEvents {
    private static boolean REGISTERED = false;

    private AbilitiesEvents() {}

    public static void register() {
        if (REGISTERED) return;
        REGISTERED = true;

        AbilitySyncEvents.register();
        AbilityLevelIntegrationEvents.register();
        AbilityPowerEnchantmentEvents.register();
    }
}
