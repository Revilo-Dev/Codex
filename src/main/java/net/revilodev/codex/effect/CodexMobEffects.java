package net.revilodev.codex.effect;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.revilodev.codex.CodexMod;
import net.revilodev.codex.attributes.CodexAttributes;

public final class CodexMobEffects {
    public static final DeferredRegister<MobEffect> REGISTER = DeferredRegister.create(Registries.MOB_EFFECT, CodexMod.MOD_ID);
    public static final Holder<MobEffect> ABILITY_POWER_BOOST = REGISTER.register("ability_power_boost", () ->
            new AbilityPowerBoostEffect()
                    .addAttributeModifier(CodexAttributes.ABILITY_POWER,
                            ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "ability_power_boost"),
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                            amplifier -> switch (amplifier) {
                                case 0 -> 0.25D;
                                case 1 -> 0.50D;
                                default -> 1.0D;
                            }));

    private CodexMobEffects() {}

    public static void register(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
