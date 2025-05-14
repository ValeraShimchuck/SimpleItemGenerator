package ua.valeriishymchuk.simpleitemgenerator.controller;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
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


    private static boolean isAir(ItemStack item) {
        return item == null || item.getType().name().endsWith("AIR");
    }


    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onItemClick(InventoryClickEvent event) {
        ItemCopy[] inventorySnapshot = getInventorySnapshot(event.getView());
        int offSlotsPointer = event.getView().countSlots();
        scheduler.runTask(() -> {
            ItemCopy[] newInventorySnapshot = getInventorySnapshot(event.getView());
            List<SlotChangeDTO> removals = new ArrayList<>();
            List<SlotChangeDTO> additions = new ArrayList<>();
            for (int i = 0; i < inventorySnapshot.length; i++) {
                ItemCopy item = inventorySnapshot[i];
                ItemCopy newStack = newInventorySnapshot[i];
                if (Objects.equals(item, newStack)) continue;
                if (item != null && newStack != null && item.getClone().isSimilar(newStack.getClone())) {
                    continue; // no change
                }
                if (newStack != null) {
                    additions.add(new SlotChangeDTO(true, i, newStack));
                }
                if (item != null) {
                    removals.add(new SlotChangeDTO(false, i, item));
                }
            }
            List<SlotChangeDTO> changes = new ArrayList<>(removals);
            changes.addAll(additions);
            changes.forEach(change -> {
                ItemUsageResultDTO result = itemService.moveItem(change, event.getWhoClicked().getInventory().getHeldItemSlot());
                handleResult(
                        result,
                        change.getItemStack().getRealItem(),
                        (Player) event.getWhoClicked(),
                        new DummyEvent(),
                        false
                );
            });
        });

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

    private ItemMovement findFirstAvailableSlot(ItemStack item, Inventory inventory,int prevSlot, int startSlot, int endSlot) {
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
            } else if(!existingItem.isSimilar(item)) {
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
        if ( event.getAction() == Action.PHYSICAL) return;
        long currentTick = tickerTime.getTick();
        Long lastDropTick = this.lastDropTick.get(event.getPlayer());
        Long lastPlayerClickTick = this.lastPlayerClickTick.get(event.getPlayer());
        if (lastDropTick != null && currentTick == lastDropTick) return;
        if (lastPlayerClickTick != null && currentTick == lastPlayerClickTick) return;
        Option<String> customItemOpt = Option.of(event.getItem())
                .flatMap(NBTCustomItem::getCustomItemId);
        if (customItemOpt.isDefined()) {
            Map<Integer, Long> map = lastUsedItemTicks.computeIfAbsent(event.getPlayer(), p -> new HashMap<>());
            Long lastUsedItemTick = map.get(event.getItem().hashCode());
            long ticksSinceLastUse = currentTick - (lastUsedItemTick != null ? lastUsedItemTick : 0);
            boolean shouldCancel = ticksSinceLastUse == 0 || (event.getAction() == Action.LEFT_CLICK_BLOCK && ticksSinceLastUse < 2);
            if (lastUsedItemTick != null && shouldCancel) {
                event.setCancelled(true);
                return;
            }
            map.put(event.getItem().hashCode(), currentTick);
        }
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

}
