package ua.valeriishymchuk.simpleitemgenerator.common.item;

import com.github.retrooper.packetevents.protocol.color.Color;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemCustomModelData;
import com.google.common.primitives.Floats;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.vavr.API;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.With;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import ua.valeriishymchuk.simpleitemgenerator.common.config.DefaultLoader;
import ua.valeriishymchuk.simpleitemgenerator.common.config.exception.InvalidConfigurationException;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.reflection.ReflectedRepresentations;
import ua.valeriishymchuk.simpleitemgenerator.common.text.StringSimilarityUtils;
import ua.valeriishymchuk.simpleitemgenerator.common.version.FeatureSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.version.SemanticVersion;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@ConfigSerializable
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@With
public class RawItem implements Cloneable {

    public static final RawItem EMPTY = new RawItem();

    String material;
    @Nullable
    String name;
    List<String> lore;
    @Setting("cmd")
    @Nullable ConfigurationNode customModelData;
    @Nullable Boolean unbreakable;
    List<String> itemFlags;
    Map<String, Integer> enchantments;
    List<ConfigurationNode> attributes;


    @Override
    public RawItem clone() {
        return withName(name);
    }

    @SneakyThrows
    public RawItem withCmd(int cmd) {
        ensureCmdSupport();
        ConfigurationNode rawCmd = DefaultLoader.yaml().createNode();
        rawCmd.set(cmd);
        return withCustomModelData(rawCmd);
    }

    @SneakyThrows
    public RawItem withCmd(float... cmds) {
        ensureModernCmdSupport();
        ConfigurationNode rawCmd = DefaultLoader.yaml().createNode();
        if (cmds.length == 0) return this;
        if (cmds.length == 1) {
            rawCmd.set(cmds[0]);
        } else rawCmd.setList(Float.class, Floats.asList(cmds));
        return withCustomModelData(rawCmd);
    }

    private static void ensureModernCmdSupport() {
        if (!FeatureSupport.MODERN_CMD_SUPPORT) throw new UnsupportedOperationException(
                "Modern custom model data is supported from >=1.21.4. Current version " + SemanticVersion.CURRENT_MINECRAFT
        );
    }

    private static void ensureCmdSupport() {
        if (!FeatureSupport.CMD_SUPPORT) throw new UnsupportedOperationException(
                "Custom model data is supported from >=1.14. Current version " + SemanticVersion.CURRENT_MINECRAFT
        );
    }

    private static int parseInt(ConfigurationNode s) throws InvalidConfigurationException {
        return Option.of(s.getString())
                .toTry(() -> new InvalidConfigurationException("Invalid custom model data"))
                .mapTry(Integer::parseInt)
                .mapFailure(API.Case(API.$(e -> e instanceof NumberFormatException),
                        e -> InvalidConfigurationException.format(
                                "Not a number: <white>%s</white>.", s.raw()
                        )
                )).get();
    }

    private static float parseFloat(ConfigurationNode s) throws InvalidConfigurationException {
        return Option.of(s.getString())
                .toTry(() -> new InvalidConfigurationException("Invalid custom model data"))
                .mapTry(Float::parseFloat)
                .mapFailure(API.Case(API.$(e -> e instanceof NumberFormatException),
                        e -> InvalidConfigurationException.format(
                                "Not a number: <white>%s</white>.", s.raw()
                        )
                )).get();
    }

    private Option<ItemCustomModelData> getCustomModelData() throws InvalidConfigurationException {
        if (customModelData == null || customModelData.isNull()) return Option.none();
        boolean isCmdSupported = FeatureSupport.CMD_SUPPORT;
        boolean isModernCmdSupported = FeatureSupport.MODERN_CMD_SUPPORT;
        ItemCustomModelData cmd;
        try {
            if (!isCmdSupported) throw new InvalidConfigurationException("Custom model data is supported from >=1.14. Current version " + SemanticVersion.CURRENT_MINECRAFT);
            boolean nodeIsList = customModelData.isList();
            boolean nodeIsMap = customModelData.isMap();
            boolean isScalar = !nodeIsList && !nodeIsMap;
            if (!isScalar && !isModernCmdSupported) throw new InvalidConfigurationException("Modern custom model data(list of floats) is supported from >=1.21.4. Current version " + SemanticVersion.CURRENT_MINECRAFT);
            if (!isModernCmdSupported) {
                cmd = new ItemCustomModelData(parseInt(customModelData));
            } else if (nodeIsList) {
                List<Float> floats = customModelData.getList(ConfigurationNode.class).stream().map(RawItem::parseFloat).collect(Collectors.toList());
                cmd = new ItemCustomModelData(floats, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
            } else if (isScalar) {
                cmd = new ItemCustomModelData(Collections.singletonList(parseFloat(customModelData)), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
            } else {
                ConfigurationNode floats = customModelData.node("floats");
                ConfigurationNode flags = customModelData.node("flags");
                ConfigurationNode strings = customModelData.node("strings");
                ConfigurationNode colors = customModelData.node("colors");

                List<Float> floatList = floats.isNull()? Collections.emptyList() : floats.isList() ? floats.getList(ConfigurationNode.class).stream().map(RawItem::parseFloat).collect(Collectors.toList()) : Collections.singletonList(parseFloat(floats));
                List<Boolean> flagList = flags.isNull()? Collections.emptyList() : flags.isList() ? flags.getList(Boolean.class) : Collections.singletonList(flags.getBoolean());
                List<String> stringList = strings.isNull()? Collections.emptyList() : strings.isList() ? strings.getList(String.class) : Collections.singletonList(strings.getString());
                List<Integer> colorList = colors.isNull()? Collections.emptyList() : colors.isList() ? colors.getList(ConfigurationNode.class).stream().map(RawItem::parseInt).collect(Collectors.toList()) : Collections.singletonList(parseInt(colors));
                cmd = new ItemCustomModelData(floatList, flagList, stringList, colorList.stream().map(Color::new).collect(Collectors.toList()));
            }
        } catch (Exception e) {
            throw InvalidConfigurationException.path("cmd", e);
        }
        return Option.some(cmd);
        //return Option.of(customModelData.getInt());
    }
    //public Option<Integer> getCustomModelData() {
    //    if (customModelData == null || customModelData.isNull()) return Option.none();
    //    return Option.of(customModelData.getInt());
    //}

    @SneakyThrows
    public List<Float> getModernCmd() {
        if (customModelData == null || customModelData.isNull()) return Collections.emptyList();
        if (customModelData.isList()) return customModelData.getList(Float.class);
        else return Collections.singletonList(customModelData.getFloat());
    }


    public RawItem replace(String placeholder, String value) {
        return withName(Option.of(name).map(s -> s.replace(placeholder, value)).getOrNull())
                .withLore(lore.stream().map(s -> s.replace(placeholder, value)).collect(Collectors.toList()));
    }

    private RawItem() {
        this(
                null,
                null,
                Collections.emptyList(),
                null,
                null,
                Collections.emptyList(),
                Collections.emptyMap(),
                Collections.emptyList()
        );
    }

    public List<ItemFlag> getFlags() throws InvalidConfigurationException {
        if (itemFlags == null || itemFlags.isEmpty()) return Collections.emptyList();
        List<ItemFlag> flags = new ArrayList<>();
        int i = 0;
        for (String flag : itemFlags) {
            String rawFlag = flag.toUpperCase();
            Try<ItemFlag> flagTry = Try.of(() -> ItemFlag.valueOf(rawFlag))
                    .mapFailure(API.Case(API.$(), e -> {
                        List<String> list = Arrays.stream(ItemFlag.values())
                                .map(ItemFlag::name)
                                .map(m -> new AbstractMap.SimpleEntry<>(
                                        m, StringSimilarityUtils.jaroDistance(m, rawFlag)
                                ))
                                .filter(m -> m.getValue() > 0.8)
                                .sorted(Comparator.comparingDouble(entry -> -entry.getValue()))
                                .limit(5)
                                .map(AbstractMap.SimpleEntry::getKey)
                                .collect(Collectors.toList());
                        return InvalidConfigurationException.format("Unknown flag <white>%s</white>. %s", flag, (!list.isEmpty() ? "Did you mean: <white>" + list : "</white>"));
                    }));
            try {
                if (flagTry.isSuccess()) flags.add(flagTry.get());
                else throw flagTry.getCause();
            } catch (Throwable e) {
                throw InvalidConfigurationException.path("item-flags, " + i, e);
            }
            i++;
        }
        return flags;
    }

    public Material getMaterial() throws InvalidConfigurationException {
        try {
            if (this.material == null) throw new InvalidConfigurationException("Property is not defined");
            String rawMaterial = material.toUpperCase();
            Try<Material> materialTry = Try.of(() -> Material.valueOf(rawMaterial))
                    .mapFailure(API.Case(API.$(), e -> {
                        List<String> list = Arrays.stream(Material.values())
                                .filter(m -> !m.isBlock())
                                .map(Material::name)
                                .filter(m -> !m.startsWith("LEGACY_"))
                                .map(m -> new AbstractMap.SimpleEntry<>(
                                        m, StringSimilarityUtils.jaroDistance(m, rawMaterial)
                                ))
                                .filter(m -> m.getValue() > 0.8)
                                .sorted(Comparator.comparingDouble(entry -> -entry.getValue()))
                                .limit(5)
                                .map(AbstractMap.SimpleEntry::getKey)
                                .collect(Collectors.toList());
                        return InvalidConfigurationException.format("Unknown material <white>%s</white>. %s", this.material, (!list.isEmpty() ? "Did you mean: <white>" + list : "</white>"));
                    }));
            if (materialTry.isFailure()) throw (InvalidConfigurationException) materialTry.getCause();
            return materialTry.get();
        } catch (Exception e) {
            throw InvalidConfigurationException.path("material", e);
        }
    }

    private ItemStack applyModernCmd(ItemStack stack, ItemCustomModelData cmd) {
        com.github.retrooper.packetevents.protocol.item.ItemStack peStack = SpigotConversionUtil.fromBukkitItemStack(stack);
        peStack.setComponent(ComponentTypes.CUSTOM_MODEL_DATA_LISTS, cmd);
        return SpigotConversionUtil.toBukkitItemStack(peStack);
    }

    public ItemStack bake() throws InvalidConfigurationException {
        ItemStack preparedItem = new ItemStack(getMaterial());
        ItemStack item;
        if (!attributes.isEmpty()) {
            AtomicInteger increment = new AtomicInteger();
            try {
                item = io.vavr.collection.List.ofAll(attributes.stream()
                                .map(attribute -> {
                                    Attribute a = Attribute.fromNode(attribute);
                                    increment.incrementAndGet();
                                    return a;
                                }))
                        .foldLeft(preparedItem, (itemStream, attribute) -> attribute.applyOnItem(itemStream));
            } catch (Throwable e) {
                throw InvalidConfigurationException.path("attributes, " + increment.get(), e);
            }
        } else item = preparedItem;
        ItemCustomModelData cmd = getCustomModelData().getOrNull();
        if (FeatureSupport.MODERN_CMD_SUPPORT) {
            if (cmd != null) {
                item = applyModernCmd(item, cmd);
            }
        }
        ItemMeta meta = item.getItemMeta();
        if (name != null) ReflectedRepresentations.ItemMeta.setDisplayName(meta, KyoriHelper.parseMiniMessage(name));
        if (!lore.isEmpty()) ReflectedRepresentations.ItemMeta.setLore(meta, lore.stream()
                .map(KyoriHelper::parseMiniMessage)
                .collect(Collectors.toList())
        );
        if (FeatureSupport.CMD_SUPPORT && !FeatureSupport.MODERN_CMD_SUPPORT) {
            if (cmd != null) setLegacyCustomModelData(meta, cmd.getLegacyId());
        }
        meta.addItemFlags(getFlags().toArray(new ItemFlag[0]));
        //if (!enchantments.isEmpty()) enchantments.forEach((k, v) -> meta.addEnchant(findEnchantment(k), v, true));
        getEnchantments().forEach((ench, level) -> meta.addEnchant(ench, level, true));
        if (unbreakable != null) ReflectedRepresentations.ItemMeta.setUnbreakable(meta, unbreakable);
        item.setItemMeta(meta);
        return item;
    }

    private Map<Enchantment, Integer> getEnchantments() throws InvalidConfigurationException {
        if (enchantments == null || enchantments.isEmpty()) return Collections.emptyMap();
        Map<Enchantment, Integer> map = new HashMap<>();
        try {
            for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
                try {
                    int level = entry.getValue();
                    String rawEnchantment = entry.getKey().toUpperCase();
                    Enchantment enchantment = Try.of(() -> ReflectedRepresentations.Enchantment.tryGetByKey(entry.getKey()))
                            .toOption()
                            .flatMap(Function.identity())
                            .getOrElse(Enchantment.getByName(rawEnchantment));
                    if (enchantment == null) {
                        List<String> list = Arrays.stream(Enchantment.values())
                                .map(e -> ReflectedRepresentations.Enchantment.tryGetKey(e).map(Key::asString)
                                        .getOrElse(e.getName()))
                                .map(e -> new AbstractMap.SimpleEntry<>(
                                        e, StringSimilarityUtils.jaroDistance(e, FeatureSupport.NAMESPACED_ENCHANTMENTS_SUPPORT? entry.getKey() : rawEnchantment)
                                ))
                                .filter(m -> m.getValue() > 0.8)
                                .sorted(Comparator.comparingDouble(e -> -e.getValue()))
                                .limit(5)
                                .map(AbstractMap.SimpleEntry::getKey)
                                .collect(Collectors.toList());
                        throw InvalidConfigurationException.format("Unknown enchantment <white>%s</white>. %s", entry.getKey(), (!list.isEmpty() ? "Did you mean: <white>" + list + "</white>" : ""));
                    }
                    map.put(enchantment, level);
                } catch (Throwable e) {
                    throw InvalidConfigurationException.path(entry.getKey(), e);
                }

            }
            return map;
        } catch (Exception e) {
            throw InvalidConfigurationException.path("enchantments", e);
        }
    }

    private Enchantment findEnchantment(String name) {
        return ReflectedRepresentations.Enchantment.tryGetByKey(name).getOrElse(() -> Enchantment.getByName(name));
    }

    @SneakyThrows
    private void setLegacyCustomModelData(ItemMeta itemMeta, int cmd) {
        ReflectedRepresentations.ItemMeta.setCustomModelData(itemMeta, cmd);
    }

}
