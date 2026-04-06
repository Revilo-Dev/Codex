package net.revilodev.codex.abilities;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public final class PlayerAbilities implements INBTSerializable<CompoundTag> {
    private static final int RECENT_COUNT = 4;

    private int points;
    private final EnumMap<AbilityId, Integer> ranks = new EnumMap<>(AbilityId.class);
    private final EnumMap<AbilityId, Integer> cooldowns = new EnumMap<>(AbilityId.class);
    private final EnumMap<AbilityId, Integer> activeTicks = new EnumMap<>(AbilityId.class);
    private final List<AbilityId> recent = new ArrayList<>();

    public PlayerAbilities() {
        reset();
    }

    public int points() {
        return points;
    }

    public int rank(AbilityId id) {
        return ranks.getOrDefault(id, 0);
    }

    public boolean unlocked(AbilityId id) {
        return rank(id) > 0;
    }

    public int cooldownTicks(AbilityId id) {
        return Math.max(0, cooldowns.getOrDefault(id, 0));
    }

    public int activeTicks(AbilityId id) {
        return Math.max(0, activeTicks.getOrDefault(id, 0));
    }

    public boolean tryUpgrade(AbilityId id) {
        if (id == null || !AbilityConfig.enabled(id)) return false;
        int cur = rank(id);
        if (cur >= id.maxRank() || points <= 0) return false;
        points--;
        ranks.put(id, cur + 1);
        return true;
    }

    public boolean tryDowngrade(AbilityId id) {
        if (id == null) return false;
        int cur = rank(id);
        if (cur <= 0) return false;
        int next = cur - 1;
        ranks.put(id, next);
        points++;
        if (next <= 0) {
            cooldowns.remove(id);
            activeTicks.remove(id);
            recent.remove(id);
        }
        return true;
    }

    public void addPoints(int amount) {
        if (amount > 0) points += amount;
    }

    public void setPoints(int amount) {
        points = Math.max(0, amount);
    }

    public void setRank(AbilityId id, int rank) {
        if (id == null) return;
        int next = Math.max(0, Math.min(id.maxRank(), rank));
        ranks.put(id, next);
        if (next <= 0) {
            cooldowns.remove(id);
            activeTicks.remove(id);
            recent.remove(id);
        }
    }

    public void setCooldown(AbilityId id, int ticks) {
        if (id == null) return;
        int next = Math.max(0, ticks);
        if (next <= 0) cooldowns.remove(id);
        else cooldowns.put(id, next);
    }

    public void setActiveTicks(AbilityId id, int ticks) {
        if (id == null) return;
        int next = Math.max(0, ticks);
        if (next <= 0) activeTicks.remove(id);
        else activeTicks.put(id, next);
    }

    public boolean tickCooldowns() {
        boolean changed = false;
        for (AbilityId id : AbilityId.values()) {
            int cur = cooldownTicks(id);
            if (cur <= 0) continue;
            int next = cur - 1;
            if (next <= 0) cooldowns.remove(id);
            else cooldowns.put(id, next);
            changed = true;
        }
        return changed;
    }

    public void markUsed(AbilityId id) {
        if (id == null || !unlocked(id)) return;
        recent.remove(id);
        recent.add(0, id);
        while (recent.size() > RECENT_COUNT) {
            recent.remove(recent.size() - 1);
        }
    }

    public List<AbilityId> recentAbilities() {
        return List.copyOf(recent);
    }

    public void adminReset() {
        reset();
    }

    private void reset() {
        points = 0;
        ranks.clear();
        cooldowns.clear();
        activeTicks.clear();
        recent.clear();
        for (AbilityId id : AbilityId.values()) {
            ranks.put(id, 0);
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("points", points);

        CompoundTag ranksTag = new CompoundTag();
        CompoundTag cooldownTag = new CompoundTag();
        CompoundTag activeTag = new CompoundTag();
        for (AbilityId id : AbilityId.values()) {
            ranksTag.putInt(id.name(), rank(id));
            int cooldown = cooldownTicks(id);
            if (cooldown > 0) cooldownTag.putInt(id.name(), cooldown);
            int active = activeTicks(id);
            if (active > 0) activeTag.putInt(id.name(), active);
        }

        tag.put("ranks", ranksTag);
        tag.put("cooldowns", cooldownTag);
        tag.put("active", activeTag);
        CompoundTag recentTag = new CompoundTag();
        for (int i = 0; i < recent.size(); i++) {
            recentTag.putString("recent" + i, recent.get(i).name());
        }
        tag.put("recent", recentTag);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        reset();
        if (nbt == null) return;
        points = Math.max(0, nbt.getInt("points"));

        CompoundTag ranksTag = nbt.getCompound("ranks");
        CompoundTag cooldownTag = nbt.getCompound("cooldowns");
        CompoundTag activeTag = nbt.getCompound("active");
        CompoundTag recentTag = nbt.getCompound("recent");
        for (AbilityId id : AbilityId.values()) {
            if (ranksTag.contains(id.name())) {
                ranks.put(id, Math.max(0, Math.min(id.maxRank(), ranksTag.getInt(id.name()))));
            }
            if (cooldownTag.contains(id.name())) {
                cooldowns.put(id, Math.max(0, cooldownTag.getInt(id.name())));
            }
            if (activeTag.contains(id.name())) {
                activeTicks.put(id, Math.max(0, activeTag.getInt(id.name())));
            }
        }
        for (int i = 0; i < RECENT_COUNT; i++) {
            AbilityId id = parseAbility(recentTag.getString("recent" + i));
            if (id != null && unlocked(id) && !recent.contains(id)) {
                recent.add(id);
            }
        }
    }

    private static AbilityId parseAbility(String name) {
        if (name == null || name.isEmpty()) return null;
        try {
            return AbilityId.valueOf(name);
        } catch (Exception ignored) {
            return null;
        }
    }
}
