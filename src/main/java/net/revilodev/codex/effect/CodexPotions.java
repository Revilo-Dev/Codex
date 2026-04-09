package net.revilodev.codex.effect;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.revilodev.codex.CodexMod;

public final class CodexPotions {
    public static final DeferredRegister<Potion> REGISTER = DeferredRegister.create(Registries.POTION, CodexMod.MOD_ID);
    public static final Holder<Potion> ABILITY_POWER = REGISTER.register("ability_power",
            () -> new Potion("ability_power", new MobEffectInstance(CodexMobEffects.ABILITY_POWER_BOOST, 200, 0)));
    public static final Holder<Potion> STRONG_ABILITY_POWER = REGISTER.register("strong_ability_power",
            () -> new Potion("ability_power", new MobEffectInstance(CodexMobEffects.ABILITY_POWER_BOOST, 160, 1)));
    public static final Holder<Potion> SUPREME_ABILITY_POWER = REGISTER.register("supreme_ability_power",
            () -> new Potion("ability_power", new MobEffectInstance(CodexMobEffects.ABILITY_POWER_BOOST, 100, 2)));

    private CodexPotions() {}

    public static void register(IEventBus modBus) {
        REGISTER.register(modBus);
        NeoForge.EVENT_BUS.addListener(CodexPotions::onRegisterBrewingRecipes);
    }

    private static void onRegisterBrewingRecipes(RegisterBrewingRecipesEvent event) {
        event.getBuilder().addMix(Potions.AWKWARD, Items.GOLDEN_APPLE, ABILITY_POWER);
        event.getBuilder().addMix(ABILITY_POWER, Items.GLOWSTONE_DUST, STRONG_ABILITY_POWER);
        event.getBuilder().addMix(STRONG_ABILITY_POWER, Items.GLOWSTONE_DUST, SUPREME_ABILITY_POWER);
    }
}
