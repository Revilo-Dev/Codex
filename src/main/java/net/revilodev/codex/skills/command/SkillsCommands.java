package net.revilodev.codex.skills.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.revilodev.codex.skills.PlayerSkills;
import net.revilodev.codex.skills.SkillCategory;
import net.revilodev.codex.skills.SkillId;
import net.revilodev.codex.skills.SkillsAttachments;
import net.revilodev.codex.skills.SkillsNetwork;
import net.revilodev.codex.skills.logic.SkillLogic;

import java.util.Arrays;
import java.util.Locale;

public final class SkillsCommands {
    private SkillsCommands() {}

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_SKILLS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    Arrays.stream(SkillId.values()).map(SkillId::name),
                    builder
            );

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_CATEGORIES =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    Arrays.stream(SkillCategory.values()).map(SkillCategory::name),
                    builder
            );

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SkillsCommands::onRegisterCommands);
    }

    private static void onRegisterCommands(RegisterCommandsEvent e) {
        registerAll(e.getDispatcher());
    }

    private static void registerAll(CommandDispatcher<CommandSourceStack> d) {
        var root = Commands.literal("skills")
                .requires(s -> s.hasPermission(2));

        root.then(Commands.literal("level")
                .then(Commands.literal("up")
                        .then(Commands.argument("skill", StringArgumentType.word())
                                .suggests(SUGGEST_SKILLS)
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            ServerPlayer sp = ctx.getSource().getPlayerOrException();

                                            SkillId id = parseSkill(StringArgumentType.getString(ctx, "skill"));
                                            int amt = IntegerArgumentType.getInteger(ctx, "amount");
                                            if (id == null) {
                                                ctx.getSource().sendFailure(Component.literal("Unknown skill."));
                                                return 0;
                                            }

                                            PlayerSkills data = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
                                            int before = data.level(id);
                                            int after = data.adminAddLevel(id, amt);

                                            SkillLogic.applyAllEffects(sp, data);
                                            SkillsNetwork.syncTo(sp);

                                            ctx.getSource().sendSuccess(() -> Component.literal(id.name() + " " + before + " -> " + after), true);
                                            return 1;
                                        })))));

        root.then(Commands.literal("points")
                .then(Commands.literal("add")
                        .then(Commands.argument("category", StringArgumentType.word())
                                .suggests(SUGGEST_CATEGORIES)
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            ServerPlayer sp = ctx.getSource().getPlayerOrException();

                                            SkillCategory c = parseCategory(StringArgumentType.getString(ctx, "category"));
                                            int amt = IntegerArgumentType.getInteger(ctx, "amount");
                                            if (c == null) {
                                                ctx.getSource().sendFailure(Component.literal("Unknown category."));
                                                return 0;
                                            }

                                            PlayerSkills data = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
                                            data.adminAddPoints(c, amt);
                                            SkillsNetwork.syncTo(sp);

                                            ctx.getSource().sendSuccess(() -> Component.literal("Added " + amt + " points to " + c.name()), true);
                                            return 1;
                                        }))
                                .executes(ctx -> {
                                    ServerPlayer sp = ctx.getSource().getPlayerOrException();

                                    SkillCategory c = parseCategory(StringArgumentType.getString(ctx, "category"));
                                    if (c == null) {
                                        ctx.getSource().sendFailure(Component.literal("Unknown category."));
                                        return 0;
                                    }

                                    PlayerSkills data = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
                                    data.adminAddPoints(c, 1);
                                    SkillsNetwork.syncTo(sp);

                                    ctx.getSource().sendSuccess(() -> Component.literal("Added 1 point to " + c.name()), true);
                                    return 1;
                                }))));

        root.then(Commands.literal("points")
                .then(Commands.literal("reset")
                        .then(Commands.argument("category", StringArgumentType.word())
                                .suggests(SUGGEST_CATEGORIES)
                                .executes(ctx -> {
                                    ServerPlayer sp = ctx.getSource().getPlayerOrException();

                                    SkillCategory c = parseCategory(StringArgumentType.getString(ctx, "category"));
                                    if (c == null) {
                                        ctx.getSource().sendFailure(Component.literal("Unknown category."));
                                        return 0;
                                    }

                                    PlayerSkills data = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
                                    data.adminResetCategoryPoints(c);
                                    SkillsNetwork.syncTo(sp);

                                    ctx.getSource().sendSuccess(() -> Component.literal("Reset points/progress for " + c.name()), true);
                                    return 1;
                                }))));

        root.then(Commands.literal("reset")
                .executes(ctx -> {
                    ServerPlayer sp = ctx.getSource().getPlayerOrException();

                    PlayerSkills data = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
                    data.adminResetAll();

                    SkillLogic.applyAllEffects(sp, data);
                    SkillsNetwork.syncTo(sp);

                    ctx.getSource().sendSuccess(() -> Component.literal("Reset all skills and points."), true);
                    return 1;
                }));

        d.register(root);

        d.register(Commands.literal("skill")
                .requires(s -> s.hasPermission(2))
                .redirect(d.getRoot().getChild("skills")));

        var codex = Commands.literal("codex")
                .requires(s -> s.hasPermission(2));

        codex.then(Commands.literal("points")
                .then(Commands.literal("add")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                                    int amt = IntegerArgumentType.getInteger(ctx, "amount");
                                    PlayerSkills data = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
                                    data.adminAddPoints(amt);
                                    SkillsNetwork.syncTo(sp);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Added " + amt + " skill points."), true);
                                    return 1;
                                })))
                .then(Commands.literal("set")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                                    int amt = IntegerArgumentType.getInteger(ctx, "amount");
                                    PlayerSkills data = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
                                    data.adminSetPoints(amt);
                                    SkillsNetwork.syncTo(sp);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Set skill points to " + amt + "."), true);
                                    return 1;
                                })))
                .then(Commands.literal("reset")
                        .executes(ctx -> {
                            ServerPlayer sp = ctx.getSource().getPlayerOrException();
                            PlayerSkills data = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
                            data.adminResetPoints();
                            SkillsNetwork.syncTo(sp);
                            ctx.getSource().sendSuccess(() -> Component.literal("Reset skill points and progress."), true);
                            return 1;
                        })));

        codex.then(Commands.literal("skills")
                .then(Commands.literal("reset")
                        .executes(ctx -> {
                            ServerPlayer sp = ctx.getSource().getPlayerOrException();
                            PlayerSkills data = sp.getData(SkillsAttachments.PLAYER_SKILLS.get());
                            data.adminResetAll();
                            SkillLogic.applyAllEffects(sp, data);
                            SkillsNetwork.syncTo(sp);
                            ctx.getSource().sendSuccess(() -> Component.literal("Reset all skills, points, and progress."), true);
                            return 1;
                        })));

        d.register(codex);
    }

    private static SkillId parseSkill(String s) {
        try {
            return SkillId.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static SkillCategory parseCategory(String s) {
        try {
            return SkillCategory.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return null;
        }
    }
}
