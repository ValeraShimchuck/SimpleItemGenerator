package ua.valeriishymchuk.simpleitemgenerator.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import ua.valeriishymchuk.simpleitemgenerator.common.scheduler.BukkitTaskScheduler;
import ua.valeriishymchuk.simpleitemgenerator.service.IItemService;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class TickController {

    IItemService itemService;
    BukkitTaskScheduler taskScheduler;


    public void start() {
        taskScheduler.runTaskLater(() -> {
            updateItems();
            start();
        }, itemService.getUpdatePeriodTicks());
    }

    private void updateItems() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            InventoryView inventoryView = player.getOpenInventory();
            Stream.of(
                            inventoryView.getTopInventory().getContents(),
                            inventoryView.getBottomInventory().getContents()
                    ).flatMap(Arrays::stream)
                    .filter(Objects::nonNull)
                    .filter(ItemStack::hasItemMeta)
                    .forEach(item -> itemService.updateItem(item, player));
        });
    }

}
