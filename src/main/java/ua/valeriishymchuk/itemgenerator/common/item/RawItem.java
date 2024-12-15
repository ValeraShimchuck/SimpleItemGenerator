package ua.valeriishymchuk.itemgenerator.common.item;

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
import ua.valeriishymchuk.itemgenerator.common.reflection.ReflectionObject;
import ua.valeriishymchuk.itemgenerator.common.reflection.ReflectedRepresentations;
import ua.valeriishymchuk.itemgenerator.common.version.FeatureSupport;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ConfigSerializable
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@With
public class RawItem {

    public static final RawItem EMPTY = new RawItem();

    Material material;
    @Nullable
    String name;
    List<String> lore;
    @Nullable Integer cmd;
    List<ItemFlag> itemFlags;
    Map<String, Integer> enchantments;

    private RawItem() {
        this(
                Arrays.stream(Material.values()).findFirst().get(),
                null, Collections.emptyList(),
                null,
                Collections.emptyList(),
                Collections.emptyMap()
        );
    }

    public ItemStack bake() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (name != null) meta.setDisplayName(KyoriHelper.parseMiniMessage(name));
        if (!lore.isEmpty()) meta.setLore(lore.stream()
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
