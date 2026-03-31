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
import net.revilodev.codex.abilities.PlayerAbilities;

@OnlyIn(Dist.CLIENT)
public final class AbilityKeybinds {
    private static final String CATEGORY = "key.categories.codex";
    private static final KeyMapping[] SLOTS = new KeyMapping[] {
            new KeyMapping("key.codex.ability_slot_1", InputConstants.KEY_Z, CATEGORY),
            new KeyMapping("key.codex.ability_slot_2", InputConstants.KEY_X, CATEGORY),
            new KeyMapping("key.codex.ability_slot_3", InputConstants.KEY_C, CATEGORY),
            new KeyMapping("key.codex.ability_slot_4", InputConstants.KEY_V, CATEGORY),
            new KeyMapping("key.codex.ability_slot_5", InputConstants.KEY_B, CATEGORY)
    };

    private AbilityKeybinds() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(AbilityKeybinds::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(AbilityKeybinds::onClientTick);
    }

    public static String slotKeyName(int slot) {
        return key(slot).getTranslatedKeyMessage().getString();
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyMapping key : SLOTS) event.register(key);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        PlayerAbilities data = mc.player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
        data.tickCooldowns();

        for (int i = 0; i < SLOTS.length; i++) {
            int slot = i + 1;
            while (SLOTS[i].consumeClick()) {
                useSlot(slot);
            }
        }
    }

    private static KeyMapping key(int slot) {
        if (slot < 1 || slot > SLOTS.length) return SLOTS[0];
        return SLOTS[slot - 1];
    }

    private static void useSlot(int slot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        PlayerAbilities data = mc.player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
        if (data.slot(slot) == null) return;
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new AbilitiesNetwork.AbilityUsePayload(slot));
    }
}
