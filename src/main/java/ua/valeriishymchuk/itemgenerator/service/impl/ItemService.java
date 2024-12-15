package ua.valeriishymchuk.itemgenerator.service.impl;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import ua.valeriishymchuk.itemgenerator.common.component.RawComponent;
import ua.valeriishymchuk.itemgenerator.common.item.NBTCustomItem;
import ua.valeriishymchuk.itemgenerator.common.regex.RegexUtils;
import ua.valeriishymchuk.itemgenerator.common.support.PapiSupport;
import ua.valeriishymchuk.itemgenerator.dto.CommandExecutionDTO;
import ua.valeriishymchuk.itemgenerator.dto.GiveItemDTO;
import ua.valeriishymchuk.itemgenerator.dto.ItemUsageResultDTO;
import ua.valeriishymchuk.itemgenerator.entity.ConfigEntity;
import ua.valeriishymchuk.itemgenerator.entity.LangEntity;
import ua.valeriishymchuk.itemgenerator.entity.UsageEntity;
import ua.valeriishymchuk.itemgenerator.repository.IConfigRepository;
import ua.valeriishymchuk.itemgenerator.service.IItemService;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ItemService implements IItemService {

    private static final Pattern TIME_PATTERN = Pattern.compile("%time_(?<timeunit>[a-z])(\\.(?<precision>\\d+f))?%");

    IConfigRepository configRepository;


    private ConfigEntity config() {
        return configRepository.getConfig();
    }

    private LangEntity lang() {
        return configRepository.getLang();
    }

    @Override
    public ItemUsageResultDTO useItem(Player player, Action action, ItemStack item) {
        boolean isBlock = action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR;
        boolean isLeftClick = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        UsageEntity.ClickType clickType = new UsageEntity.ClickType(
                isLeftClick ? UsageEntity.ClickButton.LEFT : UsageEntity.ClickButton.RIGHT,
                isBlock ? UsageEntity.ClickAt.BLOCK : UsageEntity.ClickAt.AIR
        );
        ItemUsageResultDTO nop = new ItemUsageResultDTO(
                null,
                Collections.emptyList(),
                false
        );
        if (action == Action.PHYSICAL) return nop;
        return useItem0(player, item, clickType);

    }

    private ItemUsageResultDTO useItem0(Player player, ItemStack item, UsageEntity.ClickType clickType) {
        ItemUsageResultDTO nop = new ItemUsageResultDTO(
                null,
                Collections.emptyList(),
                false
        );
        if (item == null || !NBTCustomItem.hasCustomItemId(item)) return nop;
        String customItemId = NBTCustomItem.getCustomItemId(item).getOrNull();
        if (customItemId == null) return nop;
        ConfigEntity.CustomItem customItem = config().getItem(customItemId).getOrNull();
        if (customItem == null) return new ItemUsageResultDTO(
                lang().getInvalidItem().replaceText("%key%", customItemId).bake(),
                Collections.emptyList(),
                true
        );
        UsageEntity usage = customItem.getUsages().stream()
                .filter(usageFilter -> usageFilter.accepts(clickType))
                .findFirst().orElse(null);
        if (usage == null) return new ItemUsageResultDTO(
                null,
                Collections.emptyList(),
                true
        );
        NBTCustomItem.Cooldown cooldown = NBTCustomItem.queryCooldown(item, usage.getCooldownMillis(), usage.getCooldownFreezeTimeMillis());
        if (cooldown.isFrozen()) return new ItemUsageResultDTO(
                null,
                Collections.emptyList(),
                true
        );
        if (cooldown.isDefault()) return new ItemUsageResultDTO(
                null,
                usage.getOnCooldown().stream().map(it -> prepareCooldown(cooldown.getRemainingMillis(), player, it))
                        .collect(Collectors.toList()),
                true
        );
        return new ItemUsageResultDTO(
                null,
                usage.getCommands().stream()
                        .map(command -> prepare(command, player))
                        .collect(Collectors.toList()),
                usage.isCancel()
        );
    }

    private CommandExecutionDTO prepare(UsageEntity.Command command, Player player) {
        String rawCommand = replacePlayer(command.getCommand(), player);
        return new CommandExecutionDTO(command.isExecuteAsConsole(), rawCommand);
    }

    private CommandExecutionDTO prepareCooldown(long milliseconds, Player player, UsageEntity.Command command) {
        String rawCommand = replacePlayer(command.getCommand(), player);
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
        return new CommandExecutionDTO(command.isExecuteAsConsole(), finalCommand);
    }

    private String replacePlayer(String text, Player player) {
        return PapiSupport.tryParse(player, text).replace("%player%", player.getName());
    }


    @Override
    public ItemUsageResultDTO useItemAt(Player player, boolean isRightClicked, Entity clicked, ItemStack item) {
        boolean isPlayer = clicked instanceof Player;
        UsageEntity.ClickType clickType = new UsageEntity.ClickType(
                isRightClicked? UsageEntity.ClickButton.RIGHT : UsageEntity.ClickButton.LEFT,
                isPlayer ? UsageEntity.ClickAt.PLAYER : UsageEntity.ClickAt.ENTITY
        );
        return useItem0(player, item, clickType);
    }

    @Override
    public void updateItem(ItemStack itemStack, Player player) {
        config().updateItem(itemStack, player);
    }

    @Override
    public GiveItemDTO giveItem(String key, @Nullable Player player) {
        if (player == null) return new GiveItemDTO(
                lang().getSenderNotPlayer().bake(),
                null
        );
        ItemStack itemStack = config().bakeItem(key, player).getOrNull();
        RawComponent message = itemStack == null? lang().getItemDoesntExist() : lang().getGiveItemSuccessfully();
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
        return result? lang().getReloadSuccessfully().bake() : lang().getReloadUnsuccessfully().bake();
    }
}
