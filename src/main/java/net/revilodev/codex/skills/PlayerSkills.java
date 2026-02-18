package net.revilodev.codex.skills;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.EnumMap;

public final class PlayerSkills implements INBTSerializable<CompoundTag> {
    private static final int START_POINTS = 10;
    private static final double XP_MULT_PER_POINT = 0.02D;

    private final EnumMap<SkillCategory, Integer> points = new EnumMap<>(SkillCategory.class);
    private final EnumMap<SkillCategory, Integer> progress = new EnumMap<>(SkillCategory.class);
    private final EnumMap<SkillCategory, Integer> earned = new EnumMap<>(SkillCategory.class);
    private final EnumMap<SkillId, Integer> levels = new EnumMap<>(SkillId.class);

    public PlayerSkills() {
        initDefaults();
    }

    public int points(SkillCategory c) {
        return points.getOrDefault(c, 0);
    }

    public int progress(SkillCategory c) {
        return progress.getOrDefault(c, 0);
    }

    public int earnedPoints(SkillCategory c) {
        return earned.getOrDefault(c, 0);
    }

    public int requiredForNextPoint(SkillCategory c) {
        return requiredXpFor(c, earnedPoints(c));
    }

    public float progressPct(SkillCategory c) {
        int req = requiredForNextPoint(c);
        if (req <= 0) return 0.0F;
        return Math.min(1.0F, Math.max(0.0F, progress(c) / (float) req));
    }

    public int level(SkillId id) {
        return levels.getOrDefault(id, 0);
    }

    public int spentIn(SkillCategory c) {
        int sum = 0;
        for (SkillId id : SkillId.values()) {
            if (id.category() == c) sum += level(id);
        }
        return sum;
    }

    public boolean addProgress(SkillCategory c, int xp) {
        if (xp <= 0) return false;

        int curXp = progress(c);
        int add = xp;
        if (add > Integer.MAX_VALUE - curXp) add = Integer.MAX_VALUE - curXp;

        int newXp = curXp + add;
        int earnedPts = earnedPoints(c);

        while (true) {
            int req = requiredXpFor(c, earnedPts);
            if (req <= 0) req = 1;
            if (newXp < req) break;

            newXp -= req;
            points.put(c, points(c) + 1);
            earnedPts++;
        }

        progress.put(c, newXp);
        earned.put(c, earnedPts);
        return true;
    }

    public boolean tryUpgrade(SkillId id) {
        SkillDefinition def = SkillRegistry.def(id);
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

    public void adminAddPoints(SkillCategory c, int amt) {
        if (amt <= 0) return;
        points.put(c, Math.max(0, points(c) + amt));
    }

    public int adminAddLevel(SkillId id, int amt) {
        if (amt <= 0) return level(id);

        SkillDefinition def = SkillRegistry.def(id);
        int max = def != null ? def.maxLevel() : Integer.MAX_VALUE;

        int cur = level(id);
        int next = cur + amt;
        if (next < 0) next = 0;
        if (next > max) next = max;

        levels.put(id, next);
        return next;
    }

    public void adminResetCategoryPoints(SkillCategory c) {
        points.put(c, START_POINTS);
        progress.put(c, 0);
        earned.put(c, START_POINTS);
    }

    public void adminResetAll() {
        initDefaults();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();

        CompoundTag p = new CompoundTag();
        for (SkillCategory c : SkillCategory.values()) p.putInt(c.name(), points(c));

        CompoundTag x = new CompoundTag();
        for (SkillCategory c : SkillCategory.values()) x.putInt(c.name(), progress(c));

        CompoundTag e = new CompoundTag();
        for (SkillCategory c : SkillCategory.values()) e.putInt(c.name(), earnedPoints(c));

        CompoundTag l = new CompoundTag();
        for (SkillId id : SkillId.values()) l.putInt(id.name(), level(id));

        root.put("p", p);
        root.put("x", x);
        root.put("e", e);
        root.put("l", l);
        return root;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        initDefaults();
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

        if (nbt.contains("x")) {
            CompoundTag x = nbt.getCompound("x");
            for (SkillCategory c : SkillCategory.values()) {
                if (x.contains(c.name())) progress.put(c, Math.max(0, x.getInt(c.name())));
            }
        }

        if (nbt.contains("e")) {
            CompoundTag e = nbt.getCompound("e");
            for (SkillCategory c : SkillCategory.values()) {
                if (e.contains(c.name())) earned.put(c, Math.max(0, e.getInt(c.name())));
            }
        } else {
            for (SkillCategory c : SkillCategory.values()) {
                int v = Math.max(0, points(c) + spentIn(c));
                earned.put(c, v);
            }
        }
    }

    private static int requiredXpFor(SkillCategory c, int earnedPoints) {
        long e = Math.max(0L, (long) earnedPoints);

        long base;
        long lin;
        long quad;

        switch (c) {
            case COMBAT -> { base = 18L; lin = 7L; quad = 2L; }
            case UTILITY -> { base = 22L; lin = 8L; quad = 3L; }
            case SURVIVAL -> { base = 20L; lin = 7L; quad = 2L; }
            default -> { base = 20L; lin = 7L; quad = 2L; }
        }

        long req = base + (lin * e) + (quad * e * e);

        if (e > 25L) {
            long d = e - 25L;
            req += 6L * d * d;
        }
        if (e > 60L) {
            long d = e - 60L;
            req += 15L * d * d;
        }

        if (req < 1L) req = 1L;

        double mult = 1.0D + (XP_MULT_PER_POINT * e);
        double scaled = req * mult;
        if (scaled > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        int out = (int) Math.round(scaled);
        if (out < 1) out = 1;
        return out;
    }

    private void initDefaults() {
        for (SkillCategory c : SkillCategory.values()) {
            points.put(c, START_POINTS);
            progress.put(c, 0);
            earned.put(c, START_POINTS);
        }
        for (SkillId id : SkillId.values()) levels.put(id, 0);
    }
}
