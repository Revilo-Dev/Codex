package net.revilodev.codex.skills;

import net.minecraft.resources.ResourceLocation;
import net.revilodev.codex.CodexMod;

public enum SkillId {
    STRENGTH(SkillCategory.STRENGTH, true, null, "Strength", "Primary strength skill.", "strength", 10),
    POWER(SkillCategory.STRENGTH, false, STRENGTH, "Power", "Increases melee damage.", "strength-power", 10),
    CRIT_POWER(SkillCategory.STRENGTH, false, STRENGTH, "Crit Power", "Increases critical damage multiplier.", "strength-crit", 10),
    HASTE(SkillCategory.STRENGTH, false, STRENGTH, "Haste", "Increases attack speed.", "strength-haste", 10),

    RESISTANCE(SkillCategory.RESISTANCE, true, null, "Resistance", "Primary resistance skill.", "resistance", 10),
    FIRE_RESISTANCE(SkillCategory.RESISTANCE, false, RESISTANCE, "Fire Resistance", "Reduces fire and lava damage.", "resistance-fire", 10),
    PROJECTILE_RESISTANCE(SkillCategory.RESISTANCE, false, RESISTANCE, "Projectile Resistance", "Reduces projectile damage.", "resistance-projectile", 10),
    KNOCKBACK_RESISTANCE(SkillCategory.RESISTANCE, false, RESISTANCE, "Knockback Resistance", "Increases knockback resistance.", "resistance-knockback", 10),

    AGILITY(SkillCategory.AGILITY, true, null, "Agility", "Primary agility skill.", "agility", 10),
    LEAPING(SkillCategory.AGILITY, false, AGILITY, "Leaping", "Increases jump height.", "agility-jump", 10),

    VITALITY(SkillCategory.VITALITY, true, null, "Vitality", "Primary vitality skill.", "vitaility", 10),
    REGENERATION(SkillCategory.VITALITY, false, VITALITY, "Regeneration", "Grants passive healing.", "vitaility-regen", 10),
    HEALTH_BOOST(SkillCategory.VITALITY, false, VITALITY, "Health Boost", "Increases max health.", "vitaility-health_boost", 10),
    CLEANSE(SkillCategory.VITALITY, false, VITALITY, "Cleanse", "Chance to remove a negative effect.", "vitaility-cleanse", 10),

    LUCK(SkillCategory.LUCK, true, null, "Luck", "Primary luck skill.", "luck", 10),
    LOOTING(SkillCategory.LUCK, false, LUCK, "Looting", "Improves mob drops.", "luck-looting", 10),
    FORTUNE(SkillCategory.LUCK, false, LUCK, "Fortune", "Improves block drops.", "luck-fortune", 10);

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
