package ua.valeriishymchuk.simpleitemgenerator.entity;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import io.vavr.API;
import io.vavr.Function0;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.TagStringIO;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.serialize.SerializationException;
import ua.valeriishymchuk.simpleitemgenerator.common.config.DefaultLoader;
import ua.valeriishymchuk.simpleitemgenerator.common.config.exception.InvalidConfigurationException;
import ua.valeriishymchuk.simpleitemgenerator.common.item.HeadTexture;
import ua.valeriishymchuk.simpleitemgenerator.common.item.NBTCustomItem;
import ua.valeriishymchuk.simpleitemgenerator.common.item.RawItem;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.nbt.NBTConverter;
import ua.valeriishymchuk.simpleitemgenerator.common.reflection.ReflectedRepresentations;
import ua.valeriishymchuk.simpleitemgenerator.common.regex.RegexUtils;
import ua.valeriishymchuk.simpleitemgenerator.common.support.ItemsAdderSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.support.PapiSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.text.StringSimilarityUtils;
import ua.valeriishymchuk.simpleitemgenerator.common.time.TimeTokenParser;
import ua.valeriishymchuk.simpleitemgenerator.common.version.FeatureSupport;
import ua.valeriishymchuk.simpleitemgenerator.entity.UsageEntity.ClickAt;
import ua.valeriishymchuk.simpleitemgenerator.entity.UsageEntity.ClickButton;
import ua.valeriishymchuk.simpleitemgenerator.entity.UsageEntity.ClickType;

import java.io.IOException;
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
    @Getter
    boolean checkForUpdates = true;
    @Getter
    boolean sendWelcomeMessage = true;

    private static String serializeEnchantment(Enchantment enchantment) {
        if (!FeatureSupport.NAMESPACED_ENCHANTMENTS_SUPPORT) return enchantment.getName();
        return ReflectedRepresentations.Enchantment.getKyoriKey(enchantment).asString();
    }

    private static UsageEntity.Command deserializeCommand(String rawCommand) throws IllegalArgumentException {
        return prepareCommand(rawCommand).replace(command -> RegexUtils.replaceAll(
                CustomItem.MINIMESSAGE_COMMAND_PLACEHOLDER.matcher(command),
                commandMatcher -> {
                    String rawMessage = commandMatcher.group("placeholder");
                    return KyoriHelper.mimiMessageToJson(rawMessage);
                }
        ));
    }

    private static UsageEntity.Command prepareCommand(String rawCommand) throws InvalidConfigurationException {
        Matcher matcher = CustomItem.COMMAND_EXECUTION_PATTERN.matcher(rawCommand);
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

    @SneakyThrows
    public Option<ItemStack> bakeItem(String key, @Nullable Player player) {
        CustomItem customItem = getItem(key).getOrNull();
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

    public Option<CustomItem> getItem(String key) {
        return Option.of(items.get(key));
    }

    @SneakyThrows
    public void updateItem(ItemStack itemStack, @Nullable Player player) {
        String customItemId = NBTCustomItem.getCustomItemId(itemStack).getOrNull();
        if (customItemId == null) return;
        CustomItem customItem = items.get(customItemId);
        if (customItem == null) return;
        int configItemSignature = customItem.getSignature();
        Integer itemSignature = NBTCustomItem.getSignature(itemStack).getOrNull();
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
        CompoundBinaryTag nbt;
        Boolean isIngredient;
        Boolean canBePutInInventory;
        Boolean removeOnDeath;
        Boolean isPlain;
        Boolean canMove;

        @NonFinal
        transient List<UsageEntity> usages;
        @NonFinal
        transient ItemStack itemStack;
        @NonFinal
        transient Boolean hasPlaceholders;
        @NonFinal
        transient Option<HeadTexture> headTexture;

        private CustomItem() {
            this(
                    createNode(),
                    createNode(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        private static ConfigurationNode createNode() {
            return DefaultLoader.yaml().createNode();
            //return createNode(ConfigurationOptions.defaults());
        }

        public Option<HeadTexture> getHeadTexture() {
            if (headTexture == null) headTexture = getHeadTexture0();
            return headTexture;
        };

        private Option<HeadTexture> getHeadTexture0() {
            if (!item.isMap()) return Option.none();
            if (!item.hasChild("head-texture")) return Option.none();
            ConfigurationNode headTextureNode = item.node("head-texture");
            if (headTextureNode.isMap() || headTextureNode.isList())
                throw InvalidConfigurationException.nestedPath("Should be a scalar", "item", "head-texture");
            HeadTexture texture = HeadTexture.fromString(headTextureNode.getString());
            return Option.some(texture);
        }

        public boolean isIngredient() {
            if (isPlainItem()) return true;
            if (isIngredient == null) return false;
            return isIngredient;
        }

        public boolean canBePutInInventory() {
            if (isPlainItem()) return true;
            if (canBePutInInventory == null) return false;
            return canBePutInInventory;
        }

        public boolean removeOnDeath() {
            if (isPlainItem()) return false;
            if (removeOnDeath == null) return false;
            return removeOnDeath;
        }

        public boolean isPlainItem() {
            if (isPlain == null) return false;
            return isPlain;
        }

        public boolean canMove() {
            if (isPlainItem()) return true;
            if (canMove == null) return true;
            return canMove;
        }

        public static CustomItem of(ItemStack item, List<UsageEntity> usages) {
            return new CustomItem(serializeItemStack(item), serializeUsages(usages), null,null, null, null, null, null);
        }

        @SneakyThrows
        public static CustomItem of(RawItem item, List<UsageEntity> usages) {
            return new CustomItem(createNode().set(item), serializeUsages(usages), null, null, null, null, null, null);
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
                    new ArrayList<>(meta.getItemFlags().stream().map(ItemFlag::name).collect(Collectors.toList())),
                    io.vavr.collection.HashMap.ofAll(meta.getEnchants())
                            .mapKeys(ConfigEntity::serializeEnchantment)
                            .toJavaMap(),
                    Collections.emptyList(),
                    null, // TODO add serializers later
                    null
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

        public boolean hasPlaceHolders() throws InvalidConfigurationException {
            if (hasPlaceholders == null) hasPlaceholders = hasPlaceholders0();
            return hasPlaceholders;
        }

        private boolean hasPlaceholders0() throws InvalidConfigurationException {
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


        public List<UsageEntity> getUsages() throws InvalidConfigurationException {
            if (usages == null) usages = parseUsages();
            return usages;
        }

        public ItemStack getItemStack() throws InvalidConfigurationException {
            if (itemStack == null) {
                itemStack = parseItem();
                getHeadTexture().peek(h -> {
                    if (h.getValue().contains("%player%")) return;
                    itemStack = h.apply(itemStack, s -> s);
                });
                if (nbt != null) {
                    NBT.modify(itemStack, itemNbt -> {
                        ReadWriteNBT nbt2 = NBTConverter.toNBTApi(nbt);
                        itemNbt.mergeCompound(nbt2);
                    });
                }
                int signature = itemStack.serialize().hashCode();
                NBTCustomItem.setSignature(itemStack, signature);
            }
            return itemStack.clone();
        }

        public int getSignature() {
            if (itemStack == null) getItemStack();
            return NBTCustomItem.getSignature(itemStack).get();
        }

        private ItemStack parseItem() throws InvalidConfigurationException {
            try {
                if (item == null || item.isNull()) throw new InvalidConfigurationException("Property is not defined");
                if (item.isMap()) return item.get(RawItem.class).bake();
                String rawItem = item.getString();
                Matcher matcher = ITEM_LINK_PATTERN.matcher(rawItem);
                if (!matcher.find())
                    throw InvalidConfigurationException.format("Invalid item: <white>%s</white>", rawItem);
                String linkType = matcher.group("linktype");
                String link = matcher.group("link");
                ItemStack item;
                if (linkType.equals("itemsadder")) {
                    try {
                        item = ItemsAdderSupport.getItem(link);
                    } catch (Exception e) {
                        if (!ItemsAdderSupport.isPluginEnabled()) throw new InvalidConfigurationException("Plugin ItemsAdder is not enabled!");
                        else throw new InvalidConfigurationException("Can't find item <white>" + link + "</white>");
                    }

                } else {
                    throw InvalidConfigurationException.format("Invalid link type: <white>[%s]</white>[", linkType);
                }
                return item;
            } catch (Exception e) {
                throw InvalidConfigurationException.path("item", e);
            }
        }


        private List<UsageEntity> parseUsages() throws InvalidConfigurationException {
            if (usage.isNull())
                return Collections.singletonList(
                        new UsageEntity(
                                Collections.emptyList(),
                                0,
                                0,
                                true,
                                UsageEntity.Consume.NONE,
                                Collections.emptyList(),
                                Collections.emptyList()
                        )
                );
            AtomicInteger increment = new AtomicInteger();
            try {
                if (usage.isList()) {
                    List<ConfigurationNode> usages = usage.getList(ConfigurationNode.class);
                    List<UsageEntity> result = new ArrayList<>(usages.size());
                    for (ConfigurationNode node : usages) {
                        UsageEntity usage = parseUsage(node);
                        increment.incrementAndGet();
                        result.add(usage);
                    }
                    return result;
                }
            } catch (Exception e) {
                throw InvalidConfigurationException.path("usage, " + increment.get(), e);
            }

            try {
                return Collections.singletonList(parseUsage(usage));
            } catch (Exception e) {
                throw InvalidConfigurationException.path("usage", e);
            }

        }

        private static long parseTime(ConfigurationNode node) throws InvalidConfigurationException {
            if (node.isNull()) return 0;
            return Try.ofSupplier(node::getString)
                    .mapTry(TimeTokenParser::parse)
                    .getOrElseThrow(ex -> InvalidConfigurationException.path(node.key().toString(), ex));
        }

        private UsageEntity parseUsage(ConfigurationNode node) throws InvalidConfigurationException, SerializationException {
            if (!node.isMap()) return new UsageEntity(
                    Collections.emptyList(),
                    0,
                    0,
                    true,
                    UsageEntity.Consume.NONE,
                    Collections.emptyList(),
                    parseCommands(node, true)
            );
            long cooldown = parseTime(node.node("cooldown"));
            long freezeTime = parseTime(node.node("freezetime"));
            boolean shouldCancelEvent = node.node("cancel").getBoolean(true);
            ConfigurationNode consumeNode = node.node("consume");
            UsageEntity.Consume consume;
            if (consumeNode.isNull()) consume = UsageEntity.Consume.NONE;
            else if (consumeNode.isMap() || consumeNode.isList()) throw InvalidConfigurationException.path(
                    "consume",
                    new InvalidConfigurationException("Property can't be a list or map")
            );
            else {
                String rawConsume = consumeNode.getString();
                Integer number;
                try {
                    number = Integer.parseInt(rawConsume);
                } catch (NumberFormatException ignored) {
                    number = null;
                }
                boolean isNumber = number != null;
                if (isNumber) {
                    consume = new UsageEntity.Consume(UsageEntity.ConsumeType.AMOUNT, number);
                } else {
                    String upper = rawConsume.toUpperCase();
                    Set<UsageEntity.ConsumeType> allowed = io.vavr.collection.HashSet
                            .of(UsageEntity.ConsumeType.values())
                            .reject(type -> io.vavr.collection.List.of(
                                    UsageEntity.ConsumeType.AMOUNT,
                                    UsageEntity.ConsumeType.NONE
                            ).contains(type))
                            .toJavaSet();
                    UsageEntity.ConsumeType type = Try.ofSupplier(() -> UsageEntity.ConsumeType.valueOf(upper))
                            .filter(allowed::contains)
                            .mapFailure(API.Case(API.$(),
                                    e -> {
                                        List<String> suggestions = StringSimilarityUtils.getSuggestions(
                                                upper,
                                                allowed.stream()
                                                        .map(UsageEntity.ConsumeType::name)
                                        );
                                        return InvalidConfigurationException.unknownOption("consume type", rawConsume, suggestions);
                                    }
                            )).getOrElseThrow(e -> InvalidConfigurationException.path("consume", e));
                    consume = new UsageEntity.Consume(type, 0);

                }
            }
            //boolean shouldConsume = node.node("consume").getBoolean(false);
            List<ClickType> predicate;
            ConfigurationNode predicateNode = node.node("predicate");
            if (predicateNode.isNull()) predicate = Collections.emptyList();
            else if (predicateNode.isList()) {
                AtomicInteger increment = new AtomicInteger();
                try {
                    predicate = predicateNode.getList(ConfigurationNode.class).stream()
                            .map(prediacteNode -> {
                                ClickType predicateResult = parsePredicate(prediacteNode);
                                increment.incrementAndGet();
                                return predicateResult;
                            })
                            .collect(Collectors.toList());
                } catch (Exception e) {
                    throw InvalidConfigurationException.path("predicate, " + increment.get(), e);
                }
            } else {
                try {
                    predicate = Collections.singletonList(parsePredicate(predicateNode));
                } catch (Exception e) {
                    throw InvalidConfigurationException.path("predicate", e);
                }

            }
            List<UsageEntity.Command> onCooldown;
            List<UsageEntity.Command> commands;
            onCooldown = parseCommands(node.node("on-cooldown"));
            commands = parseCommands(node.node("commands"));
            return new UsageEntity(predicate, cooldown, freezeTime, shouldCancelEvent, consume, onCooldown, commands);
        }

        private List<UsageEntity.Command> parseCommands(ConfigurationNode node) throws InvalidConfigurationException {
            try {
                return parseCommands(node, false);
            } catch (SerializationException e) {
                throw new RuntimeException(e); // no point, but it exists to peace the compiler
            }
        }

        private List<UsageEntity.Command> parseCommands(ConfigurationNode node, boolean ignoreKey) throws InvalidConfigurationException, SerializationException {
            if (node.isNull()) return Collections.emptyList();
            AtomicInteger increment = new AtomicInteger();
            try {
                if (node.isList()) {
                    return node.getList(ConfigurationNode.class).stream()
                            .map(commandNode -> {
                                UsageEntity.Command command = parseCommand(commandNode);
                                increment.incrementAndGet();
                                return command;
                            })
                            .collect(Collectors.toList());
                }
            } catch (Error | RuntimeException | SerializationException e) {
                if (ignoreKey) throw e;
                throw InvalidConfigurationException.path(node.key().toString() + ", " + increment.get(), e);
            }
            try {
                return Collections.singletonList(parseCommand(node));
            } catch (Error | RuntimeException e) {
                if (ignoreKey) throw e;
                throw InvalidConfigurationException.path(node.key().toString(), e);
            }
        }


        private UsageEntity.Command parseCommand(ConfigurationNode node) throws InvalidConfigurationException {
            return deserializeCommand(node.getString());
        }

        private static ClickAt parseClickAt(String raw) throws InvalidConfigurationException {
            String upper = raw.toUpperCase();
            return Try.ofSupplier(() -> ClickAt.valueOf(upper))
                    .mapFailure(API.Case(API.$(e -> e instanceof IllegalArgumentException), e -> {
                        List<String> suggestions = StringSimilarityUtils.getSuggestions(
                                upper,
                                Arrays.stream(ClickAt.values())
                                        .map(ClickAt::name)
                        );
                        return InvalidConfigurationException.unknownOption("at", raw, suggestions);
                    })).getOrElseThrow(x -> InvalidConfigurationException.path("at", x));
        }

        private static ClickButton parseClickButton(String raw) throws InvalidConfigurationException {
            String upper = raw.toUpperCase();
            return Try.ofSupplier(() -> ClickButton.valueOf(upper))
                    .mapFailure(API.Case(API.$(e -> e instanceof IllegalArgumentException), e -> {
                        List<String> suggestions = StringSimilarityUtils.getSuggestions(
                                upper,
                                Arrays.stream(ClickButton.values())
                                        .map(ClickButton::name)
                        );
                        return InvalidConfigurationException.unknownOption("button", raw, suggestions);
                    })).getOrElseThrow(x -> InvalidConfigurationException.path("button", x));
        }

        private ClickType parsePredicate(ConfigurationNode node) throws InvalidConfigurationException {
            if (node.isMap()) {
                ClickAt clickAt = Option.of(node.node("at").getString())
                        .map(CustomItem::parseClickAt)
                        .getOrNull();
                ClickButton clickButton = Option.of(node.node("button").getString())
                        .map(CustomItem::parseClickButton)
                        .getOrNull();
                return new ClickType(clickButton, clickAt);
            }
            Matcher matcher = SINGLE_PREDICATE_PATTERN.matcher(node.getString());
            if (!matcher.find())
                throw InvalidConfigurationException.format("Invalid predicate: <white>%s</white>", node.getString());
            String type = matcher.group("type");
            boolean isAt = matcher.group("enum").equals("at");
            ClickAt clickAt = null;
            ClickButton clickButton = null;
            if (isAt) clickAt = parseClickAt(type);
            else clickButton = parseClickButton(type);
            return new ClickType(clickButton, clickAt);
        }

    }


}
