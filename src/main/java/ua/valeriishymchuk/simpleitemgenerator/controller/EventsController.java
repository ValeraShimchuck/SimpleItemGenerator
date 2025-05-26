package ua.valeriishymchuk.simpleitemgenerator.controller;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import ua.valeriishymchuk.simpleitemgenerator.common.item.ItemCopy;
import ua.valeriishymchuk.simpleitemgenerator.common.item.NBTCustomItem;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.reflection.ReflectedRepresentations;
import ua.valeriishymchuk.simpleitemgenerator.common.scheduler.BukkitTaskScheduler;
import ua.valeriishymchuk.simpleitemgenerator.common.slot.EquipmentToSlotConverter;
import ua.valeriishymchuk.simpleitemgenerator.common.tick.TickTimer;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.SlotPredicate;
import ua.valeriishymchuk.simpleitemgenerator.common.version.FeatureSupport;
import ua.valeriishymchuk.simpleitemgenerator.dto.*;
import ua.valeriishymchuk.simpleitemgenerator.entity.UsageEntity;
import ua.valeriishymchuk.simpleitemgenerator.service.IInfoService;
import ua.valeriishymchuk.simpleitemgenerator.service.IItemService;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ua.valeriishymchuk.simpleitemgenerator.common.item.ItemCopy.isAir;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventsController implements Listener {

    IItemService itemService;
    IInfoService infoService;
    TickTimer tickerTime;
    BukkitTaskScheduler scheduler;
    Map<Player, Long> lastDropTick = new WeakHashMap<>();
    Map<Player, Long> lastPlayerClickTick = new WeakHashMap<>();
    Map<Player, Map<Integer, Long>> lastUsedItemTicks = new WeakHashMap<>();
    Map<Player, Tuple2<Long, Integer>> playerTickSlotMap = new WeakHashMap<>();

    public EventsController(IItemService itemService, IInfoService infoService, TickTimer tickerTime, BukkitTaskScheduler scheduler) {
        this.itemService = itemService;
        this.infoService = infoService;
        this.tickerTime = tickerTime;
        this.scheduler = scheduler;
    }


    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        scheduler.runTaskLater(() -> {
            infoService.getMessage(event.getPlayer()).peek(msg -> KyoriHelper.sendMessage(event.getPlayer(), msg));
            infoService.getNewUpdateMessage(event.getPlayer())
                    .thenAccept(msgOpt -> {
                                msgOpt.peek(msg -> KyoriHelper.sendMessage(event.getPlayer(), msg));
                            }
                    ).exceptionally(e -> {
                        e.printStackTrace();
                        return null;
                    });
        }, 40L);
    }

    private ItemCopy[] getInventorySnapshot(InventoryView inventory) {
        Inventory topInventory = inventory.getTopInventory();
        Inventory bottomInventory = inventory.getBottomInventory();
        int topSize = topInventory.getSize();
        int bottomSize = bottomInventory.getSize();
        boolean hasOffhand = FeatureSupport.MODERN_COMBAT;
        int offsize = 4 + (hasOffhand ? 1 : 0);
        ItemCopy[] snapshot = new ItemCopy[topSize + bottomSize + offsize];


        for (int i = 0; i < bottomSize; i++) {
            ItemStack item = bottomInventory.getItem(i);
            snapshot[i] = ItemCopy.from(item);
        }


        snapshot[bottomSize] = ItemCopy.from(((Player) inventory.getBottomInventory().getHolder()).getInventory().getBoots());
        snapshot[bottomSize + 1] = ItemCopy.from(((Player) inventory.getBottomInventory().getHolder()).getInventory().getLeggings());
        snapshot[bottomSize + 2] = ItemCopy.from(((Player) inventory.getBottomInventory().getHolder()).getInventory().getChestplate());
        snapshot[bottomSize + 3] = ItemCopy.from(((Player) inventory.getBottomInventory().getHolder()).getInventory().getHelmet());
        if (hasOffhand)
            snapshot[bottomSize + 4] = ItemCopy.from(((Player) inventory.getBottomInventory().getHolder()).getInventory().getItem(40));

        for (int i = 0; i < topSize; i++) {
            ItemStack item = topInventory.getItem(i);
            snapshot[i + bottomSize + offsize] = ItemCopy.from(item);
        }


        return snapshot;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onItemClick(InventoryClickEvent event) {
        if (!(event.getClickedInventory() instanceof PlayerInventory) &&
                event.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY &&
                event.getAction() != InventoryAction.HOTBAR_SWAP
        ) return;
        boolean isClickedSlotEmptied;
        if (isAir(event.getCurrentItem())) {
            isClickedSlotEmptied = false;
        } else if (event.getAction() == InventoryAction.PICKUP_ALL ||
                event.getAction() == InventoryAction.SWAP_WITH_CURSOR
        ) {
            isClickedSlotEmptied = true;
        } else if ((event.getAction() == InventoryAction.PICKUP_ONE ||
                event.getAction() == InventoryAction.PICKUP_HALF)
                && event.getCurrentItem().getAmount() == 1) {
            isClickedSlotEmptied = true;
        } else {
            isClickedSlotEmptied = event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY ||
                    event.getAction() == InventoryAction.HOTBAR_SWAP;
        }

        Map<Integer, ItemStack> movedSlots = new HashMap<>();

        Player player = (Player) event.getWhoClicked();

        InventoryAction action = event.getAction();
        switch (action) {
            case MOVE_TO_OTHER_INVENTORY:
                handleMoveToOtherInventory(event, movedSlots, player);
                break;
            case HOTBAR_SWAP:
                handleHotbarSwap(event, movedSlots, player);
                break;
            case SWAP_WITH_CURSOR:
                handleSwapWithCursor(event, movedSlots);
                break;
            case PLACE_ONE:
            case PLACE_SOME:
            case PLACE_ALL:
                handlePlaceActions(event, movedSlots);
                break;
        }
        Map<ItemUsageResultDTO, ItemStack> results = new HashMap<>();
        if (isClickedSlotEmptied) {
            results.put(itemService.moveItem(player, new SlotChangeDTO(
                            false,
                            new SlotPredicate.Input(event.getSlot(), EquipmentToSlotConverter.convert(event.getSlot(), player).getOrNull()),
                            tickerTime.getTick(),
                            ItemCopy.from(event.getCurrentItem())
                    ), player.getInventory().getHeldItemSlot()),
                    event.getCurrentItem()
            );
        }


        movedSlots.forEach((slot, item) -> {
            if (item.isSimilar(event.getClickedInventory().getItem(slot))) return;
            results.put(itemService.moveItem(player,new SlotChangeDTO(
                            true,
                            new SlotPredicate.Input(event.getSlot(), EquipmentToSlotConverter.convert(event.getSlot(), player).getOrNull()),
                            tickerTime.getTick(),
                            ItemCopy.from(item)
                    ), player.getInventory().getHeldItemSlot()),
                    item
            );
        });

        if (results.isEmpty()) return;

        boolean isAnyCancelled = results.keySet().stream().anyMatch(ItemUsageResultDTO::isShouldCancel);

        if (isAnyCancelled) {
            results.keySet().stream()
                    .filter(usage -> !usage.isShouldCancel())
                    .forEach(results::remove);
        }

        results.forEach((usage, item) ->
                handleResult(usage, item, player, event, false));

        //results.stream().reduce((left, right) -> {
        //    Component message = left.getMessage().map(leftMsg -> right.getMessage()
        //            .map(rightMsg -> leftMsg.appendNewline()
        //                    .append(rightMsg)
        //            ).getOrElse(leftMsg)).orElse(right.getMessage()).getOrNull();
        //    return new ItemUsageResultDTO(
        //            message,
        //            Stream.of(left.getCommands(), right.getCommands())
        //                    .flatMap(Collection::stream).collect(Collectors.toList()),
        //            isAnyCancelled,
        //            left.getConsume()
        //    )
        //});

    }

    private void handleMoveToOtherInventory(InventoryClickEvent event, Map<Integer, ItemStack> movedSlots, Player player) {
        // Determine the destination inventory (opposite of the clicked inventory)
        Inventory destination = event.getClickedInventory() instanceof PlayerInventory
                ? event.getView().getTopInventory()
                : player.getInventory();

        ItemStack movedItem = event.getCurrentItem().clone();
        if (isAir(movedItem)) return;

        // Clone destination contents to simulate the move
        ItemStack[] originalContents = destination.getContents().clone();
        Inventory tempInv = Bukkit.createInventory(
                null,
                (int) Math.ceil(((double) destination.getSize()) / 9) * 9
        );
        tempInv.setContents(originalContents.clone());

        // Simulate adding the item
        tempInv.addItem(movedItem);

        // Find slots that were filled
        for (int slot = 0; slot < originalContents.length; slot++) {
            ItemStack original = originalContents[slot];
            ItemStack updated = tempInv.getItem(slot);
            if (isAir(original) && !isAir(updated)) {
                movedSlots.put(slot, updated.clone());
            }
        }
    }

    private void handleHotbarSwap(InventoryClickEvent event, Map<Integer, ItemStack> movedSlots, Player player) {
        int hotbarSlot = event.getHotbarButton();
        if (hotbarSlot == -1) hotbarSlot = 40;
        ItemStack hotbarItem = player.getInventory().getItem(hotbarSlot);
        int clickedSlot = event.getSlot();
        Inventory clickedInventory = event.getClickedInventory();

        if (isAir(clickedInventory.getItem(clickedSlot)) && !isAir(hotbarItem)) {
            movedSlots.put(clickedSlot, hotbarItem.clone());
        }
    }


    private void handleSwapWithCursor(InventoryClickEvent event, Map<Integer, ItemStack> movedSlots) {
        ItemStack cursorItem = event.getCursor();
        int clickedSlot = event.getSlot();
        Inventory clickedInventory = event.getClickedInventory();

        if (isAir(clickedInventory.getItem(clickedSlot)) && !isAir(cursorItem)) {
            movedSlots.put(clickedSlot, cursorItem.clone());
        }
    }

    private void handlePlaceActions(InventoryClickEvent event, Map<Integer, ItemStack> movedSlots) {
        int clickedSlot = event.getSlot();
        Inventory clickedInventory = event.getClickedInventory();
        ItemStack originalItem = clickedInventory.getItem(clickedSlot);
        ItemStack cursorItem = event.getCursor();

        if (isAir(originalItem) && !isAir(cursorItem)) {
            ItemStack newItem = cursorItem.clone();
            if (event.getAction() == InventoryAction.PLACE_ONE) {
                newItem.setAmount(1);
            } else {
                int maxStack = newItem.getMaxStackSize();
                newItem.setAmount(Math.min(cursorItem.getAmount(), maxStack));
            }
            movedSlots.put(clickedSlot, newItem);
        }
    }

    private ItemMovement findFirstAvailableSlot(ItemStack item, Inventory inventory, int prevSlot, int startSlot, int endSlot) {
        int amount = item.getAmount();
        int stackSize = item.getMaxStackSize();
        List<ItemMovement.NewItemInfo> newItemInfos = new ArrayList<>();
        for (int i = startSlot; i < endSlot; i++) {
            ItemStack existingItem = inventory.getItem(i);
            if (existingItem == null || existingItem.getType().name().endsWith("AIR")) {
                return new ItemMovement(
                        prevSlot,
                        0,
                        io.vavr.collection.List.of(
                                Collections.singleton(new ItemMovement.NewItemInfo(i, existingItem.getAmount() + amount, true)),
                                newItemInfos
                        ).flatMap(c -> c)
                );
            } else if (!existingItem.isSimilar(item)) {
                continue;
            } else if (existingItem.getAmount() + amount <= stackSize) {
                return new ItemMovement(
                        prevSlot,
                        0,
                        io.vavr.collection.List.of(
                                Collections.singleton(new ItemMovement.NewItemInfo(i, existingItem.getAmount() + amount, true)),
                                newItemInfos
                        ).flatMap(c -> c)
                );
            } else {
                int availableAmount = stackSize - existingItem.getAmount();
                amount -= availableAmount;
                newItemInfos.add(new ItemMovement.NewItemInfo(i, stackSize, true));
                if (amount == 0) {
                    return new ItemMovement(
                            prevSlot,
                            0,
                            io.vavr.collection.List.of(
                                    newItemInfos
                            ).flatMap(c -> c)
                    );
                }
            }
        }
        return new ItemMovement(
                prevSlot,
                amount,
                io.vavr.collection.List.of(
                        newItemInfos
                ).flatMap(c -> c)
        );
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onUsage(PlayerInteractEvent event) {
        if (event.useItemInHand() == Event.Result.DENY) return;
        if (event.getAction() == Action.PHYSICAL) return;
        long currentTick = tickerTime.getTick();
        Long lastDropTick = this.lastDropTick.get(event.getPlayer());
        Long lastPlayerClickTick = this.lastPlayerClickTick.get(event.getPlayer());
        if (lastDropTick != null && currentTick == lastDropTick) return;
        if (lastPlayerClickTick != null && currentTick == lastPlayerClickTick) return;
        Option<String> customItemOpt = Option.of(event.getItem())
                .flatMap(NBTCustomItem::getCustomItemId);
        EquipmentSlot equipmentSlot = ReflectedRepresentations.PlayerInteractEvent.getClickedItemSlot(event);
        int currentMainHandSlot = event.getPlayer().getInventory().getHeldItemSlot();
        ItemUsageResultDTO result = itemService.useItem(
                new ItemUsageBlockDTO(
                        event.getPlayer(),
                        event.getAction(),
                        event.getItem(),
                        event.getClickedBlock(),
                        event.getBlockFace(),
                        new SlotPredicate.Input(EquipmentToSlotConverter.convert(equipmentSlot, currentMainHandSlot), equipmentSlot),
                        tickerTime.getTick()
                )
        );
        handleResult(result, event.getItem(), event.getPlayer(), event, true);
    }

    //private ItemMovement getItemMovement(InventoryClickEvent event) {
    //    int currentSlot = event.getSlot();
    //    switch (event.getAction()) {
    //        case MOVE_TO_OTHER_INVENTORY:
    //            if (event.getInventory() instanceof PlayerInventory) {
    //                boolean isHotbarClick = currentSlot >= 0 && currentSlot < 9;
    //                if (isHotbarClick) {
    //                    return findFirstAvailableSlot(event.getCursor(), event.getView().getTopInventory(), currentSlot, 9, 36);
    //                } else {
    //                    return findFirstAvailableSlot(event.getCursor(), event.getView().getTopInventory(), currentSlot, 0, 9);
    //                }
    //            }
    //            break;
    //    }
    //}

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onWorkbench(PrepareItemCraftEvent event) {
        boolean shouldCancel = !Arrays.stream(event.getInventory().getMatrix())
                .filter(Objects::nonNull)
                .allMatch(itemService::canBeUsedInCraft);
        if (shouldCancel) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlayerInventoryDrop(InventoryClickEvent event) {
        if (event.getClick() == ClickType.CREATIVE || event.getClick() == ClickType.DROP) {
            playerTickSlotMap.put((Player) event.getWhoClicked(), Tuple.of(tickerTime.getTick(), event.getSlot()));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        lastDropTick.put(player, tickerTime.getTick());
        ItemStack itemStack = event.getItemDrop().getItemStack();
        int slot;
        Tuple2<Long, Integer> tuple = playerTickSlotMap.get(player);
        if (tuple != null && tuple._1() == tickerTime.getTick()) {
            slot = tuple._2();
        } else slot = player.getInventory().getHeldItemSlot();
        boolean isHeldItem = slot == player.getInventory().getHeldItemSlot();
        handleResult(itemService.dropItem(
                new ItemUsageGeneralDTO(
                        player,
                        itemStack,
                        tickerTime.getTick(),
                        new SlotPredicate.Input(
                                slot,
                                EquipmentToSlotConverter.convert(slot, player).getOrNull()
                        )
                )
        ), itemStack, player, event, false);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onInteractAt(PlayerInteractAtEntityEvent event) {
        long currentTick = tickerTime.getTick();
        Long lastPlayerClickTickValue = this.lastPlayerClickTick.get(event.getPlayer());
        if (lastPlayerClickTickValue != null && currentTick == lastPlayerClickTickValue) return;
        int clickedSlot;
        if ((event.getPlayer().getItemInHand() == null ||
                event.getPlayer().getItemInHand().getType().name().endsWith("AIR")) &&
                FeatureSupport.MODERN_COMBAT
        ) {
            clickedSlot = 40;
        } else clickedSlot = event.getPlayer().getInventory().getHeldItemSlot();
        lastPlayerClickTick.put(event.getPlayer(), tickerTime.getTick());
        ItemUsageResultDTO result = itemService.useItemAt(
                new ItemUsageEntityDTO(
                        event.getPlayer(),
                        true,
                        event.getRightClicked(),
                        event.getPlayer().getItemInHand(),
                        new SlotPredicate.Input(clickedSlot, EquipmentToSlotConverter.convert(clickedSlot, event.getPlayer()).getOrNull()),
                        tickerTime.getTick()
                )
        );
        handleResult(result, event.getPlayer().getItemInHand(), event.getPlayer(), event, false);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        boolean isCancelled = event.isCancelled();
        ItemUsageResultDTO result = itemService.useItemAt(
                new ItemUsageEntityDTO(
                        player,
                        false,
                        event.getEntity(),
                        player.getItemInHand(),
                        new SlotPredicate.Input(player.getInventory().getHeldItemSlot(), EquipmentSlot.HAND),
                        tickerTime.getTick()
                )
        );
        handleResult(result, player.getItemInHand(), player, event, false);
        if (isCancelled) {
            event.setCancelled(true);
        }
    }

    private void handleResult(ItemUsageResultDTO result, ItemStack item, Player player, Cancellable event, boolean omitEventCancellation) {
        if (!event.isCancelled()) {
            event.setCancelled(result.isShouldCancel() || (event.isCancelled() && !omitEventCancellation));
        }
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onInventoryDrag(InventoryDragEvent event) {
        boolean canBePutInvInventory = event.getNewItems().values().stream()
                .anyMatch(itemService::canBePutInInventory);
        if (canBePutInvInventory) return;
        boolean shouldCancel = event.getNewItems().keySet().stream()
                .anyMatch(slot -> slot < event.getView().getTopInventory().getSize());
        if (shouldCancel) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(itemService::shouldRemoveOnDeath);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPickup(PlayerPickupItemEvent event) {
        if (event.getRemaining() != 0) return;
        if (!event.getItem().getItemStack().hasItemMeta()) return;
        itemService.updateItem(event.getItem().getItemStack(), event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void replaceItemOnDrop(PlayerDropItemEvent event) {
        if (!event.getItemDrop().getItemStack().hasItemMeta()) return;
        itemService.updateItem(event.getItemDrop().getItemStack(), null);
        event.getItemDrop().setItemStack(event.getItemDrop().getItemStack());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onInventoryClick(InventoryClickEvent event) {
        ItemStack carriedItem = event.getCursor();
        ItemStack clickedItem = event.getCurrentItem();
        if (!itemService.canBeMoved(clickedItem)) {
            event.setCancelled(true);
            return;
        }
        ItemStack clickedWithItem;
        Player player = (Player) event.getWhoClicked();
        boolean isSwap = Arrays.asList(
                InventoryAction.HOTBAR_SWAP,
                InventoryAction.HOTBAR_MOVE_AND_READD
        ).contains(event.getAction());
        if (isSwap) {
            int button = event.getHotbarButton();
            if (button >= 0) {
                clickedWithItem = player.getInventory().getItem(button);
            } else {
                clickedWithItem = player.getInventory().getItem(40);
            }
        } else clickedWithItem = null;
        Set<ItemStack> forbiddenForInventorySwap = new HashSet<>();
        if (!itemService.canBePutInInventory(clickedWithItem)) forbiddenForInventorySwap.add(clickedWithItem);
        if (!itemService.canBePutInInventory(carriedItem)) forbiddenForInventorySwap.add(carriedItem);
        if (!itemService.canBePutInInventory(clickedItem)) forbiddenForInventorySwap.add(clickedItem);
        if (event.getView().getTopInventory() instanceof PlayerInventory) return;
        boolean topInventoryIsPlayers;
        if (event.getView().getTopInventory() instanceof CraftingInventory) {
            CraftingInventory craftingInventory = (CraftingInventory) event.getView().getTopInventory();
            topInventoryIsPlayers = craftingInventory.getMatrix().length == 4;
        } else topInventoryIsPlayers = false;
        boolean isPlayerInventory = event.getClickedInventory() instanceof PlayerInventory;
        if (forbiddenForInventorySwap.isEmpty()) return;
        if (forbiddenForInventorySwap.contains(clickedItem)) {
            if (event.getClick().isShiftClick() && isPlayerInventory && !topInventoryIsPlayers) {
                event.setCancelled(true);
                return;
            }
        }
        if (forbiddenForInventorySwap.contains(clickedWithItem) || forbiddenForInventorySwap.contains(carriedItem)) {
            if (!isPlayerInventory) {
                event.setCancelled(true);
            }
        }
    }

    private static class DummyEvent implements Cancellable {

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public void setCancelled(boolean b) {

        }
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    private static class ItemMovement {
        int prevSlot;
        int prevSlotNewAmount;
        io.vavr.collection.List<NewItemInfo> newItemInfos;

        @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
        @RequiredArgsConstructor
        @Getter
        private static class NewItemInfo {
            int newSlot;
            int newSlotNewAmount;
            boolean existedBefore;
        }
    }

}
