package net.revilodev.codex.client.abilities;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.revilodev.codex.abilities.AbilitiesAttachments;
import net.revilodev.codex.abilities.AbilitiesNetwork;
import net.revilodev.codex.abilities.AbilityId;
import net.revilodev.codex.abilities.PlayerAbilities;

import java.util.EnumMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class AbilityKeybinds {
    private static final String CATEGORY = "key.categories.codex";
    private static final Map<AbilityId, KeyMapping> KEYS = new EnumMap<>(AbilityId.class);

    private AbilityKeybinds() {}

    public static void register(IEventBus modBus) {
        if (KEYS.isEmpty()) createMappings();
        modBus.addListener(AbilityKeybinds::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(AbilityKeybinds::onClientTick);
    }

    public static String keyName(AbilityId id) {
        KeyMapping key = KEYS.get(id);
        return key != null ? key.getTranslatedKeyMessage().getString() : "Unbound";
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyMapping key : KEYS.values()) event.register(key);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        PlayerAbilities data = mc.player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
        data.tickCooldowns();

        for (var entry : KEYS.entrySet()) {
            while (entry.getValue().consumeClick()) {
                useAbility(entry.getKey());
            }
        }
    }

    private static void createMappings() {
        add(AbilityId.DASH, "dash", InputConstants.KEY_Z);
        add(AbilityId.LEAP, "leap", InputConstants.KEY_X);
        add(AbilityId.HEAL, "heal", InputConstants.KEY_C);
        add(AbilityId.CLEANSE, "cleanse", InputConstants.KEY_V);
        add(AbilityId.GUARD, "guard", InputConstants.KEY_B);
        add(AbilityId.WARCRY, "warcry", InputConstants.KEY_N);
        add(AbilityId.EXECUTION, "execution", InputConstants.KEY_M);
        add(AbilityId.CLEAVE, "cleave", InputConstants.KEY_J);
        add(AbilityId.OVERPOWER, "overpower", InputConstants.KEY_K);
        add(AbilityId.SCAVENGER, "magnetism", InputConstants.KEY_L);
    }

    private static void add(AbilityId id, String keyName, int defaultKey) {
        KEYS.put(id, new KeyMapping("key.codex.ability." + keyName, defaultKey, CATEGORY));
    }

    private static void useAbility(AbilityId id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        PlayerAbilities data = mc.player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
        if (!data.unlocked(id)) return;
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new AbilitiesNetwork.AbilityUsePayload(id.ordinal()));
    }
}
