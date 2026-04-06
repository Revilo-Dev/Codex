package net.revilodev.codex.client.abilities;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.revilodev.codex.abilities.AbilitiesAttachments;
import net.revilodev.codex.abilities.AbilitiesNetwork;
import net.revilodev.codex.abilities.AbilityId;
import net.revilodev.codex.abilities.AbilityRegistry;
import net.revilodev.codex.abilities.PlayerAbilities;
import net.revilodev.codex.abilities.logic.AbilityScaling;

import java.util.List;
import java.util.EnumMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class AbilityKeybinds {
    private static final String CATEGORY = "key.categories.codex";
    private static final Map<AbilityId, KeyMapping> KEYS = new EnumMap<>(AbilityId.class);
    private static final List<AbilityId> ALT_GRID = AbilityRegistry.all().stream().map(def -> def.id()).toList();
    private static boolean altWasDown = false;
    private static int altSelectionIndex = 0;
    private static boolean altSelectionChanged = false;

    private AbilityKeybinds() {}

    public static void register(IEventBus modBus) {
        if (KEYS.isEmpty()) createMappings();
        modBus.addListener(AbilityKeybinds::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(AbilityKeybinds::onClientTick);
        NeoForge.EVENT_BUS.addListener(AbilityKeybinds::onMouseScroll);
    }

    public static String keyName(AbilityId id) {
        KeyMapping key = KEYS.get(id);
        return key != null ? key.getTranslatedKeyMessage().getString() : "Unbound";
    }

    public static AbilityId altSelection() {
        if (ALT_GRID.isEmpty()) return null;
        return ALT_GRID.get(Mth.clamp(altSelectionIndex, 0, ALT_GRID.size() - 1));
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyMapping key : KEYS.values()) event.register(key);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        PlayerAbilities data = mc.player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
        data.tickCooldowns();

        boolean altDown = Screen.hasAltDown();
        if (altDown && !altWasDown) {
            altSelectionChanged = false;
        } else if (!altDown && altWasDown) {
            if (altSelectionChanged) {
                useAbility(altSelection());
            }
            altSelectionChanged = false;
        }
        altWasDown = altDown;

        for (var entry : KEYS.entrySet()) {
            while (entry.getValue().consumeClick()) {
                useAbility(entry.getKey());
            }
        }
    }

    private static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!Screen.hasAltDown() || ALT_GRID.isEmpty()) return;
        double delta = event.getScrollDeltaY();
        if (delta == 0.0D) return;
        altSelectionIndex = Math.floorMod(altSelectionIndex - (delta > 0.0D ? 1 : -1), ALT_GRID.size());
        altSelectionChanged = true;
        event.setCanceled(true);
    }

    private static void createMappings() {
        add(AbilityId.DASH, "dash", InputConstants.KEY_Z);
        add(AbilityId.LEAP, "leap", InputConstants.KEY_X);
        add(AbilityId.LUNGE, "lunge", InputConstants.KEY_G);
        add(AbilityId.HEAL, "heal", InputConstants.KEY_C);
        add(AbilityId.CLEANSE, "cleanse", InputConstants.KEY_V);
        add(AbilityId.WARCRY, "rampage", InputConstants.KEY_N);
        add(AbilityId.EXECUTION, "execute", InputConstants.KEY_M);
        add(AbilityId.CLEAVE, "cleave", InputConstants.KEY_J);
        add(AbilityId.OVERPOWER, "bash", InputConstants.KEY_K);
        add(AbilityId.GUARD, "guard", InputConstants.KEY_B);
        add(AbilityId.BLAST, "blast", InputConstants.KEY_H);
        add(AbilityId.BLAZE, "blaze", InputConstants.KEY_U);
        add(AbilityId.GLACIER, "glacier", InputConstants.KEY_I);
        add(AbilityId.SMITE, "smite", InputConstants.KEY_O);
        add(AbilityId.SCAVENGER, "magnetism", InputConstants.KEY_L);
    }

    private static void add(AbilityId id, String keyName, int defaultKey) {
        KEYS.put(id, new KeyMapping("key.codex.ability." + keyName, defaultKey, CATEGORY));
    }

    private static void useAbility(AbilityId id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || id == null) return;
        PlayerAbilities data = mc.player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
        if (!data.unlocked(id)) return;
        AbilityUseFail fail = localFailure(mc, data, id);
        if (fail != null) {
            AbilityHudOverlay.notifyFailedUse(id, fail);
            return;
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new AbilitiesNetwork.AbilityUsePayload(id.ordinal()));
    }

    private static AbilityUseFail localFailure(Minecraft mc, PlayerAbilities data, AbilityId id) {
        if (data.cooldownTicks(id) > 0) {
            return AbilityUseFail.COOLDOWN;
        }
        if (mc.player == null) return AbilityUseFail.NO_TARGET;
        return switch (id) {
            case LUNGE -> hasTarget(mc, AbilityScaling.lungeDistance(Math.max(1, data.rank(id)), null)) ? null : AbilityUseFail.NO_TARGET;
            case EXECUTION -> hasTarget(mc, 4.5D) ? null : AbilityUseFail.NO_TARGET;
            case OVERPOWER -> hasTarget(mc, 4.0D) ? null : AbilityUseFail.NO_TARGET;
            case GLACIER -> hasTarget(mc, 12.0D) ? null : AbilityUseFail.NO_TARGET;
            case SMITE -> hasNearby(mc, AbilityScaling.smiteRadius(Math.max(1, data.rank(id)), null)) ? null : AbilityUseFail.NO_TARGET;
            case CLEAVE -> hasNearbyInFront(mc, AbilityScaling.cleaveRadius(Math.max(1, data.rank(id)))) ? null : AbilityUseFail.NO_TARGET;
            case BLAST -> hasNearby(mc, AbilityScaling.blastRadius(Math.max(1, data.rank(id)), null)) ? null : AbilityUseFail.NO_TARGET;
            case BLAZE -> hasNearby(mc, AbilityScaling.blazeRadius(Math.max(1, data.rank(id)), null)) ? null : AbilityUseFail.NO_TARGET;
            default -> null;
        };
    }

    private static boolean hasTarget(Minecraft mc, double range) {
        if (mc.player == null) return false;
        Vec3 eye = mc.player.getEyePosition();
        Vec3 end = eye.add(mc.player.getLookAngle().scale(range));
        AABB box = mc.player.getBoundingBox().expandTowards(mc.player.getLookAngle().scale(range)).inflate(1.2D);
        return !mc.player.level().getEntitiesOfClass(LivingEntity.class, box, entity -> entity != mc.player && entity.isAlive())
                .stream()
                .filter(entity -> entity.position().distanceToSqr(end) < range * range + 4.0D)
                .toList()
                .isEmpty();
    }

    private static boolean hasNearby(Minecraft mc, double radius) {
        if (mc.player == null) return false;
        return !mc.player.level().getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(radius), entity -> entity != mc.player && entity.isAlive()).isEmpty();
    }

    private static boolean hasNearbyInFront(Minecraft mc, double radius) {
        if (mc.player == null) return false;
        Vec3 facing = mc.player.getLookAngle().normalize();
        return !mc.player.level().getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(radius), entity -> {
            if (entity == mc.player || !entity.isAlive()) return false;
            Vec3 toTarget = entity.position().subtract(mc.player.position()).normalize();
            return facing.dot(toTarget) > 0.15D;
        }).isEmpty();
    }

    public enum AbilityUseFail {
        COOLDOWN,
        NO_TARGET
    }
}
