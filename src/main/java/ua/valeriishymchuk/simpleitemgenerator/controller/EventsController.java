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
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.*;
import org.bukkit.material.Openable;
import ua.valeriishymchuk.simpleitemgenerator.common.block.BlockDataWrapper;
import ua.valeriishymchuk.simpleitemgenerator.common.debug.PipelineDebug;
import ua.valeriishymchuk.simpleitemgenerator.common.item.ItemCopy;
import ua.valeriishymchuk.simpleitemgenerator.common.item.NBTCustomItem;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.reflection.ReflectedRepresentations;
import ua.valeriishymchuk.simpleitemgenerator.common.scheduler.BukkitTaskScheduler;
import ua.valeriishymchuk.simpleitemgenerator.common.slot.EquipmentToSlotConverter;
import ua.valeriishymchuk.simpleitemgenerator.common.tick.TickTimer;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.SlotPredicate;
import ua.valeriishymchuk.simpleitemgenerator.common.version.FeatureSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.version.SigFeatureTag;
import ua.valeriishymchuk.simpleitemgenerator.dto.*;
import ua.valeriishymchuk.simpleitemgenerator.entity.UsageEntity;
import ua.valeriishymchuk.simpleitemgenerator.service.InfoService;
import ua.valeriishymchuk.simpleitemgenerator.service.ItemService;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static ua.valeriishymchuk.simpleitemgenerator.common.item.ItemCopy.isAir;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventsController implements Listener {
    private static final Logger LOGGER = Logger.getLogger("SIG-" + EventsController.class.getSimpleName());

    private static final io.vavr.collection.Map<Integer, Set<Material>> FIT_MATERIALS = io.vavr.collection.HashMap.of(
            40, Arrays.stream(Material.values())
                    .filter(m -> m.name().equals("SHIELD"))
                    .collect(Collectors.toSet()),
            39, Arrays.stream(Material.values())
                    .filter(m -> EnchantmentTarget.ARMOR_HEAD.includes(m) ||
                            (FeatureSupport.NAMESPACED_KEYS_SUPPORT && m.name().equals("CARVED_PUMPKIN")) ||
                            (!FeatureSupport.NAMESPACED_KEYS_SUPPORT && m.name().equals("PUMPKIN")) ||
                            m.name().equals("SKULL_ITEM") || m.name().endsWith("HEAD")
                    ).collect(Collectors.toSet()),
            38, Arrays.stream(Material.values())
                    .filter(EnchantmentTarget.ARMOR_TORSO::includes).collect(Collectors.toSet()),
            37, Arrays.stream(Material.values())
                    .filter(EnchantmentTarget.ARMOR_LEGS::includes).collect(Collectors.toSet()),
            36, Arrays.stream(Material.values())
                    .filter(EnchantmentTarget.ARMOR_FEET::includes).collect(Collectors.toSet())
    );
    //private static final boolean DEBUG = false;
    private static final Set<Class<?>> DEBUG_EVENTS_TO_INCLUDE = new HashSet<>(Arrays.asList(
            InventoryClickEvent.class,
            InventoryDragEvent.class,
            PlayerDeathEvent.class,
            PlayerJoinEvent.class,
            PrepareItemCraftEvent.class,
            PlayerInteractEvent.class,
            PlayerDropItemEvent.class,
            PlayerInteractAtEntityEvent.class,
            EntityDamageByEntityEvent.class,
            PlayerPickupItemEvent.class
    ));

    ItemService itemService;
    InfoService infoService;
    TickTimer tickerTime;
    BukkitTaskScheduler scheduler;
    Map<Player, Long> lastDropTick = new WeakHashMap<>();
    Map<Player, Long> lastPlayerClickTick = new WeakHashMap<>();
    Map<Player, Tuple2<Long, Integer>> playerTickSlotMap = new WeakHashMap<>();

    public EventsController(ItemService itemService, InfoService infoService, TickTimer tickerTime, BukkitTaskScheduler scheduler) {
        this.itemService = itemService;
        this.infoService = infoService;
        this.tickerTime = tickerTime;
        this.scheduler = scheduler;
    }

    private boolean checkDebugExclusion(Event event) {
        return infoService.isDebug() && !DEBUG_EVENTS_TO_INCLUDE.contains(event.getClass());
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        if (checkDebugExclusion(event)) return;
        scheduler.runTaskLater(() -> {
            infoService.getMessage(event.getPlayer()).peek(msg -> KyoriHelper.sendMessage(event.getPlayer(), msg));
            infoService.getNewUpdateMessage(event.getPlayer())
                    .thenAccept(msgOpt -> {
                                msgOpt.peek(msg -> KyoriHelper.sendMessage(event.getPlayer(), msg));
                            }
                    ).exceptionally(e -> {
                        LOGGER.log(
                                Level.SEVERE,
                                "Please report this error there:" +
                                        " https://github.com/ValeraShimchuck/SimpleItemGenerator/issues\n",
                                e
                        );
                        return null;
                    });
        }, 40L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().replace("/", "");
        if (command.startsWith("sig") || command.startsWith("simpleitemgenerator")) {
            infoService.updatePluginActivity();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onServerCommand(ServerCommandEvent event) {
        String command = event.getCommand().replace("/", "");
        if (command.startsWith("sig") || command.startsWith("simpleitemgenerator")) {
            infoService.updatePluginActivity();
        }
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

    private boolean isInteractableBlock(Block block) {
        Supplier<Boolean> legacyDoorCheck = () -> block.getState().getData() instanceof Openable;
        Supplier<Boolean> legacySwitchableCheck = () -> block.getType().name().contains("BUTTON")
                || block.getType().name().contains("LEVER");
        BlockDataWrapper blockData = new BlockDataWrapper(block);
        boolean checkDoor = blockData.isInstanceOf("org.bukkit.block.data.Openable")
                .getOrElse(legacyDoorCheck) && !block.getType().name().contains("IRON");
        boolean checkSwitchable = blockData.isInstanceOf("org.bukkit.block.data.type.Switch")
                .getOrElse(legacySwitchableCheck);
        return block.getState() instanceof InventoryHolder
                || checkDoor
                || checkSwitchable;
    }

    @EventHandler
    private void onArmorRMBClickEvent(PlayerInteractEvent event) {
        if (!infoService.getFeatures().contains(SigFeatureTag.ENHANCED_SLOT_PREDICATE)) return;
        if (!event.getAction().name().startsWith("RIGHT")) return;
        Block block = event.getClickedBlock();
        if (block != null ) {
            System.out.println(block.getState().getData() + " " + block.getType());
            System.out.println("with block: " + event.useInteractedBlock() + " " + event.useItemInHand() + " " + isInteractableBlock(block));
        } else {
            System.out.println("without block: " + event.useInteractedBlock() + " " + event.useItemInHand());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onItemClick(InventoryClickEvent event) {
        if (!infoService.getFeatures().contains(SigFeatureTag.ENHANCED_SLOT_PREDICATE)) return;
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
                            new SlotPredicate.Input(
                                    event.getSlot(),
                                    EquipmentToSlotConverter.convert(event.getSlot(), player).getOrNull(),
                                    false
                            ),
                            tickerTime.getTick(),
                            ItemCopy.from(event.getCurrentItem())
                    ), PipelineDebug.root("Move item From " + event.getSlot(), PipelineDebug.Tag.INVENTORY)

                    ),
                    event.getCurrentItem()
            );
        }



        movedSlots.forEach((slot, item) -> {
            if (item.isSimilar(event.getClickedInventory().getItem(slot))) return;
            results.put(itemService.moveItem(player,new SlotChangeDTO(
                            new SlotPredicate.Input(
                                    slot,
                                    EquipmentToSlotConverter.convert(slot, player).getOrNull(),
                                    true
                            ),
                            tickerTime.getTick(),
                            ItemCopy.from(item)
                    ), PipelineDebug.root("Move item To " + slot, PipelineDebug.Tag.INVENTORY)),
                    item
            );
        });

        if (results.isEmpty()) return;

        boolean isAnyCancelled = results.keySet().stream().anyMatch(ItemUsageResultDTO::isShouldCancel);
        if (isAnyCancelled) {
            new HashSet<>(results.keySet()).stream()
                    .filter(usage -> !usage.isShouldCancel())
                    .forEach(results::remove);
        }

        results.forEach((usage, item) ->
                handleResult(usage, item, player, event, false));
    }



    private void handleMoveToOtherInventory(InventoryClickEvent event, Map<Integer, ItemStack> movedSlots, Player player) {
        Inventory destination = event.getClickedInventory() instanceof PlayerInventory
                ? event.getView().getTopInventory()
                : player.getInventory();
        ItemStack movedItem = event.getCurrentItem().clone();
        if (isAir(movedItem)) return;
        if (Objects.requireNonNull(event.getView().getType()) == InventoryType.CRAFTING) {
            Set<Integer> blackListedSlots = new HashSet<>();
            blackListedSlots.add(36);
            blackListedSlots.add(37);
            blackListedSlots.add(38);
            blackListedSlots.add(39);
            if (FeatureSupport.MODERN_COMBAT) blackListedSlots.add(40);
            List<Integer> slots =new ArrayList<>();
            slots.addAll(blackListedSlots);
            FIT_MATERIALS.filterValues(t -> t.contains(movedItem.getType())).keySet()
                    .forEach(blackListedSlots::remove);
            boolean isHotbar = event.getSlot() < 9;

            if (isHotbar) {
                for (int i = 35; i >= 9; i--) {
                    slots.add(i);
                }
            } else if (blackListedSlots.contains(event.getSlot())) {
                for (int i = 35; i >= 0; i--) {
                    slots.add(i);
                }
            } else {
                for (int i = 0; i < 9; i++) {
                    slots.add(i);
                }
            }
            ShiftClickResult.calculateShiftClick(
                    event.getCurrentItem(),
                    player.getInventory(),
                    slots,
                    blackListedSlots,
                    !blackListedSlots.contains(event.getSlot()),
                    infoService.isDebug()
            ).slotStatuses.forEach((slot, status) -> {
                if (status.wasBefore) return;
                movedItem.setAmount(status.itemsAdded);
                movedSlots.put(slot, movedItem);
            });
        } else {
            List<Integer> slots;
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                slots = io.vavr.collection.List.range(0, event.getClickedInventory().getSize()).asJavaMutable();
            } else {
                slots = new ArrayList<>();
                for (int layer = 0; layer < 4; layer++) {
                    for (int i = 8; i >= 0; i--) {
                        slots.add(layer * 9 + i);
                    }
                }
            }

            ShiftClickResult.calculateShiftClick(
                    event.getCurrentItem(),
                    destination,
                    slots,
                    new HashSet<>(),
                    false,
                    infoService.isDebug()
            ).slotStatuses.forEach((slot, slotStatus) -> {
                if (slotStatus.wasBefore) return;
                movedItem.setAmount(slotStatus.itemsAdded);
                movedSlots.put(slot, movedItem);
            });
        }
        if (infoService.isDebug()) {
            LOGGER.info("Handling movement to the other inventory");
            LOGGER.info("Destination " + destination + " slot: " + event.getSlot() + " \nact: " + event.getAction() +
                    "\nclick: " + event.getClick()
                    + "\nisPlayerInventory: " + (event.getView().getType() == InventoryType.CRAFTING) +
                    "\ntype: " + (event.getView().getType())
                    + "\nmoved-slots: " + movedSlots.entrySet().stream()
                    .map(e -> e.getKey() + " "  + e.getValue().getType())
                    .collect(Collectors.joining("\n")));
        }

    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    private static class ShiftClickResult {
        int remaining;
        Map<Integer, SlotStatus> slotStatuses;

        @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
        @RequiredArgsConstructor
        private static class SlotStatus {
            boolean wasBefore;
            int itemsAdded;
        }

        public static ShiftClickResult calculateShiftClick(
                ItemStack item,
                Inventory destination,
                Iterable<Integer> slotSequence,
                Set<Integer> blacklistedSlots,
                boolean startFromEnd,
                boolean isDebug
        ) {
            int firstEmpty = -1;
            int remaining = item.getAmount();
            if (isDebug)
                LOGGER.info(destination + " " + destination.getSize() + " bl: " + blacklistedSlots);
            Map<Integer, SlotStatus> slotStatusMap = new HashMap<>();
            for (int i : slotSequence) {
                if (isDebug)
                    LOGGER.info("Checking slot: " + i + " " + startFromEnd);
                if (blacklistedSlots.contains(i)) continue;
                if (isDebug)
                    LOGGER.info("pass1");
                ItemStack itemFromInventory = destination.getItem(i);
                if (firstEmpty == -1 && isAir(itemFromInventory)) {
                    firstEmpty = i;
                }
                if (!item.isSimilar(itemFromInventory)) continue;
                int slotMaxStackSize = itemFromInventory.getMaxStackSize();
                int slotAvailable = itemFromInventory.getAmount();
                int available = slotMaxStackSize - slotAvailable;
                if (available <= 0) continue;
                int addedItems = Math.min(remaining, available);
                remaining -= addedItems;
                slotStatusMap.put(i, new SlotStatus(true, addedItems));
                if (remaining <= 0) break;
            }
            if (firstEmpty >= 0 && remaining > 0) {
                slotStatusMap.put(firstEmpty, new SlotStatus(false, remaining));
                remaining = 0;
            }
            return new ShiftClickResult(
                    remaining,
                    slotStatusMap
            );
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
        if (checkDebugExclusion(event)) return;
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
                        new SlotPredicate.Input(EquipmentToSlotConverter.convert(equipmentSlot, currentMainHandSlot), equipmentSlot, true),
                        tickerTime.getTick()
                ),
                PipelineDebug.root("PlayerInteractEvent " + event.getAction())
        );
        handleResult(result, event.getItem(), event.getPlayer(), event, true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onWorkbench(PrepareItemCraftEvent event) {
        if (checkDebugExclusion(event)) return;
        boolean shouldCancel = !Arrays.stream(event.getInventory().getMatrix())
                .filter(Objects::nonNull)
                .allMatch(itemService::canBeUsedInCraft);
        if (shouldCancel) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlayerInventoryDrop(InventoryClickEvent event) {
        if (checkDebugExclusion(event)) return;
        if (event.getClick() == ClickType.CREATIVE || event.getClick() == ClickType.DROP) {
            playerTickSlotMap.put((Player) event.getWhoClicked(), Tuple.of(tickerTime.getTick(), event.getSlot()));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onDrop(PlayerDropItemEvent event) {
        if (checkDebugExclusion(event)) return;
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
                                EquipmentToSlotConverter.convert(slot, player).getOrNull(),
                                true
                        )
                ),
                PipelineDebug.root("PlayerDropItemEvent")
        ), itemStack, player, event, false);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onInteractAt(PlayerInteractAtEntityEvent event) {
        if (checkDebugExclusion(event)) return;
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
                        new SlotPredicate.Input(
                                clickedSlot,
                                EquipmentToSlotConverter.convert(clickedSlot, event.getPlayer()).getOrNull(),
                                true
                        ),
                        tickerTime.getTick()
                ),
                PipelineDebug.root("PlayerInteractAtEntityEvent")
        );
        handleResult(result, event.getPlayer().getItemInHand(), event.getPlayer(), event, false);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onDamage(EntityDamageByEntityEvent event) {
        if (checkDebugExclusion(event)) return;
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        boolean isCancelled = event.isCancelled();
        ItemUsageResultDTO result = itemService.useItemAt(
                new ItemUsageEntityDTO(
                        player,
                        false,
                        event.getEntity(),
                        player.getItemInHand(),
                        new SlotPredicate.Input(
                                player.getInventory().getHeldItemSlot(),
                                EquipmentSlot.HAND,
                                true
                        ),
                        tickerTime.getTick()
                ),
                PipelineDebug.root("EntityDamageByEntityEvent")
        );
        handleResult(result, player.getItemInHand(), player, event, false);
        if (isCancelled) {
            event.setCancelled(true);
        }
    }

    private void handleResult(ItemUsageResultDTO result, ItemStack item, Player player, Cancellable event, boolean omitEventCancellation) {
        if (infoService.isDebug()) {
            result.getPipelineDebug().print();
        }
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
                    if (itemService.areNotEqual(itemCons, clonedItem)) return;
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
                        if (itemService.areNotEqual(itemCons, clonedItem)) return;
                        itemCons.setAmount(0);
                    });
                }
            }

        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onInventoryDrag(InventoryDragEvent event) {
        if (checkDebugExclusion(event)) return;
        boolean canBePutInvInventory = event.getNewItems().values().stream()
                .anyMatch(itemService::canBePutInInventory);
        if (canBePutInvInventory) return;
        boolean shouldCancel = event.getNewItems().keySet().stream()
                .anyMatch(slot -> slot < event.getView().getTopInventory().getSize());
        if (shouldCancel) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onDeath(PlayerDeathEvent event) {
        if (checkDebugExclusion(event)) return;
        event.getDrops().removeIf(itemService::shouldRemoveOnDeath);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPickup(PlayerPickupItemEvent event) {
        if (checkDebugExclusion(event)) return;
        if (event.getRemaining() != 0) return;
        if (!event.getItem().getItemStack().hasItemMeta()) return;
        itemService.updateItem(event.getItem().getItemStack(), event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void replaceItemOnDrop(PlayerDropItemEvent event) {
        if (checkDebugExclusion(event)) return;
        if (!event.getItemDrop().getItemStack().hasItemMeta()) return;
        itemService.updateItem(event.getItemDrop().getItemStack(), null)
                .peek(event.getItemDrop()::setItemStack);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onInventoryClick(InventoryClickEvent event) {
        if (checkDebugExclusion(event)) return;
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
