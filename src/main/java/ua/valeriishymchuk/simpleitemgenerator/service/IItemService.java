package ua.valeriishymchuk.simpleitemgenerator.service;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import ua.valeriishymchuk.simpleitemgenerator.dto.*;

import java.util.List;
import java.util.Set;

public interface IItemService {

    ItemUsageResultDTO useItem(ItemUsageBlockDTO itemUsageBlockDTO);
    ItemUsageResultDTO dropItem(ItemUsageGeneralDTO itemUsageGeneralDTO);
    ItemUsageResultDTO useItemAt(ItemUsageEntityDTO itemUsageEntityDTO);
    ItemUsageResultDTO tickItem(ItemUsageGeneralDTO itemUsageGeneralDTO);
    ItemUsageResultDTO moveItem(SlotChangeDTO slotChanges, int selectedHotbarSlot);
    boolean canBePutInInventory(ItemStack item);
    boolean canBeMoved(ItemStack itemStack);
    boolean shouldRemoveOnDeath(ItemStack item);
    boolean areEqual(ItemStack item, ItemStack item2);

    void updateItem(ItemStack itemStack, @Nullable Player player);
    boolean canBeUsedInCraft(ItemStack item);

    GiveItemDTO giveItem(String key, @Nullable Player player, Integer slot);

    default GiveItemDTO giveItem(String key, @Nullable Player player) {
        return giveItem(key, player, null);
    }

    WithdrawItemDTO withdrawItem(String key, @Nullable Player player, int amount);

    Set<String> getItemKeys();
    long getUpdatePeriodTicks();
    Component reload();
    Component playerNotFound(String input);

    void cooldownAutoSave();
}
