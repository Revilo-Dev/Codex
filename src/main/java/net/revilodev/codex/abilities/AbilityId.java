package net.revilodev.codex.abilities;

import net.minecraft.resources.ResourceLocation;
import net.revilodev.codex.skills.SkillId;

public enum AbilityId {
    DASH("Dash", "Burst forward with a short sprint of momentum.", ResourceLocation.parse("minecraft:feather"), SkillId.AGILITY, 5, 120),
    LEAP("Leap", "Launch upward and forward to reposition aggressively.", ResourceLocation.parse("minecraft:rabbit_foot"), SkillId.AGILITY, 5, 160),
    HEAL("Heal", "Restore health instantly.", ResourceLocation.parse("minecraft:golden_apple"), SkillId.VITALITY, 5, 220),
    CLEANSE("Cleanse", "Strip negative effects and recover slightly.", ResourceLocation.parse("minecraft:milk_bucket"), SkillId.VITALITY, 5, 260),
    GUARD("Guard", "Brace yourself with temporary damage resistance.", ResourceLocation.parse("minecraft:shield"), SkillId.RESISTANCE, 5, 260),
    WARCRY("Warcry", "Empower yourself with a short combat buff.", ResourceLocation.parse("minecraft:goat_horn"), SkillId.STRENGTH, 5, 240),
    EXECUTION("Execution", "Strike a wounded target with heavy finishing damage.", ResourceLocation.parse("minecraft:iron_axe"), SkillId.STRENGTH, 5, 140),
    CLEAVE("Cleave", "Swing through nearby enemies in front of you.", ResourceLocation.parse("minecraft:diamond_sword"), SkillId.STRENGTH, 5, 180),
    OVERPOWER("Overpower", "Smash one target with force and knockback.", ResourceLocation.parse("minecraft:mace"), SkillId.STRENGTH, 5, 200),
    SCAVENGER("Magnetism", "Pull nearby dropped items toward you for a short period.", ResourceLocation.parse("minecraft:emerald"), SkillId.LUCK, 5, 200);

    private final String title;
    private final String description;
    private final ResourceLocation iconItem;
    private final SkillId scalingSkill;
    private final int maxRank;
    private final int baseCooldownTicks;

    AbilityId(String title, String description, ResourceLocation iconItem, SkillId scalingSkill, int maxRank, int baseCooldownTicks) {
        this.title = title;
        this.description = description;
        this.iconItem = iconItem;
        this.scalingSkill = scalingSkill;
        this.maxRank = maxRank;
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
        String name = this == EXECUTION ? "execute" : name().toLowerCase(java.util.Locale.ROOT);
        return ResourceLocation.fromNamespaceAndPath("codex", "textures/gui/abilities/" + name + ".png");
    }

    public SkillId scalingSkill() {
        return scalingSkill;
    }

    public int maxRank() {
        return maxRank;
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
