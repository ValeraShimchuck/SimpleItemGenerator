package ua.valeriishymchuk.simpleitemgenerator.service;

import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import ua.valeriishymchuk.simpleitemgenerator.dto.GiveItemDTO;
import ua.valeriishymchuk.simpleitemgenerator.dto.ItemUsageResultDTO;

import java.util.List;

public interface IItemService {

    ItemUsageResultDTO useItem(Player player, Action action, ItemStack item, @Nullable Block clickedBlock);
    ItemUsageResultDTO dropItem(Player player, ItemStack item);
    ItemUsageResultDTO useItemAt(Player player, boolean isRightClicked, Entity clicked, ItemStack item);
    boolean canBePutInInventory(ItemStack item);
    boolean canBeMoved(ItemStack itemStack);
    boolean shouldRemoveOnDeath(ItemStack item);

    void updateItem(ItemStack itemStack, Player player);
    boolean canBeUsedInCraft(ItemStack item);

    GiveItemDTO giveItem(String key, @Nullable Player player);
    List<String> getItemKeys();
    long getUpdatePeriodTicks();
    Component reload();
    Component playerNotFound(String input);
}
