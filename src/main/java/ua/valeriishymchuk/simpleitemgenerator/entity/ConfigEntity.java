package ua.valeriishymchuk.simpleitemgenerator.entity;

import io.vavr.Function0;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import ua.valeriishymchuk.simpleitemgenerator.common.config.exception.InvalidConfigurationException;
import ua.valeriishymchuk.simpleitemgenerator.common.item.NBTCustomItem;
import ua.valeriishymchuk.simpleitemgenerator.common.item.RawItem;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.reflection.ReflectedRepresentations;
import ua.valeriishymchuk.simpleitemgenerator.common.regex.RegexUtils;
import ua.valeriishymchuk.simpleitemgenerator.common.support.PapiSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.time.TimeTokenParser;
import ua.valeriishymchuk.simpleitemgenerator.common.version.FeatureSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.ClickAt;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.ClickButton;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;


@ConfigSerializable
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfigEntity {


    Map<String, CustomItemEntity> items = Function0.of(() -> {
        RawItem preparedItem = new RawItem(
                "DIAMOND",
                "<red><bold>Cool diamond%id%",
                Arrays.asList("<green>First lore", "<red>Second lore"),
                null,
                null,
                Arrays.asList(ItemFlag.HIDE_ATTRIBUTES.name(), ItemFlag.HIDE_ENCHANTS.name()),
                io.vavr.collection.HashMap.of(serializeEnchantment(Enchantment.LUCK), 1).toJavaMap(),
                Collections.emptyList(),
                null,
                null
        );
        if (FeatureSupport.CMD_SUPPORT) {
            preparedItem = preparedItem.withCmd(1);
        }
        RawItem exampleItem = preparedItem;
        UsageEntity singleCommandUsage = UsageEntity.DEFAULT
                .withCommands(deserializeConfigCommands(
                        "[console] say %player% hi, you clicked item"
                ));
        UsageEntity multipleCommandUsage = UsageEntity.DEFAULT
                .withCommands(deserializeConfigCommands(
                        "[console] tellraw %player% %minimessage_<red>I am using minimessage in placeholder%",
                        "[console] tellraw %player% %minimessage_<bold><green>So you don't have to use json%"
                ));
        UsageEntity example3 = multipleCommandUsage
                .withCooldownFreezeTimeMillis(TimeTokenParser.parse("1s"))
                .withCooldownMillis(TimeTokenParser.parse("5s"))
                .withOnCooldown(deserializeConfigCommands("[console] say %player% wait %time_s.2f%s"));
        UsageEntity example4 = example3.withCommands(deserializeConfigCommands(
                        "[console] say %player% you used item",
                        "[player] open_some_menu"
                )).withOnCooldown(deserializeConfigCommands(
                        "[console] say %player% wait %time_s.2f%s"
                )).withPredicates(Collections.singletonList(ClickButton.LEFT.asType()))
                .withCancel(false);
        UsageEntity example5 = multipleCommandUsage
                .withPredicates(Collections.singletonList(ClickAt.PLAYER.asType()))
                .withCooldownMillis(TimeTokenParser.parse("3h5m3s200"));
        UsageEntity example6 = example5
                .withPredicates(Collections.singletonList(ClickButton.LEFT.asType()
                        .withAt(ClickAt.AIR)));
        UsageEntity example7 = example6
                .withCooldownMillis(0);
        UsageEntity example8 = example7
                .withPredicates(Arrays.asList(ClickButton.LEFT.asType().withAt(ClickAt.AIR),
                        ClickAt.BLOCK.asType()
                ));
        AtomicInteger ai = new AtomicInteger();
        return io.vavr.collection.LinkedHashMap.<String, List<UsageEntity>>of(
                "item0", Collections.emptyList(),
                "item1", Collections.singletonList(singleCommandUsage),
                "item2", Collections.singletonList(multipleCommandUsage),
                "item3", Collections.singletonList(example3),
                "item4", Collections.singletonList(example4),
                "item5", Collections.singletonList(example5),
                "item6", Collections.singletonList(example6),
                "item7", Collections.singletonList(example7),
                "item8", Collections.singletonList(example8),
                "item9", Arrays.asList(example8, example8
                        .withPredicates(Collections.singletonList(ClickAt.ENTITY.asType()))
                        .withCommands(deserializeConfigCommands("[console] tellraw %minimessage_<green>You clicked at entity%")))
        ).mapValues(usage -> CustomItemEntity.of(exampleItem.replace("%id%", ai.getAndIncrement() + ""), usage)).toJavaMap();
    }).get();

    String placeholderUpdatePeriod = "10t";
    @Getter
    boolean checkForUpdates = true;
    @Getter
    boolean sendWelcomeMessage = true;

    public static String serializeEnchantment(Enchantment enchantment) {
        if (!FeatureSupport.NAMESPACED_ENCHANTMENTS_SUPPORT) return enchantment.getName();
        return ReflectedRepresentations.Enchantment.getKyoriKey(enchantment).asString();
    }

    public static UsageEntity.Command deserializeCommand(String rawCommand) throws IllegalArgumentException {
        return prepareCommand(rawCommand).replace(command -> RegexUtils.replaceAll(
                CustomItemEntity.MINIMESSAGE_COMMAND_PLACEHOLDER.matcher(command),
                commandMatcher -> {
                    String rawMessage = commandMatcher.group("placeholder");
                    return KyoriHelper.mimiMessageToJson(rawMessage);
                }
        ));
    }

    private static UsageEntity.Command prepareCommand(String rawCommand) throws InvalidConfigurationException {
        Matcher matcher = CustomItemEntity.COMMAND_EXECUTION_PATTERN.matcher(rawCommand);
        if (!matcher.find())
            throw InvalidConfigurationException.format("Invalid command: <white>%s</white>. Example command: <white>[console] msg %%player%% hello</white>", rawCommand);
        String command = matcher.group("command");
        boolean isConsoleSender = matcher.group("sender").equals("console");
        return new UsageEntity.Command(isConsoleSender, command);
    }

    private static List<UsageEntity.Command> deserializeCommands(List<String> rawCommands) {
        return rawCommands.stream().map(ConfigEntity::deserializeCommand).collect(Collectors.toList());
    }

    private static List<UsageEntity.Command> deserializeCommands(String... rawCommands) {
        return deserializeCommands(Arrays.asList(rawCommands));
    }

    private static List<UsageEntity.Command> deserializeConfigCommands(List<String> rawCommands) {
        return rawCommands.stream().map(ConfigEntity::prepareCommand).collect(Collectors.toList());
    }

    private static List<UsageEntity.Command> deserializeConfigCommands(String... rawCommands) {
        return deserializeConfigCommands(Arrays.asList(rawCommands));
    }

    public static String serializeCommand(UsageEntity.Command command) {
        String prepend = command.isExecuteAsConsole() ? "[console]" : "[player]";
        return prepend + " " + command.getCommand();
    }

    private static List<String> serializeCommands(List<UsageEntity.Command> commands) {
        return commands.stream().map(ConfigEntity::serializeCommand).collect(Collectors.toList());
    }

    public long getPlaceholderUpdatePeriod() {
        return TimeTokenParser.parse(placeholderUpdatePeriod);
    }

    @SneakyThrows
    public Option<ItemStack> bakeItem(String key, @Nullable Player player) {
        CustomItemEntity customItem = getItem(key).getOrNull();
        if (customItem == null) return Option.none();
        ItemStack itemStack = customItem.getItemStack();
        NBTCustomItem.setCustomItemId(itemStack, key);
        updateItem(itemStack, player);
        return Option.some(itemStack);
    }

    // initializing lazies
    public void init() throws InvalidConfigurationException {
        try {
            items.forEach((key, value) -> {
                try {
                    value.getUsages();
                    value.getHeadTexture().peek(h -> {
                        h.apply(value.getItemStack(), s -> s);
                    });
                    value.getItemStack();
                } catch (Exception e) {
                    throw InvalidConfigurationException.path(key, e);
                }
            });
        } catch (Exception e) {
            throw InvalidConfigurationException.path("items", e);
        }
    }

    public List<String> getItemKeys() {
        return new ArrayList<>(items.keySet());
    }

    public Option<CustomItemEntity> getItem(String key) {
        return Option.of(items.get(key));
    }

    @SneakyThrows
    public void updateItem(ItemStack itemStack, @Nullable Player player) {
        String customItemId = NBTCustomItem.getCustomItemId(itemStack).getOrNull();
        if (customItemId == null) return;
        CustomItemEntity customItem = items.get(customItemId);
        if (customItem == null) return;
        int configItemSignature = customItem.getSignature();
        Integer itemSignature = NBTCustomItem.getSignature(itemStack).getOrNull();
        if (itemSignature != null & !customItem.autoUpdate()) return;
        boolean isSameSignature = itemSignature != null && itemSignature == configItemSignature;
        String lastPlayer = NBTCustomItem.getLastHolder(itemStack).getOrNull();
        String currentPlayer = Option.of(player).map(Player::getName).getOrNull();
        boolean shouldUpdateHeadTexture = customItem.getHeadTexture()
                .map(t -> t.getValue().contains("%player%")).getOrElse(false)
                && !Objects.equals(lastPlayer, currentPlayer);
        if (!customItem.hasPlaceHolders() && isSameSignature && !shouldUpdateHeadTexture) return;
        ItemStack configItemStack = customItem.getItemStack();
        if (shouldUpdateHeadTexture) {
            customItem.getHeadTexture().get()
                    .apply(configItemStack, s -> s.replace("%player%", player == null? "n" : player.getName()));
            NBTCustomItem.setLastHolder(configItemStack, currentPlayer);
        }
        ItemMeta configItemMeta = configItemStack.getItemMeta();
        itemStack.setType(configItemStack.getType());
        //ItemMeta meta = itemStack.getItemMeta();
        ReflectedRepresentations.ItemMeta.getDisplayName(configItemMeta)
                .map(KyoriHelper::toJson)
                .map(line -> PapiSupport.tryParse(player, line))
                .map(KyoriHelper::fromJson)
                .peek(line -> ReflectedRepresentations.ItemMeta.setDisplayName(configItemMeta, line));
        ReflectedRepresentations.ItemMeta.setLore(
                configItemMeta,
                ReflectedRepresentations.ItemMeta.getLore(configItemMeta).stream()
                        .map(KyoriHelper::toJson)
                        .map(line -> PapiSupport.tryParse(player, line))
                        .map(KyoriHelper::fromJson)
                        .collect(Collectors.toList())
        );
        itemStack.setItemMeta(configItemMeta);
        NBTCustomItem.setCustomItemId(itemStack, customItemId);
        //NBTCustomItem.setSignature(itemStack, configItemSignature);
    }


}
