package ua.valeriishymchuk.simpleitemgenerator.entity;

import io.vavr.CheckedRunnable;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import ua.valeriishymchuk.simpleitemgenerator.common.config.exception.InvalidConfigurationException;
import ua.valeriishymchuk.simpleitemgenerator.common.config.tools.ConfigParsingHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.item.RawItem;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.reflection.ReflectedRepresentations;
import ua.valeriishymchuk.simpleitemgenerator.common.regex.RegexUtils;
import ua.valeriishymchuk.simpleitemgenerator.common.time.TimeTokenParser;
import ua.valeriishymchuk.simpleitemgenerator.common.version.FeatureSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.ClickAt;
import ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate.ClickButton;
import ua.valeriishymchuk.simpleitemgenerator.common.version.SigFeatureTag;
import ua.valeriishymchuk.simpleitemgenerator.entity.result.ConfigLoadResultEntity;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;


@ConfigSerializable
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MainConfigEntity {


    @Getter
    @Setting(nodeFromParent = true)
    CustomItemsStorageEntity items = new CustomItemsStorageEntity(MainConfigEntity::initConfigDefaults);

    String placeholderUpdatePeriod = "10t";
    String itemUpdatePeriod = "1t";
    transient Lazy<Long> lazyItemUpdatePeriod = Lazy.of(this::getItemUpdatePeriod0);
    transient Lazy<Long> lazyPlaceholderUpdatePeriod = Lazy.of(this::getPlaceholderUpdatePeriod0);
    @Getter
    boolean checkForUpdates = true;
    @Getter
    boolean sendWelcomeMessage = true;
    @Getter
    boolean sendInvalidItemMessage = true;
    @Getter
    boolean debug = false;
    @Getter
    boolean tickDebug = false;
    Map<String, Boolean> features = Collections.emptyMap();
    transient Set<SigFeatureTag> lazyFeatures = null;

    public Set<SigFeatureTag> getFeatures() {
        if (lazyFeatures == null) lazyFeatures = getFeatures0();
        return lazyFeatures;
    }

    private Set<SigFeatureTag> getFeatures0() throws InvalidConfigurationException {
        Set<SigFeatureTag> featureTags = new HashSet<>();
        Arrays.stream(SigFeatureTag.values())
                .filter(SigFeatureTag::isEnabledByDefault)
                .forEach(featureTags::add);
        Map<SigFeatureTag, Boolean> map = features.entrySet().stream()
                .map(entry -> {
                    SigFeatureTag sigFeatureTag = parse(entry.getKey())
                            .mapError(InvalidConfigurationException.Lambda.path("features"))
                            .getOrElseGet(e -> {
                                throw e;
                            });
                    return Tuple.of(sigFeatureTag, entry.getValue());
                }).collect(Collectors.toMap(Tuple2::_1, Tuple2::_2));
        map.entrySet().stream()
                .filter(entry -> !entry.getValue())
                .forEach(entry -> featureTags.remove(entry.getKey()));
        map.entrySet().stream()
                .filter(Map.Entry::getValue)
                .forEach(entry -> featureTags.add(entry.getKey()));
        return featureTags;
    }

    private static Validation<InvalidConfigurationException, SigFeatureTag> parse(String value) {
        return ConfigParsingHelper.parseEnum(
                SigFeatureTag.class,
                value,
                "feature tag",
                value
        );
    }

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
        return rawCommands.stream().map(MainConfigEntity::deserializeCommand).collect(Collectors.toList());
    }

    private static List<UsageEntity.Command> deserializeCommands(String... rawCommands) {
        return deserializeCommands(Arrays.asList(rawCommands));
    }

    private static List<UsageEntity.Command> deserializeConfigCommands(List<String> rawCommands) {
        return rawCommands.stream().map(MainConfigEntity::prepareCommand).collect(Collectors.toList());
    }

    private static List<UsageEntity.Command> deserializeConfigCommands(String... rawCommands) {
        return deserializeConfigCommands(Arrays.asList(rawCommands));
    }

    public static String serializeCommand(UsageEntity.Command command) {
        String prepend = command.isExecuteAsConsole() ? "[console]" : "[player]";
        return prepend + " " + command.getCommand();
    }

    private static List<String> serializeCommands(List<UsageEntity.Command> commands) {
        return commands.stream().map(MainConfigEntity::serializeCommand).collect(Collectors.toList());
    }

    private static Map<String, CustomItemEntity> initConfigDefaults() {
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
    }

    public long getPlaceholderUpdatePeriod() {
        return lazyPlaceholderUpdatePeriod.get();
    }

    public long getPlaceholderUpdatePeriod0() {
        return TimeTokenParser.parse(placeholderUpdatePeriod);
    }

    public long getItemUpdatePeriod() {
        return lazyItemUpdatePeriod.get();
    }

    public long getItemUpdatePeriod0() {
        return TimeTokenParser.parse(itemUpdatePeriod);
    }

    // initializing lazies
    public ConfigLoadResultEntity init() throws InvalidConfigurationException {
        List<InvalidConfigurationException> configErrors = new ArrayList<>();
        initSafely(this::getFeatures).peek(configErrors::add);
        return new ConfigLoadResultEntity(items.init(), configErrors);
    }

    private Option<InvalidConfigurationException> initSafely(CheckedRunnable function) {
        try {
            function.run();
            return Option.none();
        } catch (Throwable e) {
            if (e instanceof InvalidConfigurationException) {
                return Option.of((InvalidConfigurationException) e);
            } else return Option.of(InvalidConfigurationException.unhandledException(e));
        }
    }

}
