package net.revilodev.codex.abilities.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.revilodev.codex.abilities.AbilityId;
import net.revilodev.codex.skills.PlayerSkills;

public abstract class AbilityUseEvent extends Event {
    private final ServerPlayer player;
    private final AbilityId abilityId;
    private final int rank;
    private final PlayerSkills skills;
    private final double abilityPower;

    protected AbilityUseEvent(ServerPlayer player, AbilityId abilityId, int rank, PlayerSkills skills, double abilityPower) {
        this.player = player;
        this.abilityId = abilityId;
        this.rank = rank;
        this.skills = skills;
        this.abilityPower = abilityPower;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public AbilityId getAbilityId() {
        return abilityId;
    }

    public int getRank() {
        return rank;
    }

    public PlayerSkills getSkills() {
        return skills;
    }

    public double getAbilityPower() {
        return abilityPower;
    }

    public static final class Pre extends AbilityUseEvent implements ICancellableEvent {
        public Pre(ServerPlayer player, AbilityId abilityId, int rank, PlayerSkills skills, double abilityPower) {
            super(player, abilityId, rank, skills, abilityPower);
        }
    }

    public static final class Post extends AbilityUseEvent {
        public Post(ServerPlayer player, AbilityId abilityId, int rank, PlayerSkills skills, double abilityPower) {
            super(player, abilityId, rank, skills, abilityPower);
        }
    }
}
