package net.revilodev.codex.skills;

import net.minecraft.resources.ResourceLocation;
import net.revilodev.codex.CodexMod;

public enum SkillId {
    STRENGTH(SkillCategory.STRENGTH, true, null, "Strength", "+1 damage per level", "strength", 10),
    POWER(SkillCategory.STRENGTH, false, STRENGTH, "Power", "+4 power per level", "strength-power", 5),
    CRIT_POWER(SkillCategory.STRENGTH, false, STRENGTH, "Crit Power", "+2 crit power per level", "strength-crit", 5),
    HASTE(SkillCategory.STRENGTH, false, STRENGTH, "Haste", "+1 block break speed per level", "strength-haste", 2),

    RESISTANCE(SkillCategory.RESISTANCE, true, null, "Resistance", "+1 resistance per level", "resistance", 10),
    FIRE_RESISTANCE(SkillCategory.RESISTANCE, false, RESISTANCE, "Fire Resistance", "+1 resistance per level", "resistance-fire", 5),
    PROJECTILE_RESISTANCE(SkillCategory.RESISTANCE, false, RESISTANCE, "Projectile Resistance", "+1 projectile res per level", "resistance-projectile", 5),
    KNOCKBACK_RESISTANCE(SkillCategory.RESISTANCE, false, RESISTANCE, "Knockback Resistance", "+1 knockback resistance per level", "resistance-knockback", 5),

    AGILITY(SkillCategory.AGILITY, true, null, "Agility", "+1 speed per level (5%)", "agility", 10),
    LEAPING(SkillCategory.AGILITY, false, AGILITY, "Leaping", "+5% jump height per level", "agility-jump", 5),

    VITALITY(SkillCategory.VITALITY, true, null, "Vitality", "+1 heart per level", "vitaility", 10),
    REGENERATION(SkillCategory.VITALITY, false, VITALITY, "Regeneration", "+1 regen (5%)", "vitaility-regen", 5),
    HEALTH_BOOST(SkillCategory.VITALITY, false, VITALITY, "Life Leach", "+1 life steal per level (5%)", "vitaility-health_boost", 5),
    CLEANSE(SkillCategory.VITALITY, false, VITALITY, "Cleanse", "+1 negative effect immunity", "vitaility-cleanse", 5),

    LUCK(SkillCategory.LUCK, true, null, "Luck", "+1 luck per level", "luck", 10),
    LOOTING(SkillCategory.LUCK, false, LUCK, "Looting", "+1 looting per level (20%)", "luck-looting", 2),
    FORTUNE(SkillCategory.LUCK, false, LUCK, "Fortune", "+1 fortune per level", "luck-fortune", 2);

    private final SkillCategory category;
    private final boolean primary;
    private final SkillId parent;
    private final String title;
    private final String description;
    private final ResourceLocation icon;
    private final int maxLevel;

    SkillId(SkillCategory category, boolean primary, SkillId parent, String title, String description, String iconPath, int maxLevel) {
        this.category = category;
        this.primary = primary;
        this.parent = parent;
        this.title = title;
        this.description = description;
        this.icon = ResourceLocation.fromNamespaceAndPath(CodexMod.MOD_ID, "textures/gui/skills/" + iconPath + ".png");
        this.maxLevel = maxLevel;
    }

    public SkillCategory category() { return category; }
    public boolean primary() { return primary; }
    public boolean secondary() { return !primary; }
    public SkillId parent() { return parent; }
    public String title() { return title; }
    public ResourceLocation icon() { return icon; }
    public String description() { return description; }
    public int maxLevel() { return maxLevel; }

    public static SkillId byOrdinal(int ord) {
        SkillId[] v = values();
        if (ord < 0 || ord >= v.length) return null;
        return v[ord];
    }
}
