package ua.valeriishymchuk.simpleitemgenerator.entity;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import io.vavr.API;
import io.vavr.CheckedFunction0;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.apache.commons.lang.math.LongRange;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.serialize.SerializationException;
import ua.valeriishymchuk.simpleitemgenerator.common.component.WrappedComponent;
import ua.valeriishymchuk.simpleitemgenerator.common.config.DefaultLoader;
import ua.valeriishymchuk.simpleitemgenerator.common.config.exception.InvalidConfigurationException;
import ua.valeriishymchuk.simpleitemgenerator.common.config.tools.ConfigParsingHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.cooldown.CooldownType;
import ua.valeriishymchuk.simpleitemgenerator.common.item.HeadTexture;
import ua.valeriishymchuk.simpleitemgenerator.common.item.ItemPropertyType;
import ua.valeriishymchuk.simpleitemgenerator.common.item.NBTCustomItem;
import ua.valeriishymchuk.simpleitemgenerator.common.item.RawItem;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.nbt.NBTConverter;
import ua.valeriishymchuk.simpleitemgenerator.common.support.ItemsAdderSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.support.NexoSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.support.OraxenSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.support.WorldGuardSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.text.StringSimilarityUtils;
import ua.valeriishymchuk.simpleitemgenerator.common.time.TimeTokenParser;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.Predicate;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.ClickAt;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.ClickButton;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.SlotPredicate;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ConfigSerializable
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
public class CustomItemEntity {
    public static final Pattern MINIMESSAGE_COMMAND_PLACEHOLDER = Pattern.compile("%minimessage_(?<placeholder>.+)%");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%(?<placeholder>\\S+)%");
    public static final Pattern COMMAND_EXECUTION_PATTERN =
            Pattern.compile("\\[(?<sender>player|console)] (?<command>.*)");
    private static final Pattern SINGLE_PREDICATE_PATTERN =
            Pattern.compile("\\[(?<enum>" + PredicateType.getPattern() + ")] (?<type>.*)");
    private static final Pattern ITEM_LINK_PATTERN = Pattern.compile("\\[(?<linktype>.+)] (?<link>.*)");


    ConfigurationNode item;
    ConfigurationNode usage;
    CompoundBinaryTag nbt;
    Boolean isIngredient;
    Boolean canBePutInInventory;
    Boolean removeOnDeath;
    Boolean isPlain;
    Boolean canMove;
    Boolean autoUpdate;
    Set<String> updateOnly;

    @NonFinal
    transient List<UsageEntity> usages;
    @NonFinal
    transient ItemStack itemStack;
    @NonFinal
    transient Boolean hasPlaceholders;
    @NonFinal
    transient Option<HeadTexture> headTexture;
    @NonFinal
    transient Boolean hasTick;
    @NonFinal
    transient Set<ItemPropertyType> propertiesToUpdate;

    public CustomItemEntity(
            ConfigurationNode item,
            ConfigurationNode usage,
            CompoundBinaryTag nbt,
            Boolean isIngredient,
            Boolean canBePutInInventory,
            Boolean removeOnDeath,
            Boolean isPlain,
            Boolean canMove,
            Boolean autoUpdate,
            Set<String> updateOnly
    ) {
        this.item = item;
        this.usage = usage;
        this.nbt = nbt;
        this.isIngredient = isIngredient;
        this.canBePutInInventory = canBePutInInventory;
        this.removeOnDeath = removeOnDeath;
        this.isPlain = isPlain;
        this.canMove = canMove;
        this.autoUpdate = autoUpdate;
        this.updateOnly = updateOnly;
    }


    private static ConfigurationNode createNode() {
        return DefaultLoader.yaml().createNode();
        //return createNode(ConfigurationOptions.defaults());
    }

    public Set<ItemPropertyType> getPropertiesToUpdate() {
        if (this.propertiesToUpdate == null) this.propertiesToUpdate = getPropertiesToUpdate0();
        return this.propertiesToUpdate;
    }

    private Set<ItemPropertyType> getPropertiesToUpdate0() {
        boolean isEmpty = updateOnly == null || updateOnly.isEmpty();
        if (isEmpty) return Arrays.stream(ItemPropertyType.values()).collect(Collectors.toSet());
        return updateOnly.stream().map(CustomItemEntity::parseItemProperty).collect(Collectors.toSet());
    }

    private static ItemPropertyType parseItemProperty(String raw) throws InvalidConfigurationException {
        return ConfigParsingHelper.parseEnum(
                ItemPropertyType.class,
                raw,
                "cooldown type",
                "update-only"
        ).get();
    }

    public boolean hasTick() {
        if (this.hasTick == null) this.hasTick = hasTick0();
        return this.hasTick;
    }

    public boolean hasTick0() {
        return getUsages()
                .stream()
                .flatMap(e -> e.getPredicates().stream())
                .anyMatch(p -> !p.getTimeTick().isEmpty());
    }

    public Option<HeadTexture> getHeadTexture() {
        if (headTexture == null) headTexture = getHeadTexture0();
        return headTexture;
    }


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

    public boolean autoUpdate() {
        if (autoUpdate == null) return true;
        return autoUpdate;
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

    public static CustomItemEntity of(ItemStack item, List<UsageEntity> usages) {
        return new CustomItemEntity(
                serializeItemStack(item),
                serializeUsages(usages),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    @SneakyThrows
    public static CustomItemEntity of(RawItem item, List<UsageEntity> usages) {
        return new CustomItemEntity(
                createNode().set(item),
                serializeUsages(usages),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    @SneakyThrows
    private static ConfigurationNode serializeItemStack(ItemStack item) {
        ConfigurationNode node = createNode();
        ItemMeta meta = item.getItemMeta();
        Material material = item.getType();
        RawItem rawItem = new RawItem(
                material.name(),
                WrappedComponent.displayName(meta)
                        .map(WrappedComponent::asJson)
                        .map(KyoriHelper::jsonToMiniMessage).getOrNull(),
                WrappedComponent.lore(meta).stream()
                        .map(WrappedComponent::fromRootComponent)
                        .map(WrappedComponent::asJson)
                        .map(KyoriHelper::jsonToMiniMessage).toList(),
                null,
                meta.isUnbreakable(),
                new ArrayList<>(meta.getItemFlags().stream().map(ItemFlag::name).collect(Collectors.toList())),
                io.vavr.collection.HashMap.ofAll(meta.getEnchants())
                        .mapKeys(MainConfigEntity::serializeEnchantment)
                        .toJavaMap(),
                Collections.emptyList(),
                null, // TODO add serializers later
                null,
                null
        );
        Integer cmd = meta.hasCustomModelData()? meta.getCustomModelData() : null;
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
        return node.set(usages.stream().map(CustomItemEntity::serializeUsage).map(ConfigurationNode::raw).collect(Collectors.toList()));
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
                    .map(CustomItemEntity::serializeCommand)
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
    private static ConfigurationNode serializePredicates(List<Predicate> predicate) {
        ConfigurationNode node = createNode();
        if (predicate.isEmpty()) return node;
        if (predicate.size() == 1) {
            return serializePredicate(predicate.get(0));
        }
        node.set(predicate.stream().map(CustomItemEntity::serializePredicate).map(ConfigurationNode::raw).collect(Collectors.toList()));
        return node;
    }

    @SneakyThrows
    private static ConfigurationNode serializePredicate(Predicate clickType) {
        boolean hasAtOrSide = clickType.getAt().isDefined() ^ clickType.getButton().isDefined();
        if (hasAtOrSide) {
            boolean isAt = clickType.getAt().isDefined();
            String prepend = isAt ? "at" : "button";
            String value = clickType.getAt().map(Enum::name).orElse(clickType.getButton().map(Enum::name)).map(String::toLowerCase).get();
            ConfigurationNode node = createNode();
            node.set("[" + prepend + "] " + value);
            return node;
        }
        ConfigurationNode node = createNode();
        node.node("at").set(clickType.getAt().map(Enum::name).map(String::toLowerCase).get());
        node.node("button").set(clickType.getButton().map(Enum::name).map(String::toLowerCase).get());
        return node;
    }

    @SneakyThrows
    private static ConfigurationNode serializeCommands(List<UsageEntity.Command> commands) {
        ConfigurationNode node = createNode();
        if (commands.isEmpty()) return node;
        if (commands.size() == 1) return serializeCommand(commands.get(0));
        node.set(commands.stream()
                .map(CustomItemEntity::serializeCommand)
                .map(ConfigurationNode::raw)
                .collect(Collectors.toList()));
        return node;
    }

    @SneakyThrows
    private static ConfigurationNode serializeCommand(UsageEntity.Command command) {
        ConfigurationNode node = createNode();
        node.set(MainConfigEntity.serializeCommand(command));
        return node;
    }

    public boolean hasPlaceHolders() throws InvalidConfigurationException {
        if (hasPlaceholders == null) hasPlaceholders = hasPlaceholders0();
        return hasPlaceholders;
    }

    private boolean hasPlaceholders0() throws InvalidConfigurationException {
        ItemStack item = getItemStack();
        Option<WrappedComponent> displayOpt = WrappedComponent.displayName(item.getItemMeta());
        List<WrappedComponent> lore = WrappedComponent.lore(item.getItemMeta());
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
                    if (!ItemsAdderSupport.isPluginEnabled())
                        throw new InvalidConfigurationException("Plugin ItemsAdder is not enabled!");
                    else throw new InvalidConfigurationException("Can't find ItemsAdder item <white>" + link + "</white>");
                }

            } else if (linkType.equals("oraxen")) {
                try {
                    item = OraxenSupport.getItem(link);
                } catch (Exception e) {
                    if (!OraxenSupport.isPluginEnabled())
                        throw new InvalidConfigurationException("Plugin Oraxen is not enabled!");
                    else throw new InvalidConfigurationException("Can't find Oraxen item <white>" + link + "</white>");
                }
            } else if (linkType.equals("nexo")) {
                try {
                    item = NexoSupport.getItem(link);
                } catch (Exception e) {
                    if (!NexoSupport.isPluginEnabled())
                        throw new InvalidConfigurationException("Plugin Nexo is not enabled!");
                    else throw new InvalidConfigurationException("Can't find Nexo item <white>" + link + "</white>");
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
            return List.of();
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
                parseCommands(node, true),
                CooldownType.PER_ITEM
        );
        long cooldown = parseTime(node.node("cooldown"));
        long freezeTime = parseTime(node.node("freezetime"));
        CooldownType cooldownType;
        if (node.node("cooldown-type").isNull()) cooldownType = CooldownType.PER_ITEM;
        else {
            String rawType;
            try {
                rawType = node.node("cooldown-type").getString();
            } catch (IllegalArgumentException e) {
                throw InvalidConfigurationException.path("cooldown-type", e);
            }
            if (rawType != null) cooldownType = parseCooldownType(rawType);
            else cooldownType = CooldownType.PER_ITEM;
        }
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
        List<Predicate> predicate;
        ConfigurationNode predicateNode = node.node("predicate");
        if (predicateNode.isNull()) predicate = Collections.emptyList();
        else if (predicateNode.isList()) {
            AtomicInteger increment = new AtomicInteger();
            try {
                predicate = new ArrayList<>();
                List<ConfigurationNode> predicateNodeList = predicateNode.getList(ConfigurationNode.class);
                for (ConfigurationNode prediacteNode : predicateNodeList) {
                    Predicate predicateResult = parsePredicate(prediacteNode);
                    increment.incrementAndGet();
                    predicate.add(predicateResult);
                }
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
        return new UsageEntity(predicate, cooldown, freezeTime, shouldCancelEvent, consume, onCooldown, commands, cooldownType);
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
        return MainConfigEntity.deserializeCommand(node.getString());
    }

    private static ClickAt parseClickAt(String raw) throws InvalidConfigurationException {
        return ConfigParsingHelper.parseEnum(ClickAt.class, raw, "at").get();
    }

    private static CooldownType parseCooldownType(String raw) throws InvalidConfigurationException {
        return ConfigParsingHelper.parseEnum(
                CooldownType.class,
                raw,
                "cooldown type",
                "cooldown-type"
        ).get();
    }

    private static ClickButton parseClickButton(String raw) throws InvalidConfigurationException {
        return ConfigParsingHelper.parseEnum(ClickButton.class, raw, "button").get();
    }

    private static List<String> parsePermissions(String raw) throws InvalidConfigurationException {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private static int parseInt(String raw) throws InvalidConfigurationException {
        return Try.ofSupplier(() -> Integer.parseInt(raw.trim())).getOrElseThrow(x -> InvalidConfigurationException.format("Invalid integer: <white>%s</white>", raw));
    }

    private static Tuple2<String, Boolean> parseStateFlag(String raw) throws InvalidConfigurationException {
        Pattern pattern = Pattern.compile("^(?<flag>[a-z\\-\\d]+)(?::(?<value>true|false))?$");
        Matcher matcher = pattern.matcher(raw.trim());
        if (!matcher.matches())
            throw InvalidConfigurationException.format("Invalid state flag predicate format: <white>%s</white>.\n" +
                    "Should be something like <white><wg-flag></white>[:<white><true|false></white>]", raw);
        String flag = matcher.group("flag");
        WorldGuardSupport.ensureStateFlagIsValid(flag);
        boolean value = matcher.group("value") == null || matcher.group("value").equals("true");
        return new Tuple2<>(flag, value);
    }

    private Predicate parsePredicate(ConfigurationNode node) throws InvalidConfigurationException, SerializationException {
        if (node.isMap()) {
            ClickAt clickAt = Option.of(node.node("at").getString())
                    .map(CustomItemEntity::parseClickAt)
                    .getOrNull();
            ClickButton clickButton = Option.of(node.node("button").getString())
                    .map(CustomItemEntity::parseClickButton)
                    .getOrNull();
            Predicate.Amount amount = new Predicate.Amount(
                    Option.of(node.node("total-amount").get(Integer.class))
                            .getOrElse(node.node("total_amount").get(Integer.class)),
                    node.node("amount").get(Integer.class)
            );
            Map<String, Boolean> flags = new HashMap<>();
            String flagsNodePath = "state-flag";
            ConfigurationNode flagsNode = node.node(flagsNodePath);
            if (flagsNode.isNull()) {
                flagsNodePath = "state_flag";
                flagsNode = node.node(flagsNodePath);
            }
            if (flagsNode.isMap()) {
                flagsNode.childrenMap().forEach((key, value) -> {
                    String flag = key.toString();
                    flags.put(flag, wrapErrorSupply(() -> {
                        WorldGuardSupport.ensureStateFlagIsValid(flag);
                        return value.getBoolean(true);
                    }, "state-flag, " + flag));
                });
            }
            List<String> permissions = node.node("permission").getList(String.class);
            List<Integer> ticks = wrapErrorSupply(() -> {
                ConfigurationNode timeNode = node.node("time");
                if (timeNode.isList()) {
                    return timeNode.getList(String.class).stream().flatMap(s -> parseTime(s).stream()).distinct()
                            .collect(Collectors.toList());
                } else if (!timeNode.isNull() && !timeNode.isMap()) {
                    return parseTime(timeNode.getString());
                }
                return null;
            }, "time");

            SlotPredicate slots = wrapErrorSupply(() -> {
                ConfigurationNode slotNode = node.node("slot");
                if (slotNode.isList()) {
                    return SlotPredicate.union(
                            slotNode.getList(String.class).stream().map(this::parseSlots)
                                    .distinct().collect(Collectors.toList())
                    );
                } else if (!slotNode.isNull() && !slotNode.isMap()) {
                    return parseSlots(slotNode.getString());
                }
                return null;
            }, "slot");


            SlotPredicate prevSlots = wrapErrorSupply(() -> {
                ConfigurationNode slotNode = node.node("prev_slot");
                if (slotNode.isList()) {
                    return SlotPredicate.union(
                            slotNode.getList(String.class).stream().map(this::parseSlots)
                                    .distinct().collect(Collectors.toList())
                    );
                } else if (!slotNode.isNull() && !slotNode.isMap()) {
                    return parseSlots(slotNode.getString());
                }
                return null;
            }, "prev_slot");

            return new Predicate(clickButton, clickAt, flags, amount, permissions, ticks, slots, prevSlots);
        }
        Matcher matcher = SINGLE_PREDICATE_PATTERN.matcher(node.getString());
        if (!matcher.find())
            throw InvalidConfigurationException.format("Invalid predicate: <white>%s</white>", node.getString());
        String value = matcher.group("type");
        PredicateType predicateType = PredicateType.fromString(matcher.group("enum"));
        ClickAt clickAt = null;
        ClickButton clickButton = null;
        Predicate.Amount amount = null;
        List<String> permissions = null;
        Map<String, Boolean> flags = null;
        List<Integer> ticks = null;
        SlotPredicate slots = null;
        SlotPredicate prevSlots = null;
        switch (predicateType) {
            case AT:
                clickAt = parseClickAt(value);
                break;
            case BUTTON:
                clickButton = parseClickButton(value);
                break;
            case AMOUNT:
                amount = new Predicate.Amount(null, parseInt(value));
                break;
            case TOTAL_AMOUNT:
                amount = new Predicate.Amount(parseInt(value), null);
                break;
            case PERMISSION:
                permissions = parsePermissions(value);
                break;
            case STATE_FLAG:
                Tuple2<String, Boolean> flag = parseStateFlag(value);
                flags = Collections.singletonMap(flag._1, flag._2);
                break;
            case TIME:
                ticks = parseTime(value);
                break;
            case SLOT:
                slots = parseSlots(value);
                break;
            case PREV_SLOT:
                prevSlots = parseSlots(value);
                break;
        }
        return new Predicate(clickButton, clickAt, flags, amount, permissions, ticks, slots, prevSlots);
    }

    private <T> T wrapErrorSupply(CheckedFunction0<T> action, String path) {
        try {
            return action.apply();
        } catch (Throwable e) {
            throw InvalidConfigurationException.path(path, e);
        }
    }

    private SlotPredicate parseSlot(String raw) {
        String[] split = raw.split("-");
        String first = split[0];
        boolean isNegate = first.startsWith("!");
        String slot = isNegate ? first.substring(1) : first;
        if (split.length == 1) {
            return Try.ofSupplier(() -> SlotPredicate.slot(isNegate, Integer.parseInt(slot)))
                    .recover(t -> {
                        SlotPredicate equipmentSlot = parseEquipmentSlot(slot);
                        return isNegate ? SlotPredicate.negate(equipmentSlot) : equipmentSlot;
                    } ).get();
        } else {
            int start = parseInt(slot);
            int end = parseInt(split[1]);
            return SlotPredicate.range(isNegate, start, end);
        }
    }

    private SlotPredicate parseSlots(String raw) {
        String[] split = raw.split(",");
        return SlotPredicate.union(
                Arrays.stream(split)
                        .map(this::parseSlot)
                        .collect(Collectors.toList())
        );
    }

    private SlotPredicate parseEquipmentSlot(String raw) throws InvalidConfigurationException {
        return ConfigParsingHelper.parseEnumWithoutPath(
                SlotGroup.class,
                raw,
                "equipment slot"
        ).map(SlotGroup::getSlotPredicate).get();
    }

    private List<Integer> parseTime(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .flatMap(element -> {
                    String[] split = element.split("-");
                    if (split.length == 1) {
                        return Stream.of(TimeTokenParser.parse(split[0]))
                                .map(millis -> millis / 50)
                                .map(l -> (int) (long) l);
                    } else {
                        long start = TimeTokenParser.parse(split[0]) / 50;
                        long end = TimeTokenParser.parse(split[1]) / 50;
                        return Arrays.stream(new LongRange(start, end).toArray())
                                .mapToObj(l -> (int) l);
                    }
                }).map(i -> Math.max(1, i))
                .distinct()
                .collect(Collectors.toList());
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @Getter
    @RequiredArgsConstructor
    private enum SlotGroup {
        HAND(SlotPredicate.equipment(EquipmentSlot.HAND)),
        OFF_HAND(SlotPredicate.equipment(EquipmentSlot.valueOf("OFF_HAND"))),
        ANY_HAND(SlotPredicate.union(HAND.slotPredicate, OFF_HAND.slotPredicate)),
        ANY(SlotPredicate.any()),
        HEAD(SlotPredicate.equipment(EquipmentSlot.HEAD)),
        CHEST(SlotPredicate.equipment(EquipmentSlot.CHEST)),
        LEGS(SlotPredicate.equipment(EquipmentSlot.LEGS)),
        FEET(SlotPredicate.equipment(EquipmentSlot.FEET)),
        ARMOR(SlotPredicate.union(HEAD.slotPredicate, CHEST.slotPredicate, LEGS.slotPredicate, FEET.slotPredicate)),
        HOTBAR(SlotPredicate.range(0, 8)),
        ;
        SlotPredicate slotPredicate;


    }

    private enum PredicateType {
        AT,
        BUTTON,
        AMOUNT,
        TOTAL_AMOUNT,
        PERMISSION,
        STATE_FLAG,
        TIME,
        SLOT,
        PREV_SLOT;

        public static PredicateType fromString(String raw) throws InvalidConfigurationException {
            return ConfigParsingHelper.parseEnumWithoutPath(
                    PredicateType.class,
                    raw,
                    "predicate type"
            ).get();
        }

        public static String getPattern() {
            return Arrays.stream(values())
                    .map(p -> p.name().toLowerCase())
                    .reduce((s1, s2) -> s1 + "|" + s2)
                    .get();
        }

    }

}
