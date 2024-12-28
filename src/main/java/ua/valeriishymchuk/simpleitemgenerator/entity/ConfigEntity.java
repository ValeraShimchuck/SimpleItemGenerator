package ua.valeriishymchuk.simpleitemgenerator.entity;

import io.vavr.Function0;
import io.vavr.Lazy;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import ua.valeriishymchuk.simpleitemgenerator.common.config.DefaultLoader;
import ua.valeriishymchuk.simpleitemgenerator.common.item.NBTCustomItem;
import ua.valeriishymchuk.simpleitemgenerator.common.item.RawItem;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.reflection.ReflectedRepresentations;
import ua.valeriishymchuk.simpleitemgenerator.common.regex.RegexUtils;
import ua.valeriishymchuk.simpleitemgenerator.common.support.ItemsAdderSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.support.PapiSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.time.TimeTokenParser;
import ua.valeriishymchuk.simpleitemgenerator.common.version.FeatureSupport;
import ua.valeriishymchuk.simpleitemgenerator.entity.UsageEntity.ClickAt;
import ua.valeriishymchuk.simpleitemgenerator.entity.UsageEntity.ClickButton;
import ua.valeriishymchuk.simpleitemgenerator.entity.UsageEntity.ClickType;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@ConfigSerializable
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfigEntity {


    Map<String, CustomItem> items = Function0.of(() -> {
        RawItem preparedItem = new RawItem(
                "DIAMOND",
                "<red><bold>Cool diamond%id%",
                Arrays.asList("<green>First lore", "<red>Second lore"),
                null,
                null,
                Arrays.asList(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS),
                io.vavr.collection.HashMap.of(serializeEnchantment(Enchantment.LUCK), 1).toJavaMap(),
                Collections.emptyList()
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
                .withPredicates(Collections.singletonList(new ClickType(ClickButton.LEFT, ClickAt.AIR)));
        UsageEntity example7 = example6
                .withCooldownMillis(0);
        UsageEntity example8 = example7
                .withPredicates(Arrays.asList(
                        new ClickType(ClickButton.LEFT, ClickAt.AIR),
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
        ).mapValues(usage -> CustomItem.of(exampleItem.replace("%id%", ai.getAndIncrement() + ""), usage)).toJavaMap();
    }).get();

    String placeholderUpdatePeriod = "10t";

    private static String serializeEnchantment(Enchantment enchantment) {
        if (!FeatureSupport.NAMESPACED_ENCHANTMENTS_SUPPORT) return enchantment.getName();
        return ReflectedRepresentations.Enchantment.getKyoriKey(enchantment).asString();
    }

    private static UsageEntity.Command deserializeCommand(String rawCommand) {
        return prepareCommand(rawCommand).replace(command -> RegexUtils.replaceAll(
                CustomItem.MINIMESSAGE_COMMAND_PLACEHOLDER.matcher(command),
                commandMatcher -> {
                    String rawMessage = commandMatcher.group("placeholder");
                    return KyoriHelper.mimiMessageToJson(rawMessage);
                }
        ));
    }

    private static UsageEntity.Command prepareCommand(String rawCommand) {
        Matcher matcher = CustomItem.COMMAND_EXECUTION_PATTERN.matcher(rawCommand);
        if (!matcher.find()) throw new IllegalArgumentException("Invalid command: " + rawCommand);
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

    private static String serializeCommand(UsageEntity.Command command) {
        String prepend = command.isExecuteAsConsole() ? "[console]" : "[player]";
        return prepend + " " + command.getCommand();
    }

    private static List<String> serializeCommands(List<UsageEntity.Command> commands) {
        return commands.stream().map(ConfigEntity::serializeCommand).collect(Collectors.toList());
    }

    public long getPlaceholderUpdatePeriod() {
        return TimeTokenParser.parse(placeholderUpdatePeriod);
    }

    public Option<ItemStack> bakeItem(String key, Player player) {
        CustomItem customItem = getItem(key).getOrNull();
        if (customItem == null) return Option.none();
        ItemStack itemStack = customItem.getItemStack();
        NBTCustomItem.setCustomItemId(itemStack, key);
        updateItem(itemStack, player);
        return Option.some(itemStack);
    }

    public List<String> getItemKeys() {
        return new ArrayList<>(items.keySet());
    }

    public Option<CustomItem> getItem(String key) {
        return Option.of(items.get(key));
    }

    public void updateItem(ItemStack itemStack, Player player) {
        String customItemId = NBTCustomItem.getCustomItemId(itemStack).getOrNull();
        if (customItemId == null) return;
        CustomItem customItem = items.get(customItemId);
        if (customItem == null) return;
        if (!customItem.hasPlaceHolders()) return;
        ItemMeta configItemMeta = customItem.getItemStack().getItemMeta();
        ItemMeta meta = itemStack.getItemMeta();
        ReflectedRepresentations.ItemMeta.getDisplayName(configItemMeta)
                .map(KyoriHelper::toJson)
                .map(line -> PapiSupport.tryParse(player, line))
                .map(KyoriHelper::fromJson)
                .peek(line -> ReflectedRepresentations.ItemMeta.setDisplayName(meta, line));
        ReflectedRepresentations.ItemMeta.setLore(
                meta,
                ReflectedRepresentations.ItemMeta.getLore(configItemMeta).stream()
                        .map(KyoriHelper::toJson)
                        .map(line -> PapiSupport.tryParse(player, line))
                        .map(KyoriHelper::fromJson)
                        .collect(Collectors.toList())
        );
        itemStack.setItemMeta(meta);
    }

    @ConfigSerializable
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    public static class CustomItem {
        private static final Pattern MINIMESSAGE_COMMAND_PLACEHOLDER = Pattern.compile("%minimessage_(?<placeholder>.+)%");
        private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%(?<placeholder>\\S+)%");
        private static final Pattern COMMAND_EXECUTION_PATTERN =
                Pattern.compile("\\[(?<sender>player|console)] (?<command>.*)");
        private static final Pattern SINGLE_PREDICATE_PATTERN =
                Pattern.compile("\\[(?<enum>at|button)] (?<type>.*)");
        private static final Pattern ITEM_LINK_PATTERN = Pattern.compile("\\[(?<linktype>.+)] (?<link>.*)");


        ConfigurationNode item;
        ConfigurationNode usage;
        Boolean isIngredient;

        transient Lazy<List<UsageEntity>> usages = Lazy.of(this::parseUsages);
        transient Lazy<ItemStack> itemStack = Lazy.of(this::parseItem);
        transient Lazy<Boolean> hasPlaceholders = Lazy.of(this::hasPlaceholders0);

        private CustomItem() {
            this(createNode(), createNode(), false);
        }

        private static ConfigurationNode createNode() {
            return DefaultLoader.yaml().createNode();
            //return createNode(ConfigurationOptions.defaults());
        }

        public boolean isIngredient() {
            if (isIngredient == null)  return false;
            return isIngredient;
        }

        public static CustomItem of(ItemStack item, List<UsageEntity> usages) {
            return new CustomItem(serializeItemStack(item), serializeUsages(usages), false);
        }

        @SneakyThrows
        public static CustomItem of(RawItem item, List<UsageEntity> usages) {
            return new CustomItem(createNode().set(item), serializeUsages(usages), false);
        }

        @SneakyThrows
        private static ConfigurationNode serializeItemStack(ItemStack item) {
            ConfigurationNode node = createNode();
            ItemMeta meta = item.getItemMeta();
            Material material = item.getType();
            RawItem rawItem = new RawItem(
                    material.name(),
                    Option.of(meta.getDisplayName()).map(KyoriHelper::jsonToMiniMessage).getOrNull(),
                    meta.getLore().stream().map(KyoriHelper::jsonToMiniMessage).collect(Collectors.toList()),
                    null,
                    ReflectedRepresentations.ItemMeta.isUnbreakable(meta),
                    new ArrayList<>(meta.getItemFlags()),
                    io.vavr.collection.HashMap.ofAll(meta.getEnchants())
                            .mapKeys(ConfigEntity::serializeEnchantment)
                            .toJavaMap(),
                    Collections.emptyList() // TODO add later
            );
            Integer cmd = ReflectedRepresentations.ItemMeta.tryGetCustomModelData(meta).getOrNull();
            if (cmd != null) {
                rawItem = rawItem.withCmd(cmd);
            }
            node.set(RawItem.class, rawItem);
            return node;
        }

        @SneakyThrows
        private static ConfigurationNode serializeUsages(List<UsageEntity> usages) {
            ConfigurationNode node = createNode();
            if (usages.isEmpty()) return node;
            if (usages.size() == 1) {
                return serializeUsage(usages.get(0));
            }
            return node.set(usages.stream().map(ConfigEntity.CustomItem::serializeUsage).map(ConfigurationNode::raw).collect(Collectors.toList()));
        }

        @SneakyThrows
        private static ConfigurationNode serializeUsage(UsageEntity usage) {
            ConfigurationNode node = createNode();
            boolean isEmpty = !usage.isCancel() && usage.getCommands().isEmpty() &&
                    usage.getOnCooldown().isEmpty() && usage.getPredicates().isEmpty();
            if (isEmpty) return node;
            boolean isCommandsOnly = !usage.getCommands().isEmpty() && usage.getOnCooldown().isEmpty()
                    && usage.getPredicates().isEmpty() && usage.getCooldownMillis() == 0 && usage.getCooldownFreezeTimeMillis() == 0;
            if (isCommandsOnly) {
                if (usage.getCommands().size() == 1) return serializeCommand(usage.getCommands().get(0));
                return node.set(usage.getCommands().stream()
                        .map(ConfigEntity.CustomItem::serializeCommand)
                        .map(ConfigurationNode::raw)
                        .collect(Collectors.toList()));
            }
            if (usage.getCooldownMillis() > 0)
                node.node("cooldown").set(TimeTokenParser.parse(usage.getCooldownMillis()));
            if (usage.getCooldownFreezeTimeMillis() > 0)
                node.node("freezetime").set(TimeTokenParser.parse(usage.getCooldownFreezeTimeMillis()));
            if (!usage.isCancel()) node.node("cancel").set(false);
            node.node("commands").set(serializeCommands(usage.getCommands()).raw());
            node.node("on-cooldown").set(serializeCommands(usage.getOnCooldown()).raw());
            node.node("predicate").set(serializePredicates(usage.getPredicates()).raw());
            return node;
        }

        @SneakyThrows
        private static ConfigurationNode serializePredicates(List<ClickType> predicate) {
            ConfigurationNode node = createNode();
            if (predicate.isEmpty()) return node;
            if (predicate.size() == 1) {
                return serializePredicate(predicate.get(0));
            }
            node.set(predicate.stream().map(CustomItem::serializePredicate).map(ConfigurationNode::raw).collect(Collectors.toList()));
            return node;
        }

        @SneakyThrows
        private static ConfigurationNode serializePredicate(ClickType clickType) {
            boolean hasAtOrSide = clickType.getAt().isDefined() ^ clickType.getSide().isDefined();
            if (hasAtOrSide) {
                boolean isAt = clickType.getAt().isDefined();
                String prepend = isAt ? "at" : "button";
                String value = clickType.getAt().map(Enum::name).orElse(clickType.getSide().map(Enum::name)).map(String::toLowerCase).get();
                ConfigurationNode node = createNode();
                node.set("[" + prepend + "] " + value);
                return node;
            }
            ConfigurationNode node = createNode();
            node.node("at").set(clickType.getAt().map(Enum::name).map(String::toLowerCase).get());
            node.node("button").set(clickType.getSide().map(Enum::name).map(String::toLowerCase).get());
            return node;
        }

        @SneakyThrows
        private static ConfigurationNode serializeCommands(List<UsageEntity.Command> commands) {
            ConfigurationNode node = createNode();
            if (commands.isEmpty()) return node;
            if (commands.size() == 1) return serializeCommand(commands.get(0));
            node.set(commands.stream()
                    .map(ConfigEntity.CustomItem::serializeCommand)
                    .map(ConfigurationNode::raw)
                    .collect(Collectors.toList()));
            return node;
        }

        @SneakyThrows
        private static ConfigurationNode serializeCommand(UsageEntity.Command command) {
            ConfigurationNode node = createNode();
            node.set(ConfigEntity.serializeCommand(command));
            return node;
        }

        public boolean hasPlaceHolders() {
            return hasPlaceholders.get();
        }

        private boolean hasPlaceholders0() {
            ItemStack item = getItemStack();
            Option<Component> displayOpt = ReflectedRepresentations.ItemMeta.getDisplayName(item.getItemMeta());
            List<Component> lore = ReflectedRepresentations.ItemMeta.getLore(item.getItemMeta());
            return Stream.of(
                            displayOpt.toJavaList(),
                            lore
                    ).flatMap(List::stream).map(KyoriHelper::toJson)
                    .map(PLACEHOLDER_PATTERN::matcher)
                    .anyMatch(Matcher::find);
        }

        public List<UsageEntity> getUsages() {
            return usages.get();
        }

        public ItemStack getItemStack() {
            return itemStack.get().clone();
        }

        @SneakyThrows
        private ItemStack parseItem() {
            if (item.isMap()) return item.get(RawItem.class).bake();
            String rawItem = item.getString();
            Matcher matcher = ITEM_LINK_PATTERN.matcher(rawItem);
            if (!matcher.find()) throw new IllegalArgumentException("Invalid item: " + rawItem);
            String linkType = matcher.group("linktype");
            String link = matcher.group("link");
            ItemStack item;
            if (linkType.equals("itemsadder")) {
                item = ItemsAdderSupport.getItem(link);
            } else {
                throw new IllegalArgumentException("Invalid link type: " + linkType);
            }
            return item;
        }

        @SneakyThrows
        private List<UsageEntity> parseUsages() {
            if (usage.isNull())
                return Collections.singletonList(
                        new UsageEntity(
                                Collections.emptyList(),
                                0,
                                0,
                                true,
                                Collections.emptyList(),
                                Collections.emptyList()
                        )
                );
            if (usage.isList()) return usage.getList(ConfigurationNode.class).stream()
                    .map(this::parseUsage).collect(Collectors.toList());
            return Collections.singletonList(parseUsage(usage));
        }

        @SneakyThrows
        private UsageEntity parseUsage(ConfigurationNode node) {
            if (!node.isMap()) return new UsageEntity(
                    Collections.emptyList(),
                    0,
                    0,
                    true,
                    Collections.emptyList(),
                    parseCommands(node)
            );
            long cooldown = TimeTokenParser.parse(node.node("cooldown").getString(""));
            long freezeTime = TimeTokenParser.parse(node.node("freezetime").getString(""));
            boolean shouldCancelEvent = node.node("cancel").getBoolean(true);
            List<ClickType> predicate;
            ConfigurationNode predicateNode = node.node("predicate");
            if (predicateNode.isNull()) predicate = Collections.emptyList();
            else if (predicateNode.isList()) {
                predicate = predicateNode.getList(ConfigurationNode.class).stream()
                        .map(this::parsePredicate)
                        .collect(Collectors.toList());
            } else predicate = Collections.singletonList(parsePredicate(predicateNode));
            List<UsageEntity.Command> onCooldown = parseCommands(node.node("on-cooldown"));
            List<UsageEntity.Command> commands = parseCommands(node.node("commands"));
            return new UsageEntity(predicate, cooldown, freezeTime, shouldCancelEvent, onCooldown, commands);
        }

        @SneakyThrows
        private List<UsageEntity.Command> parseCommands(ConfigurationNode node) {
            if (node.isNull()) return Collections.emptyList();
            if (node.isList()) {
                return node.getList(ConfigurationNode.class).stream()
                        .map(this::parseCommand)
                        .collect(Collectors.toList());
            }
            return Collections.singletonList(parseCommand(node));
        }


        private UsageEntity.Command parseCommand(ConfigurationNode node) {
            return deserializeCommand(node.getString());
        }

        private ClickType parsePredicate(ConfigurationNode node) {
            if (node.isMap()) {
                ClickAt clickAt = Option.of(node.node("at").getString())
                        .map(String::toUpperCase)
                        .map(ClickAt::valueOf).getOrNull();
                ClickButton clickButton = Option.of(node.node("button").getString())
                        .map(String::toUpperCase)
                        .map(ClickButton::valueOf).getOrNull();
                return new ClickType(clickButton, clickAt);
            }
            Matcher matcher = SINGLE_PREDICATE_PATTERN.matcher(node.getString());
            if (!matcher.find()) throw new IllegalArgumentException("Invalid predicate: " + node.getString());
            String type = matcher.group("type");
            boolean isAt = matcher.group("enum").equals("at");
            ClickAt clickAt = null;
            ClickButton clickButton = null;
            if (isAt) clickAt = ClickAt.valueOf(type.toUpperCase());
            else clickButton = ClickButton.valueOf(type.toUpperCase());
            return new ClickType(clickButton, clickAt);
        }

    }


}
