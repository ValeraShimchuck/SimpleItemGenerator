package ua.valeriishymchuk.itemgenerator.common.item;

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
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import ua.valeriishymchuk.itemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.itemgenerator.common.reflection.ReflectedRepresentations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    @Nullable Integer cmd;
    List<ItemFlag> itemFlags;
    Map<String, Integer> enchantments;

    @Override
    public RawItem clone(){
        return withName(name);
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
                Collections.emptyList(),
                Collections.emptyMap()
        );
    }

    public ItemStack bake() {
        ItemStack item = new ItemStack(Material.valueOf(material));
        ItemMeta meta = item.getItemMeta();
        if (name != null) ReflectedRepresentations.ItemMeta.setDisplayName(meta, KyoriHelper.parseMiniMessage(name));
        if (!lore.isEmpty()) ReflectedRepresentations.ItemMeta.setLore(meta, lore.stream()
                .map(KyoriHelper::parseMiniMessage)
                .collect(Collectors.toList())
        );
        if (cmd != null) setCustomModelData(meta, cmd);
        if (!itemFlags.isEmpty()) meta.addItemFlags(itemFlags.toArray(new ItemFlag[0]));
        if (!enchantments.isEmpty()) enchantments.forEach((k, v) -> meta.addEnchant(findEnchantment(k), v, true));
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
