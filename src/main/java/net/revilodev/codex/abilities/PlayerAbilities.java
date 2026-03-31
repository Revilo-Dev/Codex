package net.revilodev.codex.abilities;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.EnumMap;

public final class PlayerAbilities implements INBTSerializable<CompoundTag> {
    private static final int SLOT_COUNT = 5;

    private int points;
    private final EnumMap<AbilityId, Integer> ranks = new EnumMap<>(AbilityId.class);
    private final EnumMap<AbilityId, Integer> cooldowns = new EnumMap<>(AbilityId.class);
    private final EnumMap<AbilityId, Integer> activeTicks = new EnumMap<>(AbilityId.class);
    private final AbilityId[] slots = new AbilityId[SLOT_COUNT];

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

    public AbilityId slot(int slot) {
        return validSlot(slot) ? slots[slot - 1] : null;
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
            clearAssignment(id);
            cooldowns.remove(id);
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
            clearAssignment(id);
            cooldowns.remove(id);
        }
    }

    public boolean assign(int slot, AbilityId id) {
        if (!validSlot(slot) || slot > AbilityConfig.maxSlots()) return false;
        if (id != null && !unlocked(id)) return false;
        if (id != null) clearAssignment(id);
        slots[slot - 1] = id;
        return true;
    }

    public boolean clearAssignmentFor(AbilityId id) {
        if (id == null) return false;
        boolean changed = assignedSlot(id) != 0;
        clearAssignment(id);
        return changed;
    }

    public int assignedSlot(AbilityId id) {
        if (id == null) return 0;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == id) return i + 1;
        }
        return 0;
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

    public void adminReset() {
        reset();
    }

    private void clearAssignment(AbilityId id) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == id) slots[i] = null;
        }
    }

    private void reset() {
        points = 0;
        ranks.clear();
        cooldowns.clear();
        activeTicks.clear();
        for (int i = 0; i < slots.length; i++) slots[i] = null;
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
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null) tag.putString("slot" + (i + 1), slots[i].name());
        }
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

        for (int i = 0; i < slots.length; i++) {
            AbilityId id = parseAbility(nbt.getString("slot" + (i + 1)));
            slots[i] = id != null && unlocked(id) ? id : null;
        }
    }

    private static boolean validSlot(int slot) {
        return slot >= 1 && slot <= SLOT_COUNT;
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
