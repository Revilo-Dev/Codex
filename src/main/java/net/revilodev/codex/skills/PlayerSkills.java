package net.revilodev.codex.skills;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.EnumMap;

public final class PlayerSkills implements INBTSerializable<CompoundTag> {
    private static final int START_POINTS = 0;
    private static final int BASE_TASKS_PER_POINT = 10;
    private static final double TASK_MULTIPLIER = 1.5D;
    private static final int MAX_STEP_INCREASE = 15;
    private static final int STEP_ROUND_TO = 5;

    private int points = START_POINTS;
    private int progress = 0;
    private int earned = START_POINTS;
    private final EnumMap<SkillId, Integer> levels = new EnumMap<>(SkillId.class);
    private boolean modifiersDirty = true;

    public PlayerSkills() {
        initDefaults();
    }

    public int points() {
        return points;
    }

    public int points(SkillCategory c) {
        return points;
    }

    public int progress() {
        return progress;
    }

    public int progress(SkillCategory c) {
        return progress;
    }

    public int earnedPoints() {
        return earned;
    }

    public int earnedPoints(SkillCategory c) {
        return earned;
    }

    public int requiredForNextPoint() {
        return requiredXpFor(earnedPoints());
    }

    public int requiredForNextPoint(SkillCategory c) {
        return requiredForNextPoint();
    }

    public float progressPct() {
        int req = requiredForNextPoint();
        if (req <= 0) return 0.0F;
        return Math.min(1.0F, Math.max(0.0F, progress / (float) req));
    }

    public float progressPct(SkillCategory c) {
        return progressPct();
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
        return addProgress(xp);
    }

    public boolean addProgress(int xp) {
        if (xp <= 0) return false;

        int curXp = progress;
        int add = xp;
        if (add > Integer.MAX_VALUE - curXp) add = Integer.MAX_VALUE - curXp;

        int newXp = curXp + add;
        int earnedPts = earned;

        while (true) {
            int req = requiredXpFor(earnedPts);
            if (req <= 0) req = 1;
            if (newXp < req) break;

            newXp -= req;
            points = Math.max(0, points + 1);
            earnedPts++;
        }

        progress = Math.max(0, newXp);
        earned = Math.max(0, earnedPts);
        return true;
    }

    public boolean tryUpgrade(SkillId id) {
        SkillDefinition def = SkillRegistry.def(id);
        if (def == null) return false;

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
        int cur = level(id);
        if (cur <= 0) return false;

        levels.put(id, cur - 1);
        points = Math.max(0, points + 1);
        modifiersDirty = true;
        return true;
    }

    public void adminAddPoints(SkillCategory c, int amt) {
        adminAddPoints(amt);
    }

    public void adminAddPoints(int amt) {
        if (amt <= 0) return;
        points = Math.max(0, points + amt);
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

    public void adminResetCategoryPoints(SkillCategory c) {
        points = START_POINTS;
        progress = 0;
        earned = START_POINTS;
    }

    public void adminResetAll() {
        initDefaults();
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
        root.putInt("gx", progress);
        root.putInt("ge", earned);

        CompoundTag p = new CompoundTag();
        for (SkillCategory c : SkillCategory.values()) p.putInt(c.name(), points);

        CompoundTag x = new CompoundTag();
        for (SkillCategory c : SkillCategory.values()) x.putInt(c.name(), progress);

        CompoundTag e = new CompoundTag();
        for (SkillCategory c : SkillCategory.values()) e.putInt(c.name(), earned);

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
                if (l.contains(id.name())) levels.put(id, Math.max(0, l.getInt(id.name())));
            }
        }

        if (nbt.contains("gx")) {
            progress = Math.max(0, nbt.getInt("gx"));
        } else if (nbt.contains("x")) {
            CompoundTag x = nbt.getCompound("x");
            int total = 0;
            for (SkillCategory c : SkillCategory.values()) {
                if (x.contains(c.name())) total += Math.max(0, x.getInt(c.name()));
            }
            progress = Math.max(0, total);
        }

        if (nbt.contains("ge")) {
            earned = Math.max(0, nbt.getInt("ge"));
        } else if (nbt.contains("e")) {
            CompoundTag e = nbt.getCompound("e");
            int total = 0;
            for (SkillCategory c : SkillCategory.values()) {
                if (e.contains(c.name())) total += Math.max(0, e.getInt(c.name()));
            }
            earned = Math.max(0, total);
        } else {
            int spent = 0;
            for (SkillCategory c : SkillCategory.values()) spent += spentIn(c);
            earned = Math.max(0, points + spent);
        }

        modifiersDirty = true;
    }

    private static int requiredXpFor(int earnedPoints) {
        int e = Math.max(0, earnedPoints);
        int req = BASE_TASKS_PER_POINT;

        for (int i = 0; i < e; i++) {
            int projected = (int) Math.floor(req * TASK_MULTIPLIER);
            int increase = projected - req;
            if (increase > MAX_STEP_INCREASE) increase = MAX_STEP_INCREASE;
            req = roundDown(req + increase, STEP_ROUND_TO);
        }

        return Math.max(1, req);
    }

    private static int roundDown(int value, int step) {
        if (step <= 1) return value;
        return (value / step) * step;
    }

    private void initDefaults() {
        points = START_POINTS;
        progress = 0;
        earned = START_POINTS;
        for (SkillId id : SkillId.values()) levels.put(id, 0);
        modifiersDirty = true;
    }
}
