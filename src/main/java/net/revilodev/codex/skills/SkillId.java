package net.revilodev.codex.skills;

import net.minecraft.resources.ResourceLocation;

public enum SkillId {
    SHARPNESS(SkillCategory.COMBAT, "Damage", ResourceLocation.parse("minecraft:iron_sword"), "Increases melee damage.", 25),
    POWER(SkillCategory.COMBAT, "Power", ResourceLocation.parse("minecraft:bow"), "Increases ranged damage.", 25),
    CRIT_BONUS(SkillCategory.COMBAT, "Crit Strength", ResourceLocation.parse("minecraft:netherite_sword"), "Increases critical damage bonus.", 30),
    FIRE_RESISTANCE(SkillCategory.COMBAT, "Fire Resistance", ResourceLocation.parse("minecraft:blaze_powder"), "Reduces fire damage taken.", 10),
    BLAST_RESISTANCE(SkillCategory.COMBAT, "Blast Resistance", ResourceLocation.parse("minecraft:tnt"), "Reduces explosion damage taken.", 10),
    PROJECTILE_RESISTANCE(SkillCategory.COMBAT, "Projectile Resistance", ResourceLocation.parse("minecraft:shield"), "Reduces projectile damage taken.", 10),
    KNOCKBACK_RESISTANCE(SkillCategory.COMBAT, "Knockback Resistance", ResourceLocation.parse("minecraft:netherite_chestplate"), "Reduces knockback taken.", 10),

    HEALTH(SkillCategory.SURVIVAL, "Health", ResourceLocation.parse("minecraft:iron_chestplate"), "Increases max health.", 20),
    REGENERATION(SkillCategory.SURVIVAL, "Regeneration", ResourceLocation.parse("minecraft:enchanted_golden_apple"), "Increases passive regeneration.", 10),
    SWIFTNESS(SkillCategory.SURVIVAL, "Swiftness", ResourceLocation.parse("minecraft:iron_boots"), "Increases movement speed.", 25),
    DEFENSE(SkillCategory.SURVIVAL, "Defense", ResourceLocation.parse("minecraft:shield"), "Increases armor.", 25),
    SATURATION(SkillCategory.SURVIVAL, "Saturation", ResourceLocation.parse("minecraft:cooked_beef"), "Improves hunger/saturation sustain.", 10),
    LEAPING(SkillCategory.SURVIVAL, "Leaping", ResourceLocation.parse("minecraft:rabbit_foot"), "Increases jump height.", 12),

    EFFICIENCY(SkillCategory.UTILITY, "Mining", ResourceLocation.parse("minecraft:iron_pickaxe"), "Increases mining speed.", 25),
    CHOPPING(SkillCategory.UTILITY, "Chopping", ResourceLocation.parse("minecraft:iron_axe"), "Increases chopping speed.", 25),
    FORAGING(SkillCategory.UTILITY, "Foraging", ResourceLocation.parse("minecraft:oak_sapling"), "Improves drop rates from sources such as leaves and crops.", 25),
    FISHING(SkillCategory.UTILITY, "Fishing", ResourceLocation.parse("minecraft:fishing_rod"), "Improves fishing outcomes.", 25),
    FORTUNE(SkillCategory.UTILITY, "Fortune", ResourceLocation.parse("minecraft:diamond"), "Improves loot/luck outcomes from chests.", 25),
    LOOTING(SkillCategory.UTILITY, "Looting", ResourceLocation.parse("minecraft:gold_ingot"), "Improves loot drops from mobs.", 25);

    private final SkillCategory category;
    private final String title;
    private final ResourceLocation icon;
    private final String description;
    private final int maxLevel;

    SkillId(SkillCategory category, String title, ResourceLocation icon, String description, int maxLevel) {
        this.category = category;
        this.title = title;
        this.icon = icon;
        this.description = description;
        this.maxLevel = maxLevel;
    }

    public SkillCategory category() { return category; }
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
