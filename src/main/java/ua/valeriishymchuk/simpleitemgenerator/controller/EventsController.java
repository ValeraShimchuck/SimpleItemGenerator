package ua.valeriishymchuk.simpleitemgenerator.controller;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import ua.valeriishymchuk.simpleitemgenerator.common.item.NBTCustomItem;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.scheduler.BukkitTaskScheduler;
import ua.valeriishymchuk.simpleitemgenerator.common.tick.TickerTime;
import ua.valeriishymchuk.simpleitemgenerator.dto.ItemUsageResultDTO;
import ua.valeriishymchuk.simpleitemgenerator.entity.UsageEntity;
import ua.valeriishymchuk.simpleitemgenerator.service.IInfoService;
import ua.valeriishymchuk.simpleitemgenerator.service.IItemService;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class EventsController implements Listener {

    IItemService itemService;
    IInfoService infoService;
    TickerTime tickerTime;
    BukkitTaskScheduler scheduler;
    Map<Player, Long> lastDropTick = new WeakHashMap<>();
    Map<Player, Long> lastPlayerClickTick = new WeakHashMap<>();
    Map<Player, Map<Integer, Long>> lastUsedItemTicks = new WeakHashMap<>();

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

    @EventHandler(priority = EventPriority.HIGH)
    private void onUsage(PlayerInteractEvent event) {
        if (event.useItemInHand() == Event.Result.DENY) return;
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
        ItemUsageResultDTO result = itemService.useItem(
                event.getPlayer(),
                event.getAction(),
                event.getItem(),
                event.getClickedBlock(),
                event.getBlockFace()
        );
        handleResult(result, event.getItem(), event.getPlayer(), event);
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        lastDropTick.put(player, tickerTime.getTick());
        handleResult(itemService.dropItem(player, event.getItemDrop().getItemStack()), event.getItemDrop().getItemStack(), player, event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onInteractAt(PlayerInteractAtEntityEvent event) {
        long currentTick = tickerTime.getTick();
        Long lastPlayerClickTickValue = this.lastPlayerClickTick.get(event.getPlayer());
        if (lastPlayerClickTickValue != null && currentTick == lastPlayerClickTickValue) return;
        lastPlayerClickTick.put(event.getPlayer(), tickerTime.getTick());
        ItemUsageResultDTO result = itemService.useItemAt(
                event.getPlayer(),
                true,
                event.getRightClicked(),
                event.getPlayer().getItemInHand()
        );
        handleResult(result, event.getPlayer().getItemInHand(), event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        boolean isCancelled = event.isCancelled();
        ItemUsageResultDTO result = itemService.useItemAt(player, false, event.getEntity(), player.getItemInHand());
        handleResult(result, player.getItemInHand(), player, event);
        if (isCancelled) {
            event.setCancelled(true);
        }
    }

    private void handleResult(ItemUsageResultDTO result, ItemStack item, Player player, Cancellable event) {
        event.setCancelled(result.isShouldCancel());
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
