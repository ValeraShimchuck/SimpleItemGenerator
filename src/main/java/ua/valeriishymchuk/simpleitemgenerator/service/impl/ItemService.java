package ua.valeriishymchuk.simpleitemgenerator.service.impl;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import ua.valeriishymchuk.simpleitemgenerator.common.component.RawComponent;
import ua.valeriishymchuk.simpleitemgenerator.common.item.NBTCustomItem;
import ua.valeriishymchuk.simpleitemgenerator.common.raytrace.IRayTraceResult;
import ua.valeriishymchuk.simpleitemgenerator.common.raytrace.RayTraceBlockResult;
import ua.valeriishymchuk.simpleitemgenerator.common.raytrace.RayTraceEntityResult;
import ua.valeriishymchuk.simpleitemgenerator.common.raytrace.RayTraceHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.regex.RegexUtils;
import ua.valeriishymchuk.simpleitemgenerator.common.support.PapiSupport;
import ua.valeriishymchuk.simpleitemgenerator.dto.CommandExecutionDTO;
import ua.valeriishymchuk.simpleitemgenerator.dto.GiveItemDTO;
import ua.valeriishymchuk.simpleitemgenerator.dto.ItemUsageResultDTO;
import ua.valeriishymchuk.simpleitemgenerator.entity.ConfigEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.LangEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.UsageEntity;
import ua.valeriishymchuk.simpleitemgenerator.repository.IConfigRepository;
import ua.valeriishymchuk.simpleitemgenerator.service.IItemService;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ItemService implements IItemService {

    private static final Pattern TIME_PATTERN = Pattern.compile("%time_(?<timeunit>[a-z])(\\.(?<precision>\\d+)f)?%");

    IConfigRepository configRepository;


    private ConfigEntity config() {
        return configRepository.getConfig();
    }

    private LangEntity lang() {
        return configRepository.getLang();
    }

    @Override
    public ItemUsageResultDTO useItem(Player player, Action action, ItemStack item, @Nullable Block clickedBlock) {
        boolean isBlock = clickedBlock != null;
        boolean isLeftClick = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        UsageEntity.ClickType clickType = new UsageEntity.ClickType(
                isLeftClick ? UsageEntity.ClickButton.LEFT : UsageEntity.ClickButton.RIGHT,
                isBlock ? UsageEntity.ClickAt.BLOCK : UsageEntity.ClickAt.AIR
        );
        ItemUsageResultDTO nop = new ItemUsageResultDTO(
                null,
                Collections.emptyList(),
                false,
                UsageEntity.Consume.NONE
        );
        if (action == Action.PHYSICAL) return nop;
        Map<String, String> placeholders = new HashMap<>();
        if (isBlock) placeholders.putAll(placeholdersFor(clickedBlock));
        return useItem0(player, item, clickType, placeholders);

    }

    private Map<String, String> placeholdersFor(Block block) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%target_x%", block.getX() + "");
        placeholders.put("%target_y%", block.getY() + "");
        placeholders.put("%target_z%", block.getZ() + "");
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
    public ItemUsageResultDTO dropItem(Player player, ItemStack item) {
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
        UsageEntity.ClickAt clickAt;
        if (result.hitBlock()) {
            RayTraceBlockResult castesResult = (RayTraceBlockResult) result;
            clickAt = UsageEntity.ClickAt.BLOCK;
            placeholders.putAll(placeholdersFor(castesResult.getHitBlock()));
        } else if (result.hitEntity()) {
            RayTraceEntityResult castesResult = (RayTraceEntityResult) result;
            if (castesResult.getEntity() instanceof Player) {
                placeholders.putAll(placeholdersFor((Player) castesResult.getEntity()));
                clickAt = UsageEntity.ClickAt.PLAYER;
            } else {
                placeholders.putAll(placeholdersFor(castesResult.getEntity()));
                clickAt = UsageEntity.ClickAt.ENTITY;
            }
        } else clickAt = UsageEntity.ClickAt.AIR;
        ItemUsageResultDTO usageResult = useItem0(player, item, new UsageEntity.ClickType(UsageEntity.ClickButton.DROP, clickAt), placeholders);
        String customItemId = NBTCustomItem.getCustomItemId(item).getOrNull();
        if (usageResult.isShouldCancel() && player.getGameMode() == GameMode.CREATIVE && customItemId != null) {
            usageResult = usageResult.withMessage(lang().getCreativeDrop().replaceText("%key%", customItemId).bake());
        }
        return usageResult;
    }

    private ItemUsageResultDTO useItem0(Player player, ItemStack item, UsageEntity.ClickType clickType, Map<String, String> placeholders) {
        ItemUsageResultDTO nop = new ItemUsageResultDTO(
                null,
                Collections.emptyList(),
                false,
                UsageEntity.Consume.NONE
        );
        if (item == null || !NBTCustomItem.hasCustomItemId(item)) return nop;
        String customItemId = NBTCustomItem.getCustomItemId(item).getOrNull();
        if (customItemId == null) return nop;
        ConfigEntity.CustomItem customItem = config().getItem(customItemId).getOrNull();
        if (customItem == null) return new ItemUsageResultDTO(
                lang().getInvalidItem().replaceText("%key%", customItemId).bake(),
                Collections.emptyList(),
                true,
                UsageEntity.Consume.NONE
        );
        List<UsageEntity> usages = customItem.getUsages().stream()
                .filter(usageFilter -> usageFilter.accepts(clickType))
                .collect(Collectors.toList());
        return usages.stream().map(usage -> {
            NBTCustomItem.Cooldown cooldown = NBTCustomItem
                    .queryCooldown(item, usage.getCooldownMillis(), usage.getCooldownFreezeTimeMillis(), customItem.getUsages().indexOf(usage));
            if (cooldown.isFrozen()) return new ItemUsageResultDTO(
                    null,
                    Collections.emptyList(),
                    true,
                    UsageEntity.Consume.NONE
            );
            if (cooldown.isDefault()) return new ItemUsageResultDTO(
                    null,
                    usage.getOnCooldown().stream().map(it -> prepareCooldown(cooldown.getRemainingMillis(), player, it, placeholders))
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
    public ItemUsageResultDTO useItemAt(Player player, boolean isRightClicked, Entity clicked, ItemStack item) {
        boolean isPlayer = clicked instanceof Player;
        UsageEntity.ClickType clickType = new UsageEntity.ClickType(
                isRightClicked ? UsageEntity.ClickButton.RIGHT : UsageEntity.ClickButton.LEFT,
                isPlayer ? UsageEntity.ClickAt.PLAYER : UsageEntity.ClickAt.ENTITY
        );
        Map<String, String> placeholders = isPlayer ? placeholdersFor((Player) clicked) : placeholdersFor(clicked);
        return useItem0(player, item, clickType, placeholders);
    }

    @Override
    public boolean canBePutInInventory(ItemStack item) {
        if (item == null || !NBTCustomItem.hasCustomItemId(item)) return true;
        String customItemId = NBTCustomItem.getCustomItemId(item).getOrNull();
        if (customItemId == null) return true;
        ConfigEntity.CustomItem customItem = config().getItem(customItemId).getOrNull();
        if (customItem == null) return true;
        return customItem.canBePutInInventory();
    }

    @Override
    public boolean canBeMoved(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().name().endsWith("AIR")) return true;
        String customItemId = NBTCustomItem.getCustomItemId(itemStack).getOrNull();
        if (customItemId == null) return true;
        ConfigEntity.CustomItem customItem = config().getItem(customItemId).getOrNull();
        if (customItem == null) return true;
        return customItem.canMove();
    }

    @Override
    public boolean shouldRemoveOnDeath(ItemStack item) {
        if (item == null || item.getType().name().endsWith("AIR")) return false;
        String customItemId = NBTCustomItem.getCustomItemId(item).getOrNull();
        if (customItemId == null) return false;
        ConfigEntity.CustomItem customItem = config().getItem(customItemId).getOrNull();
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
    public void updateItem(ItemStack itemStack, Player player) {
        if (!PapiSupport.isPluginEnabled()) return;
        config().updateItem(itemStack, player);
    }

    @Override
    public boolean canBeUsedInCraft(ItemStack item) {
        if (item == null || !NBTCustomItem.hasCustomItemId(item)) return true;
        String customItemId = NBTCustomItem.getCustomItemId(item).getOrNull();
        if (customItemId == null) return true;
        ConfigEntity.CustomItem customItem = config().getItem(customItemId).getOrNull();
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
        ItemStack itemStack = config().bakeItem(key, player).getOrNull();
        RawComponent message = itemStack == null ? lang().getItemDoesntExist() : lang().getGiveItemSuccessfully();
        message = message.replaceText("%player%", player.getName())
                .replaceText("%key%", key);
        return new GiveItemDTO(
                message.bake(),
                itemStack
        );
    }

    @Override
    public List<String> getItemKeys() {
        return config().getItemKeys();
    }

    @Override
    public long getUpdatePeriodTicks() {
        return config().getPlaceholderUpdatePeriod() / 50;
    }

    @Override
    public Component reload() {
        boolean result = configRepository.reload();
        return result ? lang().getReloadSuccessfully().bake() : lang().getReloadUnsuccessfully().bake();
    }

    @Override
    public Component playerNotFound(String input) {
        return lang().getInvalidPlayer().replaceText("%player%", input).bake();
    }
}
