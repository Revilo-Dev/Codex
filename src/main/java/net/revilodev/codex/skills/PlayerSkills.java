package net.revilodev.codex.skills;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.EnumMap;

public final class PlayerSkills implements INBTSerializable<CompoundTag> {
    private final EnumMap<SkillCategory, Integer> points = new EnumMap<>(SkillCategory.class);
    private final EnumMap<SkillId, Integer> levels = new EnumMap<>(SkillId.class);

    public PlayerSkills() {
        for (SkillCategory c : SkillCategory.values()) points.put(c, 0);
        for (SkillId id : SkillId.values()) levels.put(id, 0);
    }

    public int points(SkillCategory c) {
        return points.getOrDefault(c, 0);
    }

    public int level(SkillId id) {
        return levels.getOrDefault(id, 0);
    }

    public void addPoints(SkillCategory c, int amt) {
        if (amt <= 0) return;
        points.put(c, Math.max(0, points(c) + amt));
    }

    public boolean tryUpgrade(SkillId id) {
        SkillDef def = SkillRegistry.def(id);
        if (def == null) return false;

        int cur = level(id);
        if (cur >= def.maxLevel()) return false;

        SkillCategory c = id.category();
        int p = points(c);
        if (p <= 0) return false;

        points.put(c, p - 1);
        levels.put(id, cur + 1);
        return true;
    }

    public boolean tryDowngrade(SkillId id) {
        int cur = level(id);
        if (cur <= 0) return false;

        SkillCategory c = id.category();
        levels.put(id, cur - 1);
        points.put(c, points(c) + 1);
        return true;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();
        CompoundTag p = new CompoundTag();
        for (SkillCategory c : SkillCategory.values()) p.putInt(c.name(), points(c));
        CompoundTag l = new CompoundTag();
        for (SkillId id : SkillId.values()) l.putInt(id.name(), level(id));
        root.put("p", p);
        root.put("l", l);
        return root;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        if (nbt == null) return;

        if (nbt.contains("p")) {
            CompoundTag p = nbt.getCompound("p");
            for (SkillCategory c : SkillCategory.values()) {
                if (p.contains(c.name())) points.put(c, Math.max(0, p.getInt(c.name())));
            }
        }

        if (nbt.contains("l")) {
            CompoundTag l = nbt.getCompound("l");
            for (SkillId id : SkillId.values()) {
                if (l.contains(id.name())) levels.put(id, Math.max(0, l.getInt(id.name())));
            }
        }
    }
}
