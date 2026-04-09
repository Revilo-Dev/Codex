package net.revilodev.codex.abilities;

import net.minecraft.resources.ResourceLocation;
import net.revilodev.codex.skills.SkillId;

public enum AbilityId {
    DASH("Dash", "Dash forward with momentum.", ResourceLocation.parse("minecraft:feather"), SkillId.AGILITY, 5, 120),
    LEAP("Leap", "Leap up a few blocks.", ResourceLocation.parse("minecraft:rabbit_foot"), SkillId.AGILITY, 5, 160),
    LUNGE("Lunge", "Dash Into the target, slam into it, and knock it away.", ResourceLocation.parse("minecraft:wind_charge"), SkillId.AGILITY, 5, 130),
    HEAL("Heal", "Restore health instantly.", ResourceLocation.parse("minecraft:golden_apple"), SkillId.VITALITY, 5, 220),
    CLEANSE("Cleanse", "Cleanse negative effects and recover slightly.", ResourceLocation.parse("minecraft:milk_bucket"), SkillId.VITALITY, 5, 260),
    WARCRY("Rampage", "Short burst of Strength.", ResourceLocation.parse("minecraft:goat_horn"), SkillId.STRENGTH, 5, 2400),
    EXECUTION("Execute", "Short, High damage strike.", ResourceLocation.parse("minecraft:iron_axe"), SkillId.STRENGTH, 5, 140),
    CLEAVE("Cleave", "Cleave through enemies in front of you.", ResourceLocation.parse("minecraft:diamond_sword"), SkillId.STRENGTH, 5, 180),
    GUARD("Guard", "Gain temporary resistance and push mobs back.", ResourceLocation.parse("minecraft:shield"), SkillId.RESISTANCE, 5, 260),
    OVERPOWER("Bash", "Smash enemies with force and knockback.", ResourceLocation.parse("minecraft:mace"), SkillId.STRENGTH, 5, 200),
    SCAVENGER("Toxic", "Poison nearby enemies.", ResourceLocation.parse("minecraft:fermented_spider_eye"), SkillId.LUCK, 5, 200),
    BLAST("Blast", "Summon an explosion.", ResourceLocation.parse("minecraft:tnt"), SkillId.STRENGTH, 5, 250),
    BLAZE("Blaze", "Ignite nearby enemies on fire, +radius per lv, ignites into soul fire on lv 5.", ResourceLocation.parse("minecraft:blaze_powder"), SkillId.RESISTANCE, 5, 200),
    GLACIER("Glacier", "Summon a Piercing icicle, +1 piercing per lv, bursts into shrapnel on lv 5", ResourceLocation.parse("minecraft:packed_ice"), SkillId.LUCK, 5, 170),
    SMITE("Smite", "Smite an enemy with lighting, +1 bold per lv ", ResourceLocation.parse("minecraft:lightning_rod"), SkillId.RESISTANCE, 5, 180);

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
        return AbilityConfig.maxRank(this);
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
