package ua.valeriishymchuk.simpleitemgenerator.service;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import ua.valeriishymchuk.libs.net.kyori.adventure.text.Component;
import ua.valeriishymchuk.simpleitemgenerator.api.event.SimpleItemGeneratorReloadEvent;
import ua.valeriishymchuk.simpleitemgenerator.common.component.RawComponent;
import ua.valeriishymchuk.simpleitemgenerator.common.cooldown.CooldownType;
import ua.valeriishymchuk.simpleitemgenerator.common.debug.PipelineDebug;
import ua.valeriishymchuk.simpleitemgenerator.common.item.NBTCustomItem;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.raytrace.IRayTraceResult;
import ua.valeriishymchuk.simpleitemgenerator.common.raytrace.RayTraceBlockResult;
import ua.valeriishymchuk.simpleitemgenerator.common.raytrace.RayTraceEntityResult;
import ua.valeriishymchuk.simpleitemgenerator.common.raytrace.RayTraceHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.ClickAt;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.ClickButton;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.PredicateInput;
import ua.valeriishymchuk.simpleitemgenerator.domain.CooldownQueryDomain;
import ua.valeriishymchuk.simpleitemgenerator.dto.*;
import ua.valeriishymchuk.simpleitemgenerator.entity.MainConfigEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.CustomItemEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.LangEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.UsageEntity;
import ua.valeriishymchuk.simpleitemgenerator.repository.IConfigRepository;
import ua.valeriishymchuk.simpleitemgenerator.repository.impl.CooldownRepository;
import ua.valeriishymchuk.simpleitemgenerator.repository.impl.ItemRepository;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ua.valeriishymchuk.simpleitemgenerator.common.placeholders.PlaceholdersHelper.placeholdersFor;
import static ua.valeriishymchuk.simpleitemgenerator.common.placeholders.PlaceholdersHelper.replacePlayer;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ItemService {


    IConfigRepository configRepository;
    ItemRepository itemRepository;
    CooldownRepository cooldownRepository;
    InfoService infoService;

    private MainConfigEntity config() {
        return configRepository.getConfig();
    }

    private LangEntity lang() {
        return configRepository.getLang();
    }

    private int getTotalItems(ItemStack item, Player player) {
        if (item == null || item.getType().name().endsWith("AIR")) return 0;
        String customItemId = NBTCustomItem.getCustomItemId(item).getOrNull();
        if (customItemId == null) return 0;
        CustomItemEntity customItem = itemRepository.getItem(customItemId).getOrNull();
        if (customItem == null) return 0;
        return Arrays.stream(player.getInventory().getContents())
                .filter(Objects::nonNull)
                .filter(i -> NBTCustomItem.getCustomItemId(i)
                        .map(s -> s.equals(customItemId))
                        .getOrElse(false)
                ).mapToInt(ItemStack::getAmount).sum();
    }

    public ItemUsageResultDTO useItem(ItemUsageBlockDTO itemUsageBlockDTO, PipelineDebug pipelineDebug) {
        boolean isBlock = itemUsageBlockDTO.getClickedBlock().isDefined();
        boolean isLeftClick = itemUsageBlockDTO.getAction() == Action.LEFT_CLICK_AIR ||
                itemUsageBlockDTO.getAction() == Action.LEFT_CLICK_BLOCK;
        ItemUsageResultDTO nop = new ItemUsageResultDTO(
                null,
                Collections.emptyList(),
                false,
                UsageEntity.Consume.NONE,
                pipelineDebug
        );
        if (itemUsageBlockDTO.getAction() == Action.PHYSICAL) return nop;
        Map<String, String> placeholders = new HashMap<>();
        if (isBlock)
            placeholders.putAll(placeholdersFor(
                    itemUsageBlockDTO.getClickedBlock().get(),
                    itemUsageBlockDTO.getClickedFace().get())
            );
        PredicateInput predicateInput = new PredicateInput(
                itemUsageBlockDTO.getPlayer(),
                isBlock ? itemUsageBlockDTO.getClickedBlock().get().getLocation() : null,
                isLeftClick ? ClickButton.LEFT : ClickButton.RIGHT,
                isBlock ? ClickAt.BLOCK : ClickAt.AIR,
                new PredicateInput.Amount(
                        getTotalItems(itemUsageBlockDTO.getItem(), itemUsageBlockDTO.getPlayer()),
                        itemUsageBlockDTO.getItem() == null ? 0 : itemUsageBlockDTO.getItem().getAmount()
                ),
                itemUsageBlockDTO.getCurrentTick(),
                itemUsageBlockDTO.getSlot(),
                PredicateInput.Trigger.WORLD_CLICK
        );
        return useItem0(
                itemUsageBlockDTO.getPlayer(),
                itemUsageBlockDTO.getItem(),
                predicateInput,
                placeholders,
                pipelineDebug
        );

    }

    public ItemUsageResultDTO dropItem(ItemUsageGeneralDTO itemUsageGeneralDTO, PipelineDebug pipelineDebug) {
        Player player = itemUsageGeneralDTO.getPlayer();
        ItemStack item = itemUsageGeneralDTO.getItemStack();
        RaytraceResultDomain raytraceResult = raytrace(itemUsageGeneralDTO.getPlayer());
        ItemUsageResultDTO usageResult = useItem0(
                player,
                item,
                new PredicateInput(
                        player,
                        raytraceResult.useLoc,
                        ClickButton.DROP,
                        raytraceResult.clickAt,
                        new PredicateInput.Amount(
                                getTotalItems(item, player),
                                item == null ? 0 : item.getAmount()
                        ),
                        itemUsageGeneralDTO.getCurrentTick(),
                        itemUsageGeneralDTO.getSlot(),
                        PredicateInput.Trigger.DROP_ITEM
                ),
                raytraceResult.placeholders,
                pipelineDebug
        );
        String customItemId = NBTCustomItem.getCustomItemId(item).getOrNull();
        if (usageResult.isShouldCancel() && player.getGameMode() == GameMode.CREATIVE && customItemId != null) {
            usageResult = usageResult.withMessage(lang()
                    .getCreativeDrop()
                    .replaceText("%key%", customItemId)
                    .bakeInternal()
            );
        }
        return usageResult;
    }

    private ItemUsageResultDTO mergeUsages(ItemUsageResultDTO acc, ItemUsageResultDTO usage, boolean isPlainItem, PipelineDebug pipelineDebug) {
        UsageEntity.Consume consume;
        boolean isAccNone = acc.getConsume().isNone();
        boolean anyNone = acc.getConsume().isNone() || usage.getConsume().isNone();
        if (anyNone) {
            consume = isAccNone ? usage.getConsume() : acc.getConsume();
        } else {
            boolean isBothAmount = acc.getConsume().isAmount() && usage.getConsume().isAmount();
            if (isBothAmount) {
                consume = new UsageEntity.Consume(
                        UsageEntity.ConsumeType.AMOUNT,
                        acc.getConsume().getAmount() + usage.getConsume().getAmount()
                );
            } else {
                if (acc.getConsume().getConsumeType() == usage.getConsume().getConsumeType()) {
                    consume = acc.getConsume();
                } else {
                    List<UsageEntity.ConsumeType> typesHierarchy = Arrays.asList(
                            UsageEntity.ConsumeType.STACK,
                            UsageEntity.ConsumeType.ALL
                    );
                    int accIndex = typesHierarchy.indexOf(acc.getConsume().getConsumeType());
                    int eIndex = typesHierarchy.indexOf(usage.getConsume().getConsumeType());
                    if (accIndex > eIndex) {
                        consume = acc.getConsume();
                    } else consume = usage.getConsume();
                }
            }
        }
        return new ItemUsageResultDTO(
                null,
                Stream.of(acc, usage).map(ItemUsageResultDTO::getCommands)
                        .flatMap(List::stream).collect(Collectors.toList()),
                (acc.isShouldCancel() || usage.isShouldCancel()) && !isPlainItem,
                consume,
                pipelineDebug
        );
    }

    private CooldownQueryDomain queryPerItem(ItemStack item, UsageEntity usage, int id) {
        NBTCustomItem.Cooldown cooldown = NBTCustomItem
                .queryCooldown(item, usage.getCooldownMillis(), usage.getCooldownFreezeTimeMillis(), id);
        boolean isOnCooldown = cooldown.isDefault();
        boolean isFreeze = cooldown.isFrozen();
        Long remainingCooldownTime;
        if (isOnCooldown) {
            remainingCooldownTime = cooldown.getRemainingMillis();
        } else remainingCooldownTime = null;
        return new CooldownQueryDomain(isOnCooldown, isFreeze, remainingCooldownTime, null);
    }

    private CooldownQueryDomain queryGlobal(String customItemId, UsageEntity usage, int id) {
        Option<Long> lastUsage = cooldownRepository.lastUsageGlobal(customItemId, id, false);
        boolean isOnCooldown = lastUsage
                .map(l -> l + usage.getCooldownMillis() > System.currentTimeMillis()).getOrElse(false);
        boolean isFreeze;
        Long remainingCooldownTime;
        if (isOnCooldown) {
            isFreeze = cooldownRepository.lastUsageGlobal(customItemId, id, true)
                    .map(l -> l + usage.getCooldownFreezeTimeMillis() > System.currentTimeMillis()).getOrElse(false);
            if (!isFreeze) {
                cooldownRepository.updateGlobal(customItemId, id, true);
            }
            remainingCooldownTime = lastUsage.getOrElse(0L) + usage.getCooldownMillis() - System.currentTimeMillis();
        } else {
            remainingCooldownTime = null;
            isFreeze = false;
            cooldownRepository.updateGlobal(customItemId, id, false);
            cooldownRepository.updateGlobal(customItemId, id, true);
        }
        return new CooldownQueryDomain(isOnCooldown, isFreeze, remainingCooldownTime, lastUsage.getOrNull());
    }

    private CooldownQueryDomain queryPerPlayer(String customItemId, Player player, UsageEntity usage, int id) {
        Option<Long> lastUsage = cooldownRepository.lastUsagePerPlayer(customItemId, player.getUniqueId(), id, false);
        boolean isOnCooldown = lastUsage
                .map(l -> l + usage.getCooldownMillis() > System.currentTimeMillis()).getOrElse(false);
        boolean isFreeze;
        Long remainingCooldownTime;
        if (isOnCooldown) {
            isFreeze = cooldownRepository.lastUsagePerPlayer(customItemId, player.getUniqueId(), id, true)
                    .map(l -> l + usage.getCooldownFreezeTimeMillis() > System.currentTimeMillis()).getOrElse(false);
            if (!isFreeze) {
                cooldownRepository.updatePerPlayer(customItemId, player.getUniqueId(), id, true);
            }
            remainingCooldownTime = lastUsage.getOrElse(0L) + usage.getCooldownMillis() - System.currentTimeMillis();
        } else {
            isFreeze = false;
            cooldownRepository.updatePerPlayer(customItemId, player.getUniqueId(), id, false);
            cooldownRepository.updatePerPlayer(customItemId, player.getUniqueId(), id, true);
            remainingCooldownTime = null;
        }
        return new CooldownQueryDomain(isOnCooldown, isFreeze, remainingCooldownTime, lastUsage.getOrNull());
    }

    private CooldownQueryDomain getAndUpdateCooldown(ItemStack item, UsageEntity usage, Player player) {
        String customItemId = NBTCustomItem.getCustomItemId(item).get();
        CustomItemEntity customItem = itemRepository.getItem(customItemId).get();
        int id = customItem.getUsages().indexOf(usage);
        CooldownType cooldownType = usage.getCooldownType();
        return switch (cooldownType) {
            case PER_ITEM -> queryPerItem(item, usage, id);
            case GLOBAL -> queryGlobal(customItemId, usage, id);
            case PER_PLAYER -> queryPerPlayer(customItemId, player, usage, id);
        };
    }

    private ItemUsageResultDTO applyCooldown(
            UsageEntity usage,
            ItemStack item,
            Player player,
            PredicateInput predicateInput,
            Map<String, String> placeholders,
            PipelineDebug pipelineDebug
    ) {
        PipelineDebug subPipeline = pipelineDebug.append("Cooldown");
        String customItemId = NBTCustomItem.getCustomItemId(item).get();
        CustomItemEntity customItem = itemRepository.getItem(customItemId).get();
        CooldownQueryDomain cooldownQueryDomain = getAndUpdateCooldown(item, usage, player);
        int id = customItem.getUsages().indexOf(usage);
        if (cooldownQueryDomain.isFreeze()) return ItemUsageResultDTO.CANCELLED
                .withPipelineDebug(subPipeline.appendAndReturnSelf("Cancelled by freeze " + id));
        if (cooldownQueryDomain.isOnCooldown()) {
            return usage.prepareCooldownCommands(cooldownQueryDomain, player, placeholders, subPipeline);
        }
        boolean shouldCancel = false;
        boolean isInventoryClick = predicateInput.getTrigger() == PredicateInput.Trigger.INVENTORY_CLICK;
        if (!customItem.isPlainItem() && !isInventoryClick) {
            subPipeline.append("Not plain item and not inventory click. Setting it to " + usage.isCancel());
            shouldCancel = usage.isCancel();
        } else if (usage.isCancel() /* && !customItem.isPlainItem() */) {
            subPipeline.append("Usage is cancelled");
            shouldCancel = true;
        }
        subPipeline.appendAndReturnSelf("Cooldown, is cancelled " + shouldCancel + " id: "  + id);
        return ItemUsageResultDTO.CANCELLED
                .withCommands(usage.prepareCommands(player, placeholders))
                .withShouldCancel(shouldCancel)
                .withConsume(usage.getConsume())
                .withPipelineDebug(pipelineDebug);
    }

    private ItemUsageResultDTO useItem0(
            Player player,
            ItemStack item,
            PredicateInput predicateInput,
            Map<String, String> placeholders,
            PipelineDebug pipelineDebug
    ) {
        ItemUsageResultDTO nop = ItemUsageResultDTO.EMPTY
                .withPipelineDebug(pipelineDebug);
        if (!NBTCustomItem.hasCustomItemId(item)) return nop;
        String customItemId = NBTCustomItem.getCustomItemId(item).getOrNull();
        if (customItemId == null) return nop;
        PipelineDebug prependedDebug = PipelineDebug.prepend(pipelineDebug, customItemId);
        CustomItemEntity customItem = itemRepository.getItem(customItemId).getOrNull();
        if (customItem == null) {
            prependedDebug.append("Can't find custom item in config");
            boolean isItemUsage = predicateInput.getTrigger() != PredicateInput.Trigger.TICK
                    && predicateInput.getTrigger() != PredicateInput.Trigger.INVENTORY_CLICK;
            boolean shouldSendMessage = isItemUsage && config().isSendInvalidItemMessage();
            Component message = lang().getInvalidItem().replaceText("%key%", customItemId).bakeInternal();
            return ItemUsageResultDTO.EMPTY
                    .withMessage(shouldSendMessage ? message : null)
                    .withShouldCancel(true)
                    .withPipelineDebug(prependedDebug);
        }
        infoService.updatePluginActivity();
        List<UsageEntity> usages = customItem.getUsages().stream()
                .filter(usageFilter -> {
                    UsageEntity.AcceptResult result = usageFilter.accepts(predicateInput);
                    prependedDebug.append(
                                    "Usage result (" + result.isAccepted() + ") - "
                                            + customItem.getUsages().indexOf(usageFilter)
                            )
                            .appendAllAndReturnSelf(result.getPipelineDebugs());
                    return result.isAccepted();
                })
                .toList();
        return usages.stream().
                map(usage -> applyCooldown(usage, item, player, predicateInput, placeholders, prependedDebug))
                .reduce((acc, e) -> mergeUsages(acc, e, customItem.isPlainItem(), prependedDebug))
                .orElseGet(() -> ItemUsageResultDTO.EMPTY
                        .withShouldCancel(!customItem.isPlainItem())
                        .withPipelineDebug(prependedDebug.appendAndReturnSelf("Usages weren't found")));
    }

    private CommandExecutionDTO prepare(UsageEntity.Command command, Player player, Map<String, String> placeholders) {
        String rawCommand = replacePlayer(command.getCommand(), player);
        AtomicReference<String> strAtomic = new AtomicReference<>(rawCommand);
        placeholders.forEach((placeholder, value) -> strAtomic.set(strAtomic.get().replace(placeholder, value)));
        return new CommandExecutionDTO(command.isExecuteAsConsole(), strAtomic.get());
    }

    public ItemUsageResultDTO useItemAt(ItemUsageEntityDTO itemUsageEntityDTO, PipelineDebug pipelineDebug) {
        Entity clicked = itemUsageEntityDTO.getClicked();
        ItemStack item = itemUsageEntityDTO.getItem();
        Player player = itemUsageEntityDTO.getPlayer();
        boolean isRightClicked = itemUsageEntityDTO.isRightClicked();
        boolean isPlayer = clicked instanceof Player;
        PredicateInput clickType = new PredicateInput(
                player,
                clicked.getLocation(),
                isRightClicked ? ClickButton.RIGHT : ClickButton.LEFT,
                isPlayer ? ClickAt.PLAYER : ClickAt.ENTITY,
                new PredicateInput.Amount(
                        getTotalItems(item, player),
                        item == null ? 0 : item.getAmount()
                ),
                itemUsageEntityDTO.getCurrentTick(),
                itemUsageEntityDTO.getSlot(),
                PredicateInput.Trigger.ENTITY_CLICK
        );
        Map<String, String> placeholders = isPlayer ? placeholdersFor((Player) clicked) : placeholdersFor(clicked);
        return useItem0(player, item, clickType, placeholders, pipelineDebug);
    }

    private RaytraceResultDomain raytrace(Player player) {
        int playerReach = 3;
        IRayTraceResult result = RayTraceHelper.rayTrace(
                player,
                Arrays.stream(Material.values()).filter(material -> material.isBlock() && (
                                material.name().endsWith("AIR") ||
                                        material.name().endsWith("WATER") ||
                                        material.name().endsWith("LAVA"))
                        )
                        .collect(Collectors.toSet()),
                playerReach,
                playerReach + 1
        );
        Map<String, String> placeholders = new HashMap<>();
        ClickAt clickAt;
        Location useLoc = null;
        if (result.hitBlock()) {
            RayTraceBlockResult castesResult = (RayTraceBlockResult) result;
            clickAt = ClickAt.BLOCK;
            useLoc = castesResult.getHitBlock().getLocation();
            placeholders.putAll(placeholdersFor(castesResult.getHitBlock(), castesResult.getSide()));
        } else if (result.hitEntity()) {
            RayTraceEntityResult castesResult = (RayTraceEntityResult) result;
            useLoc = castesResult.getEntity().getLocation();
            if (castesResult.getEntity() instanceof Player) {
                placeholders.putAll(placeholdersFor((Player) castesResult.getEntity()));
                clickAt = ClickAt.PLAYER;
            } else {
                placeholders.putAll(placeholdersFor(castesResult.getEntity()));
                clickAt = ClickAt.ENTITY;
            }
        } else clickAt = ClickAt.AIR;
        return new RaytraceResultDomain(
                clickAt,
                useLoc,
                placeholders
        );
    }

    public ItemUsageResultDTO tickItem(ItemUsageGeneralDTO itemUsageGeneralDTO, PipelineDebug pipelineDebug) {
        Player player = itemUsageGeneralDTO.getPlayer();
        ItemStack item = itemUsageGeneralDTO.getItemStack();
        ItemUsageResultDTO nop = ItemUsageResultDTO.EMPTY
                .withPipelineDebug(pipelineDebug);
        if (item == null || !NBTCustomItem.hasCustomItemId(item)) return nop;
        String customItemId = NBTCustomItem.getCustomItemId(item).getOrNull();
        if (customItemId == null) return nop;
        PipelineDebug prependedDebug = PipelineDebug.prepend(pipelineDebug, customItemId);
        CustomItemEntity customItem = itemRepository.getItem(customItemId).getOrNull();
        if (customItem == null) {
            prependedDebug.append("Can't find custom item in config[tick]");
            return ItemUsageResultDTO.EMPTY
                    .withShouldCancel(true)
                    .withPipelineDebug(prependedDebug);
        }
        if (!customItem.hasTick()) return nop;
        RaytraceResultDomain raytraceResultDomain = raytrace(player);
        return useItem0(
                player,
                item,
                new PredicateInput(
                        player,
                        raytraceResultDomain.useLoc,
                        null,
                        raytraceResultDomain.clickAt,
                        new PredicateInput.Amount(
                                getTotalItems(item, player),
                                item.getAmount()
                        ),
                        itemUsageGeneralDTO.getCurrentTick(),
                        itemUsageGeneralDTO.getSlot(),
                        PredicateInput.Trigger.TICK
                ),
                raytraceResultDomain.placeholders,
                pipelineDebug
        );
    }

    public ItemUsageResultDTO moveItem(Player player, SlotChangeDTO slotChanges, PipelineDebug pipelineDebug) {
        ItemStack item = slotChanges.getItemStack().getRealItem();
        RaytraceResultDomain raytraceResultDomain = raytrace(player);
        return useItem0(
                player,
                item,
                new PredicateInput(
                        player,
                        raytraceResultDomain.useLoc,
                        null,
                        raytraceResultDomain.clickAt,
                        new PredicateInput.Amount(
                                getTotalItems(item, player),
                                item == null ? 0 : item.getAmount()
                        ),
                        slotChanges.getTick(),
                        slotChanges.getSlot(),
                        PredicateInput.Trigger.INVENTORY_CLICK
                ),
                raytraceResultDomain.placeholders,
                pipelineDebug
        );
    }

    public boolean canBePutInInventory(ItemStack item) {
        if (item == null || !NBTCustomItem.hasCustomItemId(item)) return true;
        String customItemId = NBTCustomItem.getCustomItemId(item).getOrNull();
        if (customItemId == null) return true;
        CustomItemEntity customItem = itemRepository.getItem(customItemId).getOrNull();
        if (customItem == null) return true;
        return customItem.canBePutInInventory();
    }

    public boolean canBeMoved(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().name().endsWith("AIR")) return true;
        String customItemId = NBTCustomItem.getCustomItemId(itemStack).getOrNull();
        if (customItemId == null) return true;
        CustomItemEntity customItem = itemRepository.getItem(customItemId).getOrNull();
        if (customItem == null) return true;
        return customItem.canMove();
    }

    public boolean shouldRemoveOnDeath(ItemStack item) {
        if (item == null || item.getType().name().endsWith("AIR")) return false;
        String customItemId = NBTCustomItem.getCustomItemId(item).getOrNull();
        if (customItemId == null) return false;
        CustomItemEntity customItem = itemRepository.getItem(customItemId).getOrNull();
        if (customItem == null) return false;
        return customItem.removeOnDeath();
    }

    public boolean areNotEqual(ItemStack item, ItemStack item2) {
        if (item == null || item2 == null) return true;
        String customItemId1 = NBTCustomItem.getCustomItemId(item).getOrNull();
        String customItemId2 = NBTCustomItem.getCustomItemId(item2).getOrNull();
        if (customItemId1 == null || customItemId2 == null) return true;
        return !Objects.equals(customItemId1, customItemId2);
    }

    public Option<ItemStack> updateItem(ItemStack itemStack, @Nullable Player player) {
        boolean itemHasChanged = itemRepository.updateItem(itemStack, player);
        if (itemHasChanged) return Option.some(itemStack);
        return Option.none();
    }

    public boolean canBeUsedInCraft(ItemStack item) {
        if (item == null || !NBTCustomItem.hasCustomItemId(item)) return true;
        String customItemId = NBTCustomItem.getCustomItemId(item).getOrNull();
        if (customItemId == null) return true;
        CustomItemEntity customItem = itemRepository.getItem(customItemId).getOrNull();
        if (customItem == null) return true;
        return customItem.isIngredient();
    }

    public GiveItemDTO giveItem(String key, @Nullable Player player) {
        return giveItem(key, player, null);
    }

    public GiveItemDTO giveItem(String key, @Nullable Player player, Integer slot) {
        if (player == null) return new GiveItemDTO(
                lang().getSenderNotPlayer().bakeInternal(),
                lang().getDroppedItem().bakeInternal(),
                null
        );
        if (slot != null && player.getInventory().getSize() <= slot) return new GiveItemDTO(
                lang().getSlotNotExist().replaceText("%slot%", slot + "").bakeInternal(),
                lang().getDroppedItem().bakeInternal(),
                null
        );
        ItemStack itemStack = itemRepository.bakeItem(key, player).getOrNull();
        RawComponent message = itemStack == null ? lang().getItemDoesntExist() : lang().getGiveItemSuccessfully();
        message = message.replaceText("%player%", player.getName())
                .replaceText("%key%", key);
        return new GiveItemDTO(
                message.bakeInternal(),
                lang().getDroppedItem().bakeInternal(),
                itemStack
        );
    }

    public WithdrawItemDTO withdrawItem(String key, @Nullable Player player, int amount) {
        if (player == null) return new WithdrawItemDTO(
                lang().getSenderNotPlayer().bakeInternal(),
                null,
                false
        );
        CustomItemEntity customItem = itemRepository.getItem(key).getOrNull();
        if (customItem == null) return new WithdrawItemDTO(
                lang().getItemDoesntExist().replaceText("%key%", key).bakeInternal(),
                null,
                false
        );
        int totalItems = Arrays.stream(player.getInventory().getContents())
                .filter(item -> NBTCustomItem.getCustomItemId(item).map(s -> s.equals(key))
                        .getOrElse(false))
                .filter(Objects::nonNull)
                .mapToInt(ItemStack::getAmount)
                .sum();
        List<RawComponent> messages = new ArrayList<>();
        boolean success;
        if (totalItems < amount) {
            messages.add(lang().getNotEnoughItemsSender());
            messages.add(lang().getNotEnoughItemsReceiver());
            success = false;
        } else {
            messages.add(lang().getSuccessfullyWithdrewSender());
            messages.add(lang().getSuccessfullyWithdrewReceiver());
            success = true;
        }
        Component itemName = Option.of(customItem.getItemStack().getItemMeta().displayName()).map(KyoriHelper::convert)
                .getOrElse(KyoriHelper.parseMiniMessage("<white>" + key + "</white>"));
        List<Component> finalMessages = messages.stream().map(raw -> {
            return raw.replaceText("%amount%", amount + "")
                    .replaceText("%item%", itemName)
                    .replaceText("%player%", player.getName())
                    .replaceText("%key%", key)
                    .bakeInternal();
        }).toList();
        return new WithdrawItemDTO(
                finalMessages.get(0),
                finalMessages.get(1),
                success
        );
    }

    public Set<String> getItemKeys() {
        return itemRepository.getItemKeys();
    }

    public long getPlaceholderUpdatePeriodTicks() {
        return config().getPlaceholderUpdatePeriod() / 50;
    }

    public long getItemUpdatePeriodTicks() {
        return config().getItemUpdatePeriod() / 50;
    }


    public Component reload() {
        boolean result = cooldownRepository.reload() && configRepository.reload() && itemRepository.reloadItems();
        if (result) Bukkit.getPluginManager().callEvent(new SimpleItemGeneratorReloadEvent());
        return result ? lang().getReloadSuccessfully().bakeInternal() : lang().getReloadUnsuccessfully().bakeInternal();
    }

    public Component playerNotFound(String input) {
        return lang().getInvalidPlayer().replaceText("%player%", input).bakeInternal();
    }

    public void cooldownAutoSave() {
        cooldownRepository.save();
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    private static class RaytraceResultDomain {
        ClickAt clickAt;
        Location useLoc;
        Map<String, String> placeholders;
    }
}
