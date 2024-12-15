package ua.valeriishymchuk.itemgenerator.entity;

import io.vavr.Function0;
import io.vavr.Lazy;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import ua.valeriishymchuk.itemgenerator.common.item.NBTCustomItem;
import ua.valeriishymchuk.itemgenerator.common.item.RawItem;
import ua.valeriishymchuk.itemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.itemgenerator.common.reflection.ReflectedRepresentations;
import ua.valeriishymchuk.itemgenerator.common.support.ItemsAdderSupport;
import ua.valeriishymchuk.itemgenerator.common.support.PapiSupport;
import ua.valeriishymchuk.itemgenerator.common.time.TimeTokenParser;
import ua.valeriishymchuk.itemgenerator.common.version.FeatureSupport;
import ua.valeriishymchuk.itemgenerator.entity.UsageEntity.ClickAt;
import ua.valeriishymchuk.itemgenerator.entity.UsageEntity.ClickButton;
import ua.valeriishymchuk.itemgenerator.entity.UsageEntity.ClickType;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@ConfigSerializable
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfigEntity {



    Map<String, CustomItem> items = Function0.of(() -> {
        RawItem exampleItem = new RawItem(
                Material.valueOf("DIAMOND"),
                "<red><bold>Cool diamond",
                Arrays.asList("<green>First lore", "<red>Second lore"),
                FeatureSupport.CMD_SUPPORT ? 1 : null,
                Arrays.asList(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS),
                io.vavr.collection.HashMap.of(serializeEnchantment(Enchantment.LUCK), 1).toJavaMap()
        );
        UsageEntity singleCommandUsage = UsageEntity.DEFAULT
                .withCommands(deserializeCommands(
                        "[console] msg %player% hi, you clicked item"
                ));
        UsageEntity multipleCommandUsage = UsageEntity.DEFAULT
                .withCommands(deserializeCommands(
                        "[console] msg %player% first message",
                        "[console] msg %player% second message",
                        "[console] msg %player% third message"
                ));
        UsageEntity example3 = multipleCommandUsage
                .withCooldownFreezeTimeMillis(TimeTokenParser.parse("1s"))
                .withCooldownMillis(TimeTokenParser.parse("5s"))
                .withOnCooldown(deserializeCommands("[console] msg %player% wait %time_s.2f"));
        UsageEntity example4 = example3.withCommands(deserializeCommands(
                "[console] msg %player% you used item",
                "[player] open_some_menu"
        )).withOnCooldown(deserializeCommands(
                "[console] msg %player% wait %time_s.2f",
                "[player] open_some_menu"
        )).withPredicates(Collections.singletonList(ClickButton.LEFT.asType()))
                .withCancel(false);
        UsageEntity example5 = UsageEntity.DEFAULT
                .withPredicates(Collections.singletonList(ClickAt.PLAYER.asType()))
                .withCooldownMillis(TimeTokenParser.parse("3h5m3s200"));
        UsageEntity example6 = example5
                .withPredicates(Collections.singletonList(new ClickType(ClickButton.LEFT, ClickAt.AIR )));
        UsageEntity example7 = example6
                .withCooldownMillis(0);
        UsageEntity example8 = example7
                .withPredicates(Arrays.asList(
                        new ClickType(ClickButton.LEFT, ClickAt.AIR),
                        ClickAt.BLOCK.asType()
                ));
        return io.vavr.collection.HashMap.of(
                "item0", CustomItem.of(exampleItem, Collections.emptyList()),
                "item1", CustomItem.of(exampleItem, Collections.singletonList(singleCommandUsage)),
                "item2", CustomItem.of(exampleItem, Collections.singletonList(multipleCommandUsage)),
                "item3", CustomItem.of(exampleItem, Collections.singletonList(example3)),
                "item4", CustomItem.of(exampleItem, Collections.singletonList(example4)),
                "item5", CustomItem.of(exampleItem, Collections.singletonList(example5)),
                "item6", CustomItem.of(exampleItem, Collections.singletonList(example6)),
                "item7", CustomItem.of(exampleItem, Collections.singletonList(example7)),
                "item8", CustomItem.of(exampleItem, Collections.singletonList(example8)),
                "item9", CustomItem.of(exampleItem, Arrays.asList(example8, example8.withPredicates(Collections.singletonList(ClickAt.PLAYER.asType()))))
        ).toJavaMap();
    }).get();

    String placeholderUpdatePeriod = "10t";

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

    public List<String> getItemKeys() { return new ArrayList<>(items.keySet()); }

    public Option<CustomItem> getItem(String key) { return Option.of(items.get(key)); }

    public void updateItem(ItemStack itemStack, Player player) {
        String customItemId = NBTCustomItem.getCustomItemId(itemStack).getOrNull();
        if (customItemId == null) return;
        CustomItem customItem = items.get(customItemId);
        ItemMeta configItemMeta = customItem.getItemStack().getItemMeta();
        ItemMeta meta = itemStack.getItemMeta();
        Option.of(configItemMeta.getDisplayName()).map(line -> PapiSupport.tryParse(player, line)).peek(meta::setDisplayName);
        meta.setLore(configItemMeta.getLore().stream().map(line -> PapiSupport.tryParse(player, line)).collect(Collectors.toList()));
        itemStack.setItemMeta(meta);
    }

    private static String serializeEnchantment(Enchantment enchantment) {
        if (!FeatureSupport.NAMESPACED_ENCHANTMENTS_SUPPORT) return enchantment.getName();
        return ReflectedRepresentations.Enchantment.getKyoriKey(enchantment).asString();
    }

    private static UsageEntity.Command deserializeCommand(String rawCommand) {
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

    private static String serializeCommand(UsageEntity.Command command) {
        String prepend = command.isExecuteAsConsole() ? "[console]" : "[player]";
        return prepend + " " + command.getCommand();
    }

    private static List<String> serializeCommands(List<UsageEntity.Command> commands) {
        return commands.stream().map(ConfigEntity::serializeCommand).collect(Collectors.toList());
    }

    @ConfigSerializable
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    public static class CustomItem {
        private static final Pattern COMMAND_EXECUTION_PATTERN =
                Pattern.compile("\\[(?<enum>at|button)] (?<type>.*)");
        private static final Pattern SINGLE_PREDICATE_PATTERN =
                Pattern.compile("\\[(?<sender>player|console)] (?<command>.*)");
        private static final Pattern ITEM_LINK_PATTERN = Pattern.compile("\\[(?<linktype>.+)] (?<link>.*)");

        ConfigurationNode item;
        ConfigurationNode usage;

        transient Lazy<List<UsageEntity>> usages = Lazy.of(this::parseUsages);
        transient Lazy<ItemStack> itemStack = Lazy.of(this::parseItem);

        public static CustomItem of(ItemStack item, List<UsageEntity> usages) {
            return new CustomItem(serializeItemStack(item), serializeUsages(usages));
        }

        @SneakyThrows
        public static CustomItem of(RawItem item, List<UsageEntity> usages) {
            return new CustomItem(BasicConfigurationNode.root().set(item), serializeUsages(usages));
        }

        @SneakyThrows
        private static ConfigurationNode serializeItemStack(ItemStack item) {
            ConfigurationNode node = BasicConfigurationNode.root();
            ItemMeta meta = item.getItemMeta();
            Material material = item.getType();
            RawItem rawItem = new RawItem(
                    material,
                    Option.of(meta.getDisplayName()).map(KyoriHelper::toMiniMessage).getOrNull(),
                    meta.getLore().stream().map(KyoriHelper::toMiniMessage).collect(Collectors.toList()),
                    ReflectedRepresentations.ItemMeta.tryGetCustomModelData(meta).getOrNull(),
                    new ArrayList<>(meta.getItemFlags()),
                    io.vavr.collection.HashMap.ofAll(meta.getEnchants())
                            .mapKeys(ConfigEntity::serializeEnchantment)
                            .toJavaMap()
            );
            node.set(rawItem);
            return node;
        }

        @SneakyThrows
        private static ConfigurationNode serializeUsages(List<UsageEntity> usages) {
            ConfigurationNode node = BasicConfigurationNode.root();
            if (usages.isEmpty()) return node;
            if (usages.size() == 1) {
                return serializeUsage(usages.get(0));
            }
            return node.set(usages.stream().map(ConfigEntity.CustomItem::serializeUsage).collect(Collectors.toList()));
        }

        @SneakyThrows
        private static ConfigurationNode serializeUsage(UsageEntity usage) {
            ConfigurationNode node = BasicConfigurationNode.root();
            boolean isEmpty = !usage.isCancel() && usage.getCommands().isEmpty() &&
                    usage.getOnCooldown().isEmpty() && usage.getPredicates().isEmpty();
            if (isEmpty) return null;
            boolean isCommandsOnly = !usage.getCommands().isEmpty() && usage.getOnCooldown().isEmpty()
                    && usage.getPredicates().isEmpty() && usage.getCooldownMillis() == 0 && usage.getCooldownFreezeTimeMillis() == 0;
            if (isCommandsOnly) {
                if (usage.getCommands().size() == 1) return serializeCommand(usage.getCommands().get(0));
                return node.set(usage.getCommands().stream().map(ConfigEntity.CustomItem::serializeCommand).collect(Collectors.toList()));
            }
            if (usage.getCooldownMillis() > 0)
                node.node("cooldown").set(TimeTokenParser.parse(usage.getCooldownMillis()));
            if (usage.getCooldownFreezeTimeMillis() > 0)
                node.node("freezetime").set(TimeTokenParser.parse(usage.getCooldownFreezeTimeMillis()));
            if (!usage.isCancel()) node.node("cancel").set(false);
            node.node("commands").set(serializeCommands(usage.getCommands()));
            node.node("on-cooldown").set(serializeCommands(usage.getOnCooldown()));
            node.node("predicate").set(serializePredicates(usage.getPredicates()));
            return node;
        }

        @SneakyThrows
        private static ConfigurationNode serializePredicates(List<ClickType> predicate) {
            if (predicate.isEmpty()) return null;
            if (predicate.size() == 1) {
                return serializePredicate(predicate.get(0));
            }
            ConfigurationNode node = BasicConfigurationNode.root();
            node.set(predicate.stream().map(CustomItem::serializePredicate).collect(Collectors.toList()));
            return node;
        }

        @SneakyThrows
        private static ConfigurationNode serializePredicate(ClickType clickType) {
            boolean hasAtOrSide = clickType.getAt().isDefined() ^ clickType.getSide().isDefined();
            if (hasAtOrSide) {
                boolean isAt = clickType.getAt().isDefined();
                String prepend = isAt ? "at" : "side";
                String value = clickType.getAt().map(Enum::name).orElse(clickType.getSide().map(Enum::name)).map(String::toLowerCase).get();
                ConfigurationNode node = BasicConfigurationNode.root();
                node.set("[" + prepend + "] " + value);
                return node;
            }
            ConfigurationNode node = BasicConfigurationNode.root();
            node.node("at").set(clickType.getAt().map(Enum::name).map(String::toLowerCase).get());
            node.node("side").set(clickType.getSide().map(Enum::name).map(String::toLowerCase).get());
            return node;
        }

        @SneakyThrows
        private static ConfigurationNode serializeCommands(List<UsageEntity.Command> commands) {
            if (commands.isEmpty()) return null;
            if (commands.size() == 1) return serializeCommand(commands.get(0));
            ConfigurationNode node = BasicConfigurationNode.root();
            node.set(commands.stream().map(ConfigEntity.CustomItem::serializeCommand).collect(Collectors.toList()));
            return node;
        }

        @SneakyThrows
        private static ConfigurationNode serializeCommand(UsageEntity.Command command) {
            ConfigurationNode node = BasicConfigurationNode.root();
            node.set(ConfigEntity.serializeCommand(command));
            return node;
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
                                false,
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
                ClickButton clickButton = Option.of(node.node("side").getString())
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
