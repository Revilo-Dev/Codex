package net.revilodev.codex.abilities;

import net.minecraft.resources.ResourceLocation;
import net.revilodev.codex.skills.SkillId;

public enum AbilityId {
    DASH("Dash", "Burst forward with a short sprint of momentum.", ResourceLocation.parse("minecraft:feather"), SkillId.AGILITY, 5, 120),
    LEAP("Leap", "Launch upward and forward to reposition aggressively.", ResourceLocation.parse("minecraft:rabbit_foot"), SkillId.AGILITY, 5, 160),
    LUNGE("Lunge", "Launch toward the target, slam into it, and knock it away.", ResourceLocation.parse("minecraft:wind_charge"), SkillId.AGILITY, 5, 130),
    HEAL("Heal", "Restore health instantly.", ResourceLocation.parse("minecraft:golden_apple"), SkillId.VITALITY, 5, 220),
    CLEANSE("Cleanse", "Strip negative effects and recover slightly.", ResourceLocation.parse("minecraft:milk_bucket"), SkillId.VITALITY, 5, 260),
    WARCRY("Rampage", "Channel raw power for ten seconds of scaling strength.", ResourceLocation.parse("minecraft:goat_horn"), SkillId.STRENGTH, 5, 2400),
    EXECUTION("Execute", "Strike a wounded target with heavy finishing damage.", ResourceLocation.parse("minecraft:iron_axe"), SkillId.STRENGTH, 5, 140),
    CLEAVE("Cleave", "Swing through nearby enemies in front of you.", ResourceLocation.parse("minecraft:diamond_sword"), SkillId.STRENGTH, 5, 180),
    SCAVENGER("Magnetism", "Pull nearby dropped items toward you for a short period.", ResourceLocation.parse("minecraft:emerald"), SkillId.LUCK, 5, 200),
    OVERPOWER("Bash", "Smash one target with force and knockback.", ResourceLocation.parse("minecraft:mace"), SkillId.STRENGTH, 5, 200),
    GUARD("Guard", "Brace yourself with temporary damage resistance and throw enemies back.", ResourceLocation.parse("minecraft:shield"), SkillId.RESISTANCE, 5, 260),
    BLAST("Blast", "Detonate force around you without breaking blocks.", ResourceLocation.parse("minecraft:tnt"), SkillId.STRENGTH, 5, 210),
    BLAZE("Blaze", "Ignite nearby enemies in a growing radius. At max rank, the flames become soul fire.", ResourceLocation.parse("minecraft:blaze_powder"), SkillId.RESISTANCE, 5, 200),
    GLACIER("Glacier", "Fire an icicle through enemies, gaining piercing and damage each rank.", ResourceLocation.parse("minecraft:packed_ice"), SkillId.LUCK, 5, 170),
    SMITE("Smite", "Call lightning onto the healthiest enemy, chaining to more targets with each rank.", ResourceLocation.parse("minecraft:lightning_rod"), SkillId.RESISTANCE, 5, 180);

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
