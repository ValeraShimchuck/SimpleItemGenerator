package ua.valeriishymchuk.simpleitemgenerator.controller;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.standard.EnumArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import io.vavr.Tuple;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import ua.valeriishymchuk.simpleitemgenerator.common.commands.ArgumentParserWrapper;
import ua.valeriishymchuk.simpleitemgenerator.common.commands.argument.CustomPlayerArgument;
import ua.valeriishymchuk.simpleitemgenerator.common.item.NBTCustomItem;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.dto.GiveItemDTO;
import ua.valeriishymchuk.simpleitemgenerator.dto.WithdrawItemDTO;
import ua.valeriishymchuk.simpleitemgenerator.service.IInfoService;
import ua.valeriishymchuk.simpleitemgenerator.service.impl.ItemService;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class CommandsController {

    private static final String COMMAND_PERMISSION = "simpleitemgenerator.commands";
    private static final String COMMAND_PERMISSION_PREPEND = COMMAND_PERMISSION + ".";


    ItemService itemService;
    IInfoService infoService;

    private enum OccupiedSlotHandling {
        REPLACE,
        ADD
    }

    private void addItemToInventory(Option<ItemStack> itemOpt, int amount, Player player) {
        itemOpt.map(item -> Tuple.of(player, item))
                .peek(t -> t._2.setAmount(amount))
                .map(tuple -> tuple._1.getInventory().addItem(tuple._2))
                .peek(items -> dropItems(items, player.getLocation()));
    }

    public void setupCommands(CommandManager<CommandSender> commandManager) {
        Command.Builder<CommandSender> builder = commandManager.commandBuilder("simpleitemgenerator", "sig")
                .permission(COMMAND_PERMISSION_PREPEND + "general");
        commandManager.command(builder
                .handler(ctx -> KyoriHelper.sendMessage(ctx.getSender(), infoService.getUsage()))
        );
        commandManager.command(builder.literal("give")
                .permission(COMMAND_PERMISSION_PREPEND + "give")
                .argument(StringArgument.<CommandSender>builder("key")
                        .withSuggestionsProvider((ctx, s) -> itemService.getItemKeys()
                                .stream()
                                .filter(line -> line.contains(s)).collect(Collectors.toList()))
                )
                .argument(CustomPlayerArgument.<CommandSender>builder("player")
                        .asOptional()
                        .withParser(new ArgumentParserWrapper<>(
                                new PlayerArgument.PlayerParser<>(),
                                e -> {
                                    if (e instanceof PlayerArgument.PlayerParseException) {
                                        PlayerArgument.PlayerParseException ex = (PlayerArgument.PlayerParseException) e;
                                        return itemService.playerNotFound(ex.getInput());
                                    }
                                    return itemService.playerNotFound("[blank]");
                                }
                        ))
                )
                .argument(IntegerArgument.<CommandSender>builder("amount")
                        .withMin(1)
                        .asOptional())
                .handler(ctx -> {
                    String key = ctx.get("key");
                    int amount = ctx.getOrDefault("amount", 1);
                    Option<Player> playerOpt = Option.ofOptional(ctx.<Player>getOptional("player"))
                            .orElse(() -> Option.when(ctx.getSender() instanceof Player, () -> ((Player) ctx.getSender())));
                    GiveItemDTO result = itemService.giveItem(key, playerOpt.getOrNull());
                    KyoriHelper.sendMessage(ctx.getSender(), result.getMessage());
                    //playerOpt.flatMap(player ->  result.getItemStack().map(item -> Tuple.of(player, item)))
                    //        .peek(t -> t._2.setAmount(amount))
                    //        .map(tuple -> tuple._1.getInventory().addItem(tuple._2))
                    //        .peek(items -> dropItems(items, playerOpt.get().getLocation()));
                    playerOpt.peek(p -> addItemToInventory(result.getItemStack(), amount, p));
                }));


        commandManager.command(builder.literal("set_slot")
                .permission(COMMAND_PERMISSION_PREPEND + "set_slot")
                .argument(StringArgument.<CommandSender>builder("key")
                        .withSuggestionsProvider((ctx, s) -> itemService.getItemKeys()
                                .stream()
                                .filter(line -> line.contains(s)).collect(Collectors.toList()))
                )
                .argument(IntegerArgument.<CommandSender>builder("slot")
                        .withMin(0)
                        .withMax(InventoryType.PLAYER.getDefaultSize() - 1)
                )
                .argument(CustomPlayerArgument.<CommandSender>builder("player")
                        .asOptional()
                        .withParser(new ArgumentParserWrapper<>(
                                new PlayerArgument.PlayerParser<>(),
                                e -> {
                                    if (e instanceof PlayerArgument.PlayerParseException) {
                                        PlayerArgument.PlayerParseException ex = (PlayerArgument.PlayerParseException) e;
                                        return itemService.playerNotFound(ex.getInput());
                                    }
                                    return itemService.playerNotFound("[blank]");
                                }
                        ))
                )
                .argument(IntegerArgument.<CommandSender>builder("amount")
                        .withMin(1)
                        .asOptional())
                .argument(EnumArgument.<CommandSender, OccupiedSlotHandling>builder(OccupiedSlotHandling.class, "occupied_slot_handling")
                        .asOptionalWithDefault(OccupiedSlotHandling.REPLACE.name())
                        .build()
                )
                .handler(ctx -> {
                    String key = ctx.get("key");
                    int slot = ctx.get("slot");
                    Option<Player> playerOpt = Option.ofOptional(ctx.<Player>getOptional("player"))
                            .orElse(() -> Option.when(ctx.getSender() instanceof Player, () -> ((Player) ctx.getSender())));
                    GiveItemDTO result = itemService.giveItem(key, playerOpt.getOrNull(), slot);
                    KyoriHelper.sendMessage(ctx.getSender(), result.getMessage());
                    playerOpt.flatMap(player -> result.getItemStack().map(item -> Tuple.of(player, item)))
                            .peek(t -> t._2.setAmount(ctx.getOrDefault("amount", 1)))
                            .peek(tuple -> {
                                OccupiedSlotHandling occupiedSlotHandling = ctx.get("occupied_slot_handling");
                                ItemStack slotItem = tuple._1.getInventory().getItem(slot);
                                boolean isSlotOccupied = slotItem != null && !slotItem.getType().name().endsWith("AIR");
                                if (!isSlotOccupied || occupiedSlotHandling == OccupiedSlotHandling.REPLACE)
                                    tuple._1.getInventory().setItem(slot, tuple._2);
                                else {
                                    addItemToInventory(Option.of(tuple._2), tuple._2.getAmount(), playerOpt.get());
                                }
                            });
                }));
        commandManager.command(builder
                .literal("reload")
                .permission(COMMAND_PERMISSION_PREPEND + "reload")
                .handler(ctx -> KyoriHelper.sendMessage(ctx.getSender(), itemService.reload())));

        commandManager.command(
                builder.literal("try_withdraw")
                        .permission(COMMAND_PERMISSION_PREPEND + "try_withdraw")
                        .argument(StringArgument.<CommandSender>builder("key")
                                .withSuggestionsProvider((ctx, s) -> itemService.getItemKeys()
                                        .stream()
                                        .filter(line -> line.contains(s)).collect(Collectors.toList()))
                        )
                        .argument(CustomPlayerArgument.<CommandSender>builder("player")
                                .withParser(new ArgumentParserWrapper<>(
                                        new PlayerArgument.PlayerParser<>(),
                                        e -> {
                                            if (e instanceof PlayerArgument.PlayerParseException) {
                                                PlayerArgument.PlayerParseException ex = (PlayerArgument.PlayerParseException) e;
                                                return itemService.playerNotFound(ex.getInput());
                                            }
                                            return itemService.playerNotFound("[blank]");
                                        }
                                ))
                        )
                        .argument(IntegerArgument.<CommandSender>builder("amount")
                                .withMin(1))
                        .argument(StringArgument.greedy("command"))
                        .handler(ctx -> {
                            String key = ctx.get("key");
                            Option<Player> playerOpt = Option.ofOptional(ctx.<Player>getOptional("player"))
                                    .orElse(() -> Option.when(ctx.getSender() instanceof Player, () -> ((Player) ctx.getSender())));
                            int amount = ctx.getOrDefault("amount", 1);
                            WithdrawItemDTO result = itemService.withdrawItem(key, playerOpt.getOrNull(), amount);
                            KyoriHelper.sendMessage(ctx.getSender(), result.getSenderMessage());
                            result.getReceiverMessage().peek(c -> {
                                KyoriHelper.sendMessage(ctx.getSender(), c);
                            });
                            if (!result.isSuccess()) return;
                            playerOpt.peek(player -> {
                                AtomicInteger remainingAmount = new AtomicInteger(amount);
                                Arrays.stream(player.getInventory().getContents())
                                        .filter(item -> NBTCustomItem.getCustomItemId(item).map(s -> s.equals(key)).getOrElse(false))
                                        .forEach(item -> {
                                            if (remainingAmount.get() <= 0) return;
                                            int toRemove = Math.min(remainingAmount.get(), item.getAmount());
                                            remainingAmount.set(remainingAmount.get() - toRemove);
                                            item.setAmount(item.getAmount() - toRemove);
                                            if (item.getAmount() == 0) {
                                                player.getInventory().remove(item);
                                            }
                                        });

                            });
                            Bukkit.dispatchCommand(ctx.getSender(), ctx.get("command"));
                        })
        );
    }

    private void dropItems(Map<Integer, ItemStack> items, Location location) {
        items.forEach((idx, item) -> {
            AtomicInteger totalAmount = new AtomicInteger(item.getAmount());
            int stack = item.getMaxStackSize();
            while (totalAmount.get() > 0) {
                int toRemove = Math.min(totalAmount.get(), stack);
                totalAmount.set(totalAmount.get() - toRemove);
                ItemStack clone = item.clone();
                clone.setAmount(toRemove);
                location.getWorld().dropItemNaturally(location, clone);
            }
        });
    }

}
