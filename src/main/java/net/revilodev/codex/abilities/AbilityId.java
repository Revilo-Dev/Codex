package net.revilodev.codex.abilities;

import net.minecraft.resources.ResourceLocation;
import net.revilodev.codex.skills.SkillId;

public enum AbilityId {
    DASH("Dash", "Dash forward with momentum.", ResourceLocation.parse("minecraft:feather"), SkillId.AGILITY, 3, 120),
    LEAP("Leap", "Leap up a few blocks.", ResourceLocation.parse("minecraft:rabbit_foot"), SkillId.AGILITY, 3, 160),
    LUNGE("Lunge", "Dash Into the target, slam into it, and knock it away.", ResourceLocation.parse("minecraft:wind_charge"), SkillId.AGILITY, 3, 130),
    HEAL("Heal", "Restore health instantly.", ResourceLocation.parse("minecraft:golden_apple"), SkillId.VITALITY, 5, 220),
    CLEANSE("Cleanse", "Cleanse negative effects, extinguish fire, and recover slightly.", ResourceLocation.parse("minecraft:milk_bucket"), SkillId.VITALITY, 1, 200),
    WARCRY("Rampage", "Short burst of Strength.", ResourceLocation.parse("minecraft:goat_horn"), SkillId.STRENGTH, 5, 2400),
    EXECUTION("Execute", "Short, High damage strike.", ResourceLocation.parse("minecraft:iron_axe"), SkillId.STRENGTH, 4, 140),
    CLEAVE("Cleave", "Cleave through enemies in front of you.", ResourceLocation.parse("minecraft:diamond_sword"), SkillId.STRENGTH, 4, 180),
    GUARD("Guard", "Gain temporary resistance and push mobs back.", ResourceLocation.parse("minecraft:shield"), SkillId.RESISTANCE, 4, 260),
    OVERPOWER("Bash", "Smash enemies with force and knockback.", ResourceLocation.parse("minecraft:mace"), SkillId.STRENGTH, 4, 200),
    SCAVENGER("Toxic", "Poison nearby enemies.", ResourceLocation.parse("minecraft:fermented_spider_eye"), SkillId.LUCK, 2, 200),
    BLAST("Blast", "Summon an explosion.", ResourceLocation.parse("minecraft:tnt"), SkillId.STRENGTH, 5, 250),
    BLAZE("Blaze", "Ignite nearby enemies on fire and gain soul fire at max level.", ResourceLocation.parse("minecraft:blaze_powder"), SkillId.RESISTANCE, 2, 200),
    GLACIER("Glacier", "Summon icy projectiles, +1 projectile per level, bursts into shrapnel on max level.", ResourceLocation.parse("minecraft:packed_ice"), SkillId.LUCK, 5, 170),
    SMITE("Smite", "Smite an enemy with lighting, +1 bolt per level ", ResourceLocation.parse("minecraft:lightning_rod"), SkillId.RESISTANCE, 5, 180);

    private final String title;
    private final String description;
    private final ResourceLocation iconItem;
    private final SkillId scalingSkill;
    private final int defaultMaxRank;
    private final int baseCooldownTicks;

    AbilityId(String title, String description, ResourceLocation iconItem, SkillId scalingSkill, int maxRank, int baseCooldownTicks) {
        this.title = title;
        this.description = description;
        this.iconItem = iconItem;
        this.scalingSkill = scalingSkill;
        this.defaultMaxRank = maxRank;
        this.baseCooldownTicks = baseCooldownTicks;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public ResourceLocation iconItem() {
        return iconItem;
    }

    public ResourceLocation iconTexture() {
        return switch (this) {
            case EXECUTION -> ResourceLocation.fromNamespaceAndPath("codex", "textures/gui/abilities/execute.png");
            case SCAVENGER -> ResourceLocation.fromNamespaceAndPath("codex", "textures/gui/abilities/magnetism.png");
            case OVERPOWER -> ResourceLocation.fromNamespaceAndPath("codex", "textures/gui/abilities/overpower.png");
            default -> ResourceLocation.fromNamespaceAndPath("codex", "textures/gui/abilities/" + name().toLowerCase(java.util.Locale.ROOT) + ".png");
        };
    }

    public SkillId scalingSkill() {
        return scalingSkill;
    }

    public int maxRank() {
        return Math.max(1, Math.min(defaultMaxRank, AbilityConfig.maxRank(this)));
    }

    public int defaultMaxRank() {
        return defaultMaxRank;
    }

    public int baseCooldownTicks() {
        return baseCooldownTicks;
    }

    public static AbilityId byOrdinal(int ordinal) {
        AbilityId[] values = values();
        if (ordinal < 0 || ordinal >= values.length) return null;
        return values[ordinal];
    }
}
