package net.revilodev.codex.abilities.command;

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
import net.revilodev.codex.abilities.AbilitiesAttachments;
import net.revilodev.codex.abilities.AbilitiesNetwork;
import net.revilodev.codex.abilities.AbilityId;
import net.revilodev.codex.abilities.PlayerAbilities;

import java.util.Arrays;
import java.util.Locale;

public final class AbilitiesCommands {
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_ABILITIES =
            (ctx, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(AbilityId.values()).map(AbilityId::name), builder);

    private AbilitiesCommands() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(AbilitiesCommands::onRegisterCommands);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        registerAll(event.getDispatcher());
    }

    private static void registerAll(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("abilities").requires(source -> source.hasPermission(2));

        root.then(Commands.literal("points")
                .then(Commands.literal("add")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    PlayerAbilities data = player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
                                    data.addPoints(amount);
                                    AbilitiesNetwork.syncTo(player);
                                    return success(ctx.getSource(), "Added " + amount + " ability points.");
                                })))
                .then(Commands.literal("set")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    PlayerAbilities data = player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
                                    data.setPoints(amount);
                                    AbilitiesNetwork.syncTo(player);
                                    return success(ctx.getSource(), "Set ability points to " + amount + ".");
                                })))
                .then(Commands.literal("reset")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PlayerAbilities data = player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
                            data.setPoints(0);
                            AbilitiesNetwork.syncTo(player);
                            return success(ctx.getSource(), "Reset ability points.");
                        })));

        root.then(Commands.literal("unlock")
                .then(Commands.argument("ability", StringArgumentType.word()).suggests(SUGGEST_ABILITIES)
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            AbilityId id = parseAbility(StringArgumentType.getString(ctx, "ability"));
                            if (id == null) return failure(ctx.getSource(), "Unknown ability.");
                            PlayerAbilities data = player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
                            data.setRank(id, Math.max(1, data.rank(id)));
                            AbilitiesNetwork.syncTo(player);
                            return success(ctx.getSource(), "Unlocked " + id.name() + ".");
                        })));

        root.then(Commands.literal("setslot")
                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 5))
                        .then(Commands.argument("ability", StringArgumentType.word()).suggests(SUGGEST_ABILITIES)
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int slot = IntegerArgumentType.getInteger(ctx, "slot");
                                    AbilityId id = parseAbility(StringArgumentType.getString(ctx, "ability"));
                                    if (id == null) return failure(ctx.getSource(), "Unknown ability.");
                                    PlayerAbilities data = player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
                                    if (!data.assign(slot, id)) return failure(ctx.getSource(), "Could not assign slot.");
                                    AbilitiesNetwork.syncTo(player);
                                    return success(ctx.getSource(), "Assigned " + id.name() + " to slot " + slot + ".");
                                }))));

        root.then(Commands.literal("reset")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    PlayerAbilities data = player.getData(AbilitiesAttachments.PLAYER_ABILITIES.get());
                    data.adminReset();
                    AbilitiesNetwork.syncTo(player);
                    return success(ctx.getSource(), "Reset abilities, slots, cooldowns, and points.");
                }));

        dispatcher.register(root);
    }

    private static AbilityId parseAbility(String value) {
        try {
            return AbilityId.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int success(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), true);
        return 1;
    }

    private static int failure(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }
}
