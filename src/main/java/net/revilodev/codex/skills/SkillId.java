package net.revilodev.codex.skills;

import net.minecraft.resources.ResourceLocation;

public enum SkillId {
    SHARPNESS(SkillCategory.COMBAT, "Sharpness", ResourceLocation.parse("minecraft:iron_sword"), "Increases melee damage.", 50),
    POWER(SkillCategory.COMBAT, "Power", ResourceLocation.parse("minecraft:bow"), "Increases ranged damage.", 50),
    CRIT_BONUS(SkillCategory.COMBAT, "Crit Bonus", ResourceLocation.parse("minecraft:netherite_sword"), "Increases critical damage bonus.", 50),
    FIRE_RESISTANCE(SkillCategory.COMBAT, "Fire Resistance", ResourceLocation.parse("minecraft:blaze_powder"), "Reduces fire damage taken.", 50),
    BLAST_RESISTANCE(SkillCategory.COMBAT, "Blast Resistance", ResourceLocation.parse("minecraft:tnt"), "Reduces explosion damage taken.", 50),
    PROJECTILE_RESISTANCE(SkillCategory.COMBAT, "Projectile Resistance", ResourceLocation.parse("minecraft:shield"), "Reduces projectile damage taken.", 50),
    KNOCKBACK_RESISTANCE(SkillCategory.COMBAT, "Knockback Resistance", ResourceLocation.parse("minecraft:netherite_chestplate"), "Reduces knockback taken.", 50),

    HEALTH(SkillCategory.SURVIVAL, "Health", ResourceLocation.parse("minecraft:heart_of_the_sea"), "Increases max health.", 50),
    REGENERATION(SkillCategory.SURVIVAL, "Regeneration", ResourceLocation.parse("minecraft:ghast_tear"), "Grants passive regeneration.", 50),
    SWIFTNESS(SkillCategory.SURVIVAL, "Swiftness", ResourceLocation.parse("minecraft:sugar"), "Increases movement speed.", 50),
    DEFENSE(SkillCategory.SURVIVAL, "Defense", ResourceLocation.parse("minecraft:iron_chestplate"), "Increases armor.", 50),
    SATURATION(SkillCategory.SURVIVAL, "Saturation", ResourceLocation.parse("minecraft:cooked_beef"), "Improves hunger/saturation sustain.", 50),
    LEAPING(SkillCategory.SURVIVAL, "Leaping", ResourceLocation.parse("minecraft:rabbit_foot"), "Increases jump height.", 50),

    EFFICIENCY(SkillCategory.UTILITY, "Efficiency", ResourceLocation.parse("minecraft:iron_pickaxe"), "Increases mining speed.", 50),
    CHOPPING(SkillCategory.UTILITY, "Chopping", ResourceLocation.parse("minecraft:iron_axe"), "Increases chopping speed.", 50),
    FORAGING(SkillCategory.UTILITY, "Foraging", ResourceLocation.parse("minecraft:oak_sapling"), "Improves gathering outcomes.", 50),
    FORTUNE(SkillCategory.UTILITY, "Fortune", ResourceLocation.parse("minecraft:emerald"), "Improves loot/luck outcomes.", 50);

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
}
