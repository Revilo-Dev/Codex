package net.revilodev.codex.abilities.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.revilodev.codex.abilities.AbilityId;
import org.jetbrains.annotations.Nullable;

public class AbilityPowerCalculationEvent extends Event {
    private final ServerPlayer player;
    private final AbilityId abilityId;
    private double abilityPower;

    public AbilityPowerCalculationEvent(ServerPlayer player, @Nullable AbilityId abilityId, double abilityPower) {
        this.player = player;
        this.abilityId = abilityId;
        this.abilityPower = abilityPower;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    @Nullable
    public AbilityId getAbilityId() {
        return abilityId;
    }

    public double getAbilityPower() {
        return abilityPower;
    }

    public void setAbilityPower(double abilityPower) {
        this.abilityPower = abilityPower;
    }
}
