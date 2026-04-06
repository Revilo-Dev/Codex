package net.revilodev.codex.skills;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.EnumMap;
import java.util.List;

public final class PlayerSkills implements INBTSerializable<CompoundTag> {
    private static final int START_POINTS = 0;

    private int points = START_POINTS;
    private final EnumMap<SkillId, Integer> levels = new EnumMap<>(SkillId.class);
    private boolean modifiersDirty = true;

    public PlayerSkills() {
        initDefaults();
    }

    public int points() {
        return points;
    }

    public int level(SkillId id) {
        return levels.getOrDefault(id, 0);
    }

    public boolean tryUpgrade(SkillId id) {
        SkillDefinition def = SkillRegistry.def(id);
        if (def == null) return false;
        if (!canUnlock(id)) return false;

        int cur = level(id);
        if (cur >= def.maxLevel()) return false;

        int p = points;
        if (p <= 0) return false;

        points = p - 1;
        levels.put(id, cur + 1);
        modifiersDirty = true;
        return true;
    }

    public boolean tryDowngrade(SkillId id) {
        if (!canDowngrade(id)) return false;
        int cur = level(id);

        levels.put(id, cur - 1);
        points = Math.max(0, points + 1);
        modifiersDirty = true;
        return true;
    }

    public void adminAddPoints(int amt) {
        if (amt <= 0) return;
        points = Math.max(0, points + amt);
    }

    public void adminSetPoints(int amt) {
        points = Math.max(0, amt);
    }

    public void adminResetPoints() {
        points = START_POINTS;
    }

    public int adminAddLevel(SkillId id, int amt) {
        if (amt <= 0) return level(id);

        SkillDefinition def = SkillRegistry.def(id);
        int max = def != null ? def.maxLevel() : Integer.MAX_VALUE;

        int cur = level(id);
        int next = cur + amt;
        if (next < 0) next = 0;
        if (next > max) next = max;

        if (next != cur) {
            levels.put(id, next);
            modifiersDirty = true;
        }
        return next;
    }

    public void adminResetAll() {
        initDefaults();
    }

    public boolean canUnlock(SkillId id) {
        if (id == null) return false;
        if (id.primary()) return true;
        SkillId parent = id.parent();
        return parent != null && level(parent) > 0;
    }

    public boolean canDowngrade(SkillId id) {
        if (id == null) return false;

        int cur = level(id);
        if (cur <= 0) return false;

        return !id.primary() || !hasInvestedChildren(id) || cur > 1;
    }

    public boolean consumeModifiersDirty() {
        boolean dirty = modifiersDirty;
        modifiersDirty = false;
        return dirty;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();
        root.putInt("gp", points);

        CompoundTag l = new CompoundTag();
        for (SkillId id : SkillId.values()) l.putInt(id.name(), level(id));

        root.put("l", l);
        return root;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        initDefaults();
        if (nbt == null) return;

        if (nbt.contains("gp")) {
            points = Math.max(0, nbt.getInt("gp"));
        } else if (nbt.contains("p")) {
            CompoundTag p = nbt.getCompound("p");
            int total = 0;
            for (SkillCategory c : SkillCategory.values()) {
                if (p.contains(c.name())) total += Math.max(0, p.getInt(c.name()));
            }
            points = Math.max(0, total);
        }

        if (nbt.contains("l")) {
            CompoundTag l = nbt.getCompound("l");
            for (SkillId id : SkillId.values()) {
                if (l.contains(id.name())) levels.put(id, Math.max(0, Math.min(id.maxLevel(), l.getInt(id.name()))));
            }
        }

        modifiersDirty = true;
    }

    private void initDefaults() {
        points = START_POINTS;
        for (SkillId id : SkillId.values()) levels.put(id, 0);
        modifiersDirty = true;
    }

    private boolean hasInvestedChildren(SkillId parent) {
        List<SkillDefinition> secondaries = SkillRegistry.secondarySkillsFor(parent);
        for (SkillDefinition def : secondaries) {
            if (level(def.id()) > 0) return true;
        }
        return false;
    }
}
