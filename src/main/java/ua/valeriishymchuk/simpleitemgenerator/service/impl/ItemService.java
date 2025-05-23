package ua.valeriishymchuk.simpleitemgenerator.service.impl;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;
import ua.valeriishymchuk.simpleitemgenerator.common.component.RawComponent;
import ua.valeriishymchuk.simpleitemgenerator.common.cooldown.CooldownType;
import ua.valeriishymchuk.simpleitemgenerator.common.item.NBTCustomItem;
import ua.valeriishymchuk.simpleitemgenerator.common.raytrace.IRayTraceResult;
import ua.valeriishymchuk.simpleitemgenerator.common.raytrace.RayTraceBlockResult;
import ua.valeriishymchuk.simpleitemgenerator.common.raytrace.RayTraceEntityResult;
import ua.valeriishymchuk.simpleitemgenerator.common.raytrace.RayTraceHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.reflection.ReflectedRepresentations;
import ua.valeriishymchuk.simpleitemgenerator.common.regex.RegexUtils;
import ua.valeriishymchuk.simpleitemgenerator.common.support.PapiSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.ClickAt;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.ClickButton;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.PredicateInput;
import ua.valeriishymchuk.simpleitemgenerator.dto.*;
import ua.valeriishymchuk.simpleitemgenerator.entity.MainConfigEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.CustomItemEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.LangEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.UsageEntity;
import ua.valeriishymchuk.simpleitemgenerator.repository.IConfigRepository;
import ua.valeriishymchuk.simpleitemgenerator.repository.ICooldownRepository;
import ua.valeriishymchuk.simpleitemgenerator.repository.impl.ItemRepository;
import ua.valeriishymchuk.simpleitemgenerator.service.IItemService;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ItemService implements IItemService {

    private static final Pattern TIME_PATTERN = Pattern.compile("%time_(?<timeunit>[a-z])(\\.(?<precision>\\d+)f)?%");

    IConfigRepository configRepository;
    ItemRepository itemRepository;
    ICooldownRepository cooldownRepository;

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

    @Override
    public ItemUsageResultDTO useItem(ItemUsageBlockDTO itemUsageBlockDTO) {
        boolean isBlock = itemUsageBlockDTO.getClickedBlock().isDefined();
        boolean isLeftClick = itemUsageBlockDTO.getAction() == Action.LEFT_CLICK_AIR ||
                itemUsageBlockDTO.getAction() == Action.LEFT_CLICK_BLOCK;
        ItemUsageResultDTO nop = new ItemUsageResultDTO(
                null,
                Collections.emptyList(),
                false,
                UsageEntity.Consume.NONE
        );
        if (itemUsageBlockDTO.getAction() == Action.PHYSICAL) return nop;
        Map<String, String> placeholders = new HashMap<>();
        if (isBlock) placeholders.putAll(placeholdersFor(itemUsageBlockDTO.getClickedBlock().get(), itemUsageBlockDTO.getClickedFace().get()));
        PredicateInput predicateInput = new PredicateInput(
                itemUsageBlockDTO.getPlayer(),
                isBlock ? itemUsageBlockDTO.getClickedBlock().get().getLocation() : null,
                isLeftClick ? ClickButton.LEFT : ClickButton.RIGHT,
                isBlock ? ClickAt.BLOCK : ClickAt.AIR,
                new PredicateInput.Amount(
                        getTotalItems(itemUsageBlockDTO.getItem(), itemUsageBlockDTO.getPlayer()),
                        itemUsageBlockDTO.getItem() == null? 0 : itemUsageBlockDTO.getItem().getAmount()
                ),
                itemUsageBlockDTO.getCurrentTick(),
                itemUsageBlockDTO.getSlot()
        );
        return useItem0(itemUsageBlockDTO.getPlayer(), itemUsageBlockDTO.getItem(), predicateInput, placeholders);

    }

    private Map<String, String> placeholdersFor(Block block, BlockFace blockFace) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%target_x%", block.getX() + "");
        placeholders.put("%target_y%", block.getY() + "");
        placeholders.put("%target_z%", block.getZ() + "");
        Vector3i placedVector = new Vector3i(block.getX(), block.getY(), block.getZ());
        placedVector.add(new Vector3i(blockFace.getModX(), blockFace.getModY(), blockFace.getModZ()));
        placeholders.put("%place_x%", placedVector.x() + "");
        placeholders.put("%place_y%", placedVector.y() + "");
        placeholders.put("%place_z%", placedVector.z() + "");
        return placeholders;
    }

    private Map<String, String> placeholdersFor(Player player) {
        Map<String, String> placeholders = new HashMap<>(placeholdersFor((Entity) player));
        placeholders.put("%player_target%", player.getName());
        return placeholders;
    }

    private Map<String, String> placeholdersFor(Entity entity) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%target_x%", entity.getLocation().getX() + "");
        placeholders.put("%target_y%", entity.getLocation().getY() + "");
        placeholders.put("%target_z%", entity.getLocation().getZ() + "");
        return placeholders;
    }

    @Override
    public ItemUsageResultDTO dropItem(ItemUsageGeneralDTO itemUsageGeneralDTO) {
        Player player = itemUsageGeneralDTO.getPlayer();
        ItemStack item = itemUsageGeneralDTO.getItemStack();
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
        ItemUsageResultDTO usageResult = useItem0(
                player,
                item,
                new PredicateInput(
                        player,
                        useLoc,
                        ClickButton.DROP,
                        clickAt,
                        new PredicateInput.Amount(
                                getTotalItems(item, player),
                                item == null? 0 : item.getAmount()
                        ),
                        itemUsageGeneralDTO.getCurrentTick(),
                        itemUsageGeneralDTO.getSlot()
                ),
                placeholders
        );
        String customItemId = NBTCustomItem.getCustomItemId(item).getOrNull();
        if (usageResult.isShouldCancel() && player.getGameMode() == GameMode.CREATIVE && customItemId != null) {
            usageResult = usageResult.withMessage(lang().getCreativeDrop().replaceText("%key%", customItemId).bake());
        }
        return usageResult;
    }

    private ItemUsageResultDTO useItem0(Player player, ItemStack item, PredicateInput predicateInput, Map<String, String> placeholders) {
        ItemUsageResultDTO nop = new ItemUsageResultDTO(
                null,
                Collections.emptyList(),
                false,
                UsageEntity.Consume.NONE
        );
        if (item == null || !NBTCustomItem.hasCustomItemId(item)) return nop;
        String customItemId = NBTCustomItem.getCustomItemId(item).getOrNull();
        if (customItemId == null) return nop;
        CustomItemEntity customItem = itemRepository.getItem(customItemId).getOrNull();
        if (customItem == null) {
            boolean isItemUsage = predicateInput.getButton().isDefined();
            boolean shouldSendMessage = isItemUsage && config().isSendInvalidItemMessage();
            Component message = lang().getInvalidItem().replaceText("%key%", customItemId).bake();
            return new ItemUsageResultDTO(
                    shouldSendMessage? message : null,
                    Collections.emptyList(),
                    true,
                    UsageEntity.Consume.NONE
            );
        }
        List<UsageEntity> usages = customItem.getUsages().stream()
                .filter(usageFilter -> usageFilter.accepts(predicateInput))
                .collect(Collectors.toList());
        return usages.stream().map(usage -> {
            int id = customItem.getUsages().indexOf(usage);
            CooldownType cooldownType = usage.getCooldownType();
            boolean isOnCooldown;
            boolean isFreeze;
            Long remainingCooldownTime;
            Option<Long> lastUsage;
            switch (cooldownType) {
                case PER_ITEM:
                    NBTCustomItem.Cooldown cooldown = NBTCustomItem
                            .queryCooldown(item, usage.getCooldownMillis(), usage.getCooldownFreezeTimeMillis(), id);
                    isOnCooldown = cooldown.isDefault();
                    isFreeze = cooldown.isFrozen();
                    if (isOnCooldown) {
                        remainingCooldownTime = cooldown.getRemainingMillis();
                    } else remainingCooldownTime = null;
                    break;
                case GLOBAL:
                    lastUsage = cooldownRepository.lastUsageGlobal(customItemId, id, false);
                    isOnCooldown = lastUsage
                            .map(l -> l + usage.getCooldownMillis() > System.currentTimeMillis()).getOrElse(false);
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
                    break;
                case PER_PLAYER:
                    lastUsage = cooldownRepository.lastUsagePerPlayer(customItemId, player.getUniqueId(), id, false);
                    isOnCooldown = lastUsage
                            .map(l -> l + usage.getCooldownMillis() > System.currentTimeMillis()).getOrElse(false);
                    if (isOnCooldown) {
                        isFreeze = cooldownRepository.lastUsagePerPlayer(customItemId, player.getUniqueId(), id, true)
                                .map(l -> l + usage.getCooldownFreezeTimeMillis() > System.currentTimeMillis()).getOrElse(false);
                        if (!isFreeze) {
                            cooldownRepository.updatePerPlayer(customItemId, player.getUniqueId(), id, true);
                        }
                        remainingCooldownTime = lastUsage.getOrElse(0L) + usage.getCooldownMillis() - System.currentTimeMillis() ;
                    } else {
                        isFreeze = false;
                        cooldownRepository.updatePerPlayer(customItemId, player.getUniqueId(), id, false);
                        cooldownRepository.updatePerPlayer(customItemId, player.getUniqueId(), id, true);
                        remainingCooldownTime = null;
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + cooldownType);

            }
            if (isFreeze) return new ItemUsageResultDTO(
                    null,
                    Collections.emptyList(),
                    true,
                    UsageEntity.Consume.NONE
            );
            if (isOnCooldown) return new ItemUsageResultDTO(
                    null,
                    usage.getOnCooldown().stream().map(it -> prepareCooldown(remainingCooldownTime, player, it, placeholders))
                            .collect(Collectors.toList()),
                    true,
                    UsageEntity.Consume.NONE
            );
            return new ItemUsageResultDTO(
                    null,
                    usage.getCommands().stream()
                            .map(command -> prepare(command, player, placeholders))
                            .collect(Collectors.toList()),
                    usage.isCancel()  && !customItem.isPlainItem(),
                    usage.getConsume()
            );
        }).reduce((acc, e) -> {

            UsageEntity.Consume consume;
            boolean isAccNone = acc.getConsume().isNone();
            boolean anyNone = acc.getConsume().isNone() || e.getConsume().isNone();
            if (anyNone) {
                consume = isAccNone ? e.getConsume() : acc.getConsume();
            } else {
                boolean isBothAmount = acc.getConsume().isAmount() && e.getConsume().isAmount();
                if (isBothAmount) {
                    consume = new UsageEntity.Consume(
                            UsageEntity.ConsumeType.AMOUNT,
                            acc.getConsume().getAmount() + e.getConsume().getAmount()
                    );
                } else {
                    if (acc.getConsume().getConsumeType() == e.getConsume().getConsumeType()) {
                        consume = acc.getConsume();
                    } else {
                        List<UsageEntity.ConsumeType> typesHierarchy = Arrays.asList(
                                UsageEntity.ConsumeType.STACK,
                                UsageEntity.ConsumeType.ALL
                        );
                        int accIndex = typesHierarchy.indexOf(acc.getConsume().getConsumeType());
                        int eIndex = typesHierarchy.indexOf(e.getConsume().getConsumeType());
                        if (accIndex > eIndex) {
                            consume = acc.getConsume();
                        } else consume = e.getConsume();
                    }
                }
            }
            return new ItemUsageResultDTO(
                    null,
                    Stream.of(acc, e).map(ItemUsageResultDTO::getCommands)
                            .flatMap(List::stream).collect(Collectors.toList()),
                    (acc.isShouldCancel() || e.isShouldCancel()) && !customItem.isPlainItem(),
                    consume
            );
        }).orElse(new ItemUsageResultDTO(
                null,
                Collections.emptyList(),
                !customItem.isPlainItem(),
                UsageEntity.Consume.NONE
        ));
    }

    private CommandExecutionDTO prepare(UsageEntity.Command command, Player player, Map<String, String> placeholders) {
        String rawCommand = replacePlayer(command.getCommand(), player);
        AtomicReference<String> strAtomic = new AtomicReference<>(rawCommand);
        placeholders.forEach((placeholder, value) -> strAtomic.set(strAtomic.get().replace(placeholder, value)));
        return new CommandExecutionDTO(command.isExecuteAsConsole(), strAtomic.get());
    }

    private CommandExecutionDTO prepareCooldown(long milliseconds, Player player, UsageEntity.Command command, Map<String, String> placeholders) {
        CommandExecutionDTO halfPreparedDto = prepare(command, player, placeholders);
        String rawCommand = halfPreparedDto.getCommand();
        String finalCommand = RegexUtils.replaceAll(TIME_PATTERN.matcher(rawCommand), matcher -> {
            String timeUnit = matcher.group("timeunit").toLowerCase();
            int precision = Option.of(matcher.group("precision")).map(Integer::parseInt).getOrElse(0);
            double value;
            switch (timeUnit) {
                case "s":
                    value = milliseconds / 1000.0;
                    break;
                case "m":
                    value = milliseconds / 1000.0 / 60.0;
                    break;
                case "h":
                    value = milliseconds / 1000.0 / 60.0 / 60.0;
                    break;
                case "t":
                    value = milliseconds / 50.0;
                    break;
                default:
                    throw new IllegalStateException("Unknown time unit: " + timeUnit);
            }
            return String.format(Locale.ROOT, "%." + precision + "f", value);
        });
        return new CommandExecutionDTO(halfPreparedDto.isExecuteAsConsole(), finalCommand);
    }

    private String replacePlayer(String text, Player player) {
        return PapiSupport.tryParse(player, text)
                .replace("%player%", player.getName())
                .replace("%player_x%", player.getLocation().getX() + "")
                .replace("%player_y%", player.getLocation().getY() + "")
                .replace("%player_z%", player.getLocation().getZ() + "");
    }


    @Override
    public ItemUsageResultDTO useItemAt(ItemUsageEntityDTO itemUsageEntityDTO) {
        Entity clicked = itemUsageEntityDTO.getClicked();
        ItemStack item = itemUsageEntityDTO.getItem();
        Player player = itemUsageEntityDTO.getPlayer();
        boolean isRightClicked = itemUsageEntityDTO.isRightClicked();
        boolean isPlayer = clicked instanceof Player;
        PredicateInput clickType = new PredicateInput(
                player,
                clicked == null ? null : clicked.getLocation(),
                isRightClicked ? ClickButton.RIGHT : ClickButton.LEFT,
                isPlayer ? ClickAt.PLAYER : ClickAt.ENTITY,
                new PredicateInput.Amount(
                        getTotalItems(item, player),
                        item == null? 0 : item.getAmount()
                ),
                itemUsageEntityDTO.getCurrentTick(),
                itemUsageEntityDTO.getSlot()
        );
        Map<String, String> placeholders = isPlayer ? placeholdersFor((Player) clicked) : placeholdersFor(clicked);
        return useItem0(player, item, clickType, placeholders);
    }

    @Override
    public ItemUsageResultDTO tickItem(ItemUsageGeneralDTO itemUsageGeneralDTO) {
        Player player = itemUsageGeneralDTO.getPlayer();
        ItemStack item = itemUsageGeneralDTO.getItemStack();
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
        return useItem0(
                player,
                item,
                new PredicateInput(
                        player,
                        useLoc,
                        null,
                        clickAt,
                        new PredicateInput.Amount(
                                getTotalItems(item, player),
                                item == null? 0 : item.getAmount()
                        ),
                        itemUsageGeneralDTO.getCurrentTick(),
                        itemUsageGeneralDTO.getSlot()
                ),
                placeholders
        );
    }

    @Override
    public boolean canBePutInInventory(ItemStack item) {
        if (item == null || !NBTCustomItem.hasCustomItemId(item)) return true;
        String customItemId = NBTCustomItem.getCustomItemId(item).getOrNull();
        if (customItemId == null) return true;
        CustomItemEntity customItem = itemRepository.getItem(customItemId).getOrNull();
        if (customItem == null) return true;
        return customItem.canBePutInInventory();
    }

    @Override
    public boolean canBeMoved(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().name().endsWith("AIR")) return true;
        String customItemId = NBTCustomItem.getCustomItemId(itemStack).getOrNull();
        if (customItemId == null) return true;
        CustomItemEntity customItem = itemRepository.getItem(customItemId).getOrNull();
        if (customItem == null) return true;
        return customItem.canMove();
    }

    @Override
    public boolean shouldRemoveOnDeath(ItemStack item) {
        if (item == null || item.getType().name().endsWith("AIR")) return false;
        String customItemId = NBTCustomItem.getCustomItemId(item).getOrNull();
        if (customItemId == null) return false;
        CustomItemEntity customItem = itemRepository.getItem(customItemId).getOrNull();
        if (customItem == null) return false;
        return customItem.removeOnDeath();
    }

    @Override
    public boolean areEqual(ItemStack item, ItemStack item2) {
        if (item == null || item2 == null) return false;
        String customItemId1 = NBTCustomItem.getCustomItemId(item).getOrNull();
        String customItemId2 = NBTCustomItem.getCustomItemId(item2).getOrNull();
        if (customItemId1 == null || customItemId2 == null) return false;
        return Objects.equals(customItemId1, customItemId2);
    }

    @Override
    public void updateItem(ItemStack itemStack, @Nullable Player player) {
        itemRepository.updateItem(itemStack, player);
    }

    @Override
    public boolean canBeUsedInCraft(ItemStack item) {
        if (item == null || !NBTCustomItem.hasCustomItemId(item)) return true;
        String customItemId = NBTCustomItem.getCustomItemId(item).getOrNull();
        if (customItemId == null) return true;
        CustomItemEntity customItem = itemRepository.getItem(customItemId).getOrNull();
        if (customItem == null) return true;
        return customItem.isIngredient();
    }

    @Override
    public GiveItemDTO giveItem(String key, @Nullable Player player, Integer slot) {
        if (player == null) return new GiveItemDTO(
                lang().getSenderNotPlayer().bake(),
                null
        );
        if (slot != null && player.getInventory().getSize() <= slot) return new GiveItemDTO(
                lang().getSlotNotExist().replaceText("%slot%", slot + "").bake(),
                null
        );
        ItemStack itemStack = itemRepository.bakeItem(key, player).getOrNull();
        RawComponent message = itemStack == null ? lang().getItemDoesntExist() : lang().getGiveItemSuccessfully();
        message = message.replaceText("%player%", player.getName())
                .replaceText("%key%", key);
        return new GiveItemDTO(
                message.bake(),
                itemStack
        );
    }

    @Override
    public WithdrawItemDTO withdrawItem(String key, @Nullable Player player, int amount) {
        if (player == null) return new WithdrawItemDTO(
                lang().getSenderNotPlayer().bake(),
                null,
                false
        );
        CustomItemEntity customItem = itemRepository.getItem(key).getOrNull();
        if (customItem == null) return new WithdrawItemDTO(
                lang().getItemDoesntExist().replaceText("%key%", key).bake(),
                null,
                false
        );
        int totalItems = Arrays.stream(player.getInventory().getContents())
                .filter(item -> NBTCustomItem.getCustomItemId(item).map(s -> s.equals(key)).getOrElse(false))
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
        Component itemName = ReflectedRepresentations.ItemMeta.getDisplayName(customItem.getItemStack().getItemMeta())
                .getOrElse(MiniMessage.miniMessage().deserialize("<white>" + key + "</white>"));
        List<Component> finalMessages = messages.stream().map(raw -> {
            return raw.replaceText("%amount%", amount + "")
                    .replaceText("%item%", itemName)
                    .replaceText("%player%", player.getName())
                    .replaceText("%key%", key)
                    .bake();
        }).collect(Collectors.toList());
        return new WithdrawItemDTO(
                finalMessages.get(0),
                finalMessages.get(1),
                success
        );
    }

    @Override
    public Set<String> getItemKeys() {
        return itemRepository.getItemKeys();
    }

    @Override
    public long getUpdatePeriodTicks() {
        return config().getPlaceholderUpdatePeriod() / 50;
    }

    @Override
    public Component reload() {
        try {
            cooldownRepository.reload();
        } catch (Throwable msg) {
            log.warn("Failed to reload cooldowns: {}", msg.getMessage());
        }
        boolean result = configRepository.reload() && itemRepository.reloadItems();
        return result ? lang().getReloadSuccessfully().bake() : lang().getReloadUnsuccessfully().bake();
    }

    @Override
    public Component playerNotFound(String input) {
        return lang().getInvalidPlayer().replaceText("%player%", input).bake();
    }

    @Override
    public void cooldownAutoSave() {
        cooldownRepository.save();
    }
}
