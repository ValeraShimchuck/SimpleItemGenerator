package ua.valeriishymchuk.simpleitemgenerator.controller;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import ua.valeriishymchuk.simpleitemgenerator.common.reflection.ReflectedRepresentations;
import ua.valeriishymchuk.simpleitemgenerator.common.scheduler.BukkitTaskScheduler;
import ua.valeriishymchuk.simpleitemgenerator.service.IItemService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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
