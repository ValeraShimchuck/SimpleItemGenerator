package ua.valeriishymchuk.simpleitemgenerator.common.item;

import com.google.common.primitives.Floats;
import de.tr7zw.changeme.nbtapi.NBT;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.With;
import lombok.experimental.FieldDefaults;
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
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.reflection.ReflectedRepresentations;
import ua.valeriishymchuk.simpleitemgenerator.common.version.FeatureSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.version.MinecraftVersion;

import java.util.*;
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
    List<ItemFlag> itemFlags;
    Map<String, Integer> enchantments;
    List<ConfigurationNode> attributes;


    @Override
    public RawItem clone(){
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
                "Modern custom model data is supported from >=1.21.4. Current version " + MinecraftVersion.CURRENT
        );
    }

    private static void ensureCmdSupport() {
        if (!FeatureSupport.CMD_SUPPORT) throw new UnsupportedOperationException(
                "Custom model data is supported from >=1.14. Current version " + MinecraftVersion.CURRENT
        );
    }

    public Option<Integer> getCustomModelData() {
        if (customModelData == null || customModelData.isNull()) return Option.none();
        return Option.of(customModelData.getInt());
    }

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
                "DIAMOND",
                null, Collections.emptyList(),
                null,
                null,
                Collections.emptyList(),
                Collections.emptyMap(),
                Collections.emptyList()
        );
    }

    public ItemStack bake() {
        ItemStack preparedItem = new ItemStack(Material.valueOf(material));
        ItemStack item;
        if (!attributes.isEmpty()) {
            item = io.vavr.collection.List.ofAll(attributes.stream()).map(Attribute::fromNode)
                    .foldLeft(preparedItem,(itemStream, attribute) -> attribute.applyOnItem(itemStream));
        } else item = preparedItem;
        ItemMeta meta = item.getItemMeta();
        if (name != null) ReflectedRepresentations.ItemMeta.setDisplayName(meta, KyoriHelper.parseMiniMessage(name));
        if (!lore.isEmpty()) ReflectedRepresentations.ItemMeta.setLore(meta, lore.stream()
                .map(KyoriHelper::parseMiniMessage)
                .collect(Collectors.toList())
        );
        getCustomModelData().peek(cmd -> setCustomModelData(meta, cmd));
        if (!itemFlags.isEmpty()) meta.addItemFlags(itemFlags.toArray(new ItemFlag[0]));
        if (!enchantments.isEmpty()) enchantments.forEach((k, v) -> meta.addEnchant(findEnchantment(k), v, true));
        if (unbreakable != null) ReflectedRepresentations.ItemMeta.setUnbreakable(meta, unbreakable);
        item.setItemMeta(meta);
        return item;
    }

    private Enchantment findEnchantment(String name) {
        return ReflectedRepresentations.Enchantment.tryGetByKey(name).getOrElse(() -> Enchantment.getByName(name));
    }

    @SneakyThrows
    private void setCustomModelData(ItemMeta itemMeta, int cmd) {
        ReflectedRepresentations.ItemMeta.setCustomModelData(itemMeta, cmd);
    }

}
