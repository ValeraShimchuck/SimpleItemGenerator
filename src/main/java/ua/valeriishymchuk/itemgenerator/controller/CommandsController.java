package ua.valeriishymchuk.itemgenerator.controller;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import io.vavr.Tuple;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ua.valeriishymchuk.itemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.itemgenerator.dto.GiveItemDTO;
import ua.valeriishymchuk.itemgenerator.service.IItemService;

import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class CommandsController {

    private static final String COMMAND_PERMISSION_PREPEND = "itemgenerator.commands.";

    IItemService itemService;

    public void setupCommands(CommandManager<CommandSender> commandManager) {
        Command.Builder<CommandSender> builder = commandManager.commandBuilder("itemgenerator")
                .permission(COMMAND_PERMISSION_PREPEND + "general");
        commandManager.command(builder.literal("give")
                .permission(COMMAND_PERMISSION_PREPEND + "give")
                .argument(StringArgument.<CommandSender>builder("key")
                        .withSuggestionsProvider((ctx, s) -> itemService.getItemKeys()
                                .stream()
                                .filter(line -> line.contains(s)).collect(Collectors.toList())))
                .argument(PlayerArgument.optional("player"))
                .handler(ctx -> {
                    String key = ctx.get("key");
                    Option<Player> playerOpt = Option.<Player>of(ctx.get("player"))
                            .orElse(() -> Option.when(ctx.getSender() instanceof Player, () -> ((Player) ctx.getSender())));
                    GiveItemDTO result = itemService.giveItem(key, playerOpt.getOrNull());
                    KyoriHelper.sendMessage(ctx.getSender(), result.getMessage());
                    playerOpt.flatMap(player ->  result.getItemStack().map(item -> Tuple.of(player, item)))
                            .peek(tuple -> tuple._1.getInventory().addItem(tuple._2));
                }));

        commandManager.command(builder
                        .literal("reload")
                .permission(COMMAND_PERMISSION_PREPEND + "reload")
                .handler(ctx -> KyoriHelper.sendMessage(ctx.getSender(), itemService.reload())));
    }

}
