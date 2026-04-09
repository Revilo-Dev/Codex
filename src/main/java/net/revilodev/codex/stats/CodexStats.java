package net.revilodev.codex.stats;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.abilities.AbilityId;

import java.util.EnumMap;
import java.util.Map;

public final class CodexStats {
    public static final ResourceLocation ABILITIES_USED = id("abilities_used");
    private static final Map<AbilityId, ResourceLocation> ABILITY_USE_STATS = new EnumMap<>(AbilityId.class);

    static {
        for (AbilityId abilityId : AbilityId.values()) {
            ABILITY_USE_STATS.put(abilityId, id("ability_used_" + abilityId.name().toLowerCase(java.util.Locale.ROOT)));
        }
    }

    private CodexStats() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(CodexStats::onRegister);
    }

    public static ResourceLocation abilityUse(AbilityId abilityId) {
        return ABILITY_USE_STATS.get(abilityId);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, path);
    }

    private static void onRegister(RegisterEvent event) {
        event.register(Registries.CUSTOM_STAT, helper -> {
            helper.register(ABILITIES_USED, ABILITIES_USED);
            for (ResourceLocation statId : ABILITY_USE_STATS.values()) {
                helper.register(statId, statId);
            }
        });
    }
}
