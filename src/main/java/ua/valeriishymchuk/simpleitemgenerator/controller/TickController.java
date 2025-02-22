package ua.valeriishymchuk.simpleitemgenerator.controller;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.reflection.ReflectedRepresentations;
import ua.valeriishymchuk.simpleitemgenerator.common.scheduler.BukkitTaskScheduler;
import ua.valeriishymchuk.simpleitemgenerator.common.tick.TickerTime;
import ua.valeriishymchuk.simpleitemgenerator.dto.ItemUsageGeneralDTO;
import ua.valeriishymchuk.simpleitemgenerator.dto.ItemUsageResultDTO;
import ua.valeriishymchuk.simpleitemgenerator.entity.UsageEntity;
import ua.valeriishymchuk.simpleitemgenerator.service.IItemService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class TickController {

    IItemService itemService;
    BukkitTaskScheduler taskScheduler;
    TickerTime tickerTime;


    public void start() {
        taskScheduler.runTaskLater(() -> {
            updateItems();
            tickItems();
            start();
        }, itemService.getUpdatePeriodTicks());
    }


    private void tickItems() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            ItemStack[] contents = player.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item == null || item.getType().name().endsWith("AIR")) continue;
                ItemUsageResultDTO result = itemService.tickItem(new ItemUsageGeneralDTO(
                        player,
                        item,
                        tickerTime.getTick(),
                        i
                ));
                handleResult(result, item, player);
            }
        });
    }

    private void handleResult(ItemUsageResultDTO result, ItemStack item, Player player) {
        result.getCommands().forEach(commands -> {
            CommandSender sender = commands.isExecuteAsConsole() ? Bukkit.getConsoleSender() : player;
            Bukkit.dispatchCommand(sender, commands.getCommand());
        });
        result.getMessage().peek(message -> KyoriHelper.sendMessage(player, message));
        if (!result.getConsume().isNone() && item != null) {
            ItemStack clonedItem = item.clone();

            if (result.getConsume().isAmount()) {
                AtomicInteger totalAmount = new AtomicInteger(result.getConsume().getAmount());
                player.getInventory().forEach(itemCons -> {
                    if (itemCons == null || itemCons.getType().name().endsWith("AIR")) return;
                    if (!itemService.areEqual(itemCons, clonedItem)) return;
                    if (totalAmount.get() <= 0) return;
                    int toRemove = Math.min(totalAmount.get(), itemCons.getAmount());
                    itemCons.setAmount(itemCons.getAmount() - toRemove);
                    totalAmount.set(totalAmount.get() - toRemove);
                });
            } else {
                boolean onlyStack = result.getConsume().getConsumeType() == UsageEntity.ConsumeType.STACK;
                if (onlyStack) {
                    item.setAmount(0);
                } else {
                    player.getInventory().forEach(itemCons -> {
                        if (itemCons == null || itemCons.getType().name().endsWith("AIR")) return;
                        if (!itemService.areEqual(itemCons, clonedItem)) return;
                        itemCons.setAmount(0);
                    });
                }
            }

        }
    }

    private void updateItems() {
        Set<World> worlds = new HashSet<>();
        Bukkit.getOnlinePlayers().forEach(player -> {
            worlds.add(player.getWorld());
            InventoryView inventoryView = player.getOpenInventory();
            Stream.of(
                            inventoryView.getTopInventory().getContents(),
                            inventoryView.getBottomInventory().getContents()
                    ).flatMap(Arrays::stream)
                    .filter(Objects::nonNull)
                    .filter(ItemStack::hasItemMeta)
                    .forEach(item -> itemService.updateItem(item, player));
        });
        worlds.stream()
                .flatMap(w -> w.getEntitiesByClass(Item.class).stream())
                .filter(item -> item.getItemStack().hasItemMeta())
                .forEach(item -> {
                    ItemStack itemStack = item.getItemStack();
                    itemService.updateItem(itemStack, null);
                    item.setItemStack(itemStack);
                });
        worlds.stream()
                .flatMap(w -> w.getEntitiesByClass(LivingEntity.class).stream())
                .filter(e -> !(e instanceof Player))
                .forEach(e -> {
                    EntityEquipment equipment = e.getEquipment();
                    Option.of(equipment.getBoots()).filter(ItemStack::hasItemMeta)
                            .peek(item -> itemService.updateItem(item, null))
                            .peek(equipment::setBoots);
                    Option.of(equipment.getLeggings()).filter(ItemStack::hasItemMeta)
                            .peek(item -> itemService.updateItem(item, null))
                            .peek(equipment::setLeggings);
                    Option.of(equipment.getChestplate()).filter(ItemStack::hasItemMeta)
                            .peek(item -> itemService.updateItem(item, null))
                            .peek(equipment::setChestplate);
                    Option.of(equipment.getHelmet()).filter(ItemStack::hasItemMeta)
                            .peek(item -> itemService.updateItem(item, null))
                            .peek(equipment::setHelmet);
                    Option.of(equipment.getItemInHand()).filter(ItemStack::hasItemMeta)
                            .peek(item -> itemService.updateItem(item, null))
                            .peek(equipment::setItemInHand);
                    ReflectedRepresentations.EntityEquipment.getItemInOffhand(equipment)
                            .filter(ItemStack::hasItemMeta)
                            .peek(item -> itemService.updateItem(item, null))
                            .peek(i -> ReflectedRepresentations.EntityEquipment.setItemInOffhand(equipment, i));
                });
    }

}
