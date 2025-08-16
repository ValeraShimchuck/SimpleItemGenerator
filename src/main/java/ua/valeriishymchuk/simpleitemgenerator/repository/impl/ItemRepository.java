package ua.valeriishymchuk.simpleitemgenerator.repository.impl;

import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemCustomModelData;
import com.google.common.collect.Multimap;
import io.vavr.Tuple;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.valeriishymchuk.libs.net.kyori.adventure.key.Key;
import ua.valeriishymchuk.libs.net.kyori.adventure.text.Component;
import ua.valeriishymchuk.simpleitemgenerator.common.annotation.UsesMinecraft;
import ua.valeriishymchuk.simpleitemgenerator.common.config.ConfigLoader;
import ua.valeriishymchuk.simpleitemgenerator.common.config.error.ConfigurationError;
import ua.valeriishymchuk.simpleitemgenerator.common.config.exception.InvalidConfigurationException;
import ua.valeriishymchuk.simpleitemgenerator.common.custommodeldata.CustomModelDataHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.error.ErrorVisitor;
import ua.valeriishymchuk.simpleitemgenerator.common.item.*;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.reflection.ReflectedRepresentations;
import ua.valeriishymchuk.simpleitemgenerator.common.support.PapiSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.version.FeatureSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.wrapper.*;
import ua.valeriishymchuk.simpleitemgenerator.entity.CustomItemEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.CustomItemsStorageEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.result.ConfigLoadResultEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.result.ItemLoadResultEntity;
import ua.valeriishymchuk.simpleitemgenerator.repository.IConfigRepository;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ItemRepository {

    IConfigRepository configRepository;
    ConfigLoader itemsConfigLoader;
    ErrorVisitor errorVisitor;
    Map<String, CustomItemEntity> items = new HashMap<>();

    public Set<String> getItemKeys() {
        return Collections.unmodifiableSet(items.keySet());
    }

    public Option<CustomItemEntity> getItem(String key) {
        return Option.of(items.get(key));
    }

    public boolean hasFolder() {
        return itemsConfigLoader.getFolder().exists();
    }

    public void createExample() {
        itemsConfigLoader.save(CustomItemsStorageEntity.class, "example", new CustomItemsStorageEntity(() -> {
            HashMap<String, CustomItemEntity> map = new HashMap<>();
            map.put(
                    "example",
                    CustomItemEntity.of(
                            RawItem.EMPTY
                                    .withMaterial(Material.STONE.name())
                                    .withName("<white><bold>I have a custom name"),
                            new ArrayList<>()
                    )
            );
            return map;
        }));
    }

    @SneakyThrows
    public List<ItemPatch> bakeItem(String key, @Nullable PlayerWrapper player) {
        CustomItemEntity customItem = getItem(key).getOrNull();
        if (customItem == null) return List.of();

        // Apply PDC patch for custom item
        ItemStack itemStack = customItem.getItemStack();
        NBTCustomItem.setCustomItemId(itemStack, key);

        return updateItem(new BukkitItemData(
                null,
                key,
                null,
                null
        ), player);
    }

    // TODO I have to create an ItemPatch data class that will be applied only in controllers
    // And ditch everything bukkit relatable in services and repositories
    // Everything else would be easy

    // Prototype, will be relocated after I am done
    public static abstract class ItemPatch implements Comparable<ItemPatch> {
        private ItemPatch() {}

        protected int getPriority() {
            return 0;
        }

        public abstract int hashCode();

        @Override
        public int compareTo(@NotNull ItemRepository.ItemPatch o) {
            return Integer.compare(getPriority(),o.getPriority());
        }

        @UsesMinecraft
        public abstract void apply(ItemStack itemStack);


        @UsesMinecraft
        public static void apply(ItemStack item, Collection<ItemPatch> patches) {
            patches.stream()
                    .sorted(Comparator.reverseOrder())
                    .forEach(patch -> patch.apply(item));
        }


        // Material make higher priority so it would appear earlier
        @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
        @RequiredArgsConstructor
        public static final class Material extends ItemPatch {

            ItemMaterialWrapper itemMaterialWrapper;

            @Override
            @UsesMinecraft
            public void apply(ItemStack itemStack) {
                itemStack.setType(itemMaterialWrapper.toBukkit());
            }

            @Override
            protected int getPriority() {
                return 1;
            }

            @Override
            public boolean equals(Object o) {
                if (o == null || getClass() != o.getClass()) return false;
                Material material = (Material) o;
                return Objects.equals(itemMaterialWrapper, material.itemMaterialWrapper);
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(itemMaterialWrapper);
            }
        }

        // Material make higher priority so it would appear earlier
        @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
        @RequiredArgsConstructor
        public static final class PotionEffects extends ItemPatch {

            List<PotionEffectWrapper> potions;

            @Override
            @UsesMinecraft
            public void apply(ItemStack itemStack) {
                ItemMeta meta = itemStack.getItemMeta();
                if (meta instanceof PotionMeta potionMeta) {
                    potionMeta.clearCustomEffects();
                    potions.stream().map(PotionEffectWrapper::toBukkit)
                            .forEach(effect -> potionMeta.addCustomEffect(effect, true));

                }
                itemStack.setItemMeta(meta);
            }

            @Override
            public boolean equals(Object o) {
                if (o == null || getClass() != o.getClass()) return false;
                PotionEffects that = (PotionEffects) o;
                return Objects.equals(potions, that.potions);
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(potions);
            }
        }


        // Damage
        @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
        @RequiredArgsConstructor
        public static final class Damage extends ItemPatch {

            int damage;

            @Override
            @UsesMinecraft
            public void apply(ItemStack itemStack) {
                ItemMeta itemMeta = itemStack.getItemMeta();
                Damageable meta = (Damageable) itemMeta;
                meta.setDamage(damage);
                itemStack.setItemMeta(itemMeta);
            }

            @Override
            public boolean equals(Object o) {
                if (o == null || getClass() != o.getClass()) return false;
                Damage damage1 = (Damage) o;
                return damage == damage1.damage;
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(damage);
            }
        }


        // Name
        @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
        @RequiredArgsConstructor
        public static final class Name extends ItemPatch {

            @Nullable Component name;

            @Override
            @UsesMinecraft
            public void apply(ItemStack itemStack) {
                ItemMeta meta = itemStack.getItemMeta();
                meta.displayName(KyoriHelper.convert(name));
                itemStack.setItemMeta(meta);
            }

            @Override
            public boolean equals(Object o) {
                if (o == null || getClass() != o.getClass()) return false;
                Name name1 = (Name) o;
                return Objects.equals(name, name1.name);
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(name);
            }
        }
        // Lore
        @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
        @RequiredArgsConstructor
        public static final class Lore extends ItemPatch {

            List<Component> lore;

            @Override
            @UsesMinecraft
            public void apply(ItemStack itemStack) {
                ItemMeta meta = itemStack.getItemMeta();
                meta.lore(lore.stream().map(KyoriHelper::convert).toList());
                itemStack.setItemMeta(meta);
            }

            @Override
            public boolean equals(Object o) {
                if (o == null || getClass() != o.getClass()) return false;
                Lore lore1 = (Lore) o;
                return Objects.equals(lore, lore1.lore);
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(lore);
            }
        }

        // Custom Model Data(should I split it in 2? For modern and legacy?)
        @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
        @RequiredArgsConstructor
        public static final class CustomModelDataLegacy extends ItemPatch {

            @Nullable Integer customModelData;

            @Override
            @UsesMinecraft
            public void apply(ItemStack itemStack) {
                ItemMeta meta = itemStack.getItemMeta();
                meta.setCustomModelData(customModelData);
                itemStack.setItemMeta(meta);
            }

            @Override
            public boolean equals(Object o) {
                if (o == null || getClass() != o.getClass()) return false;
                CustomModelDataLegacy that = (CustomModelDataLegacy) o;
                return Objects.equals(customModelData, that.customModelData);
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(customModelData);
            }

        }

        @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
        @RequiredArgsConstructor
        public static final class CustomModelDataModern extends ItemPatch {

            List<Float> floats;
            List<Boolean> flags;
            List<String> strings;
            List<Integer> colors;

            @Override
            @UsesMinecraft
            public void apply(ItemStack itemStack) {
                ItemStack newItem = CustomModelDataHelper.applyModernCmd(itemStack, new ItemCustomModelData(
                        floats,
                        flags,
                        strings,
                        colors.stream().map(com.github.retrooper.packetevents.protocol.color.Color::new).toList()
                ));
                ItemMeta meta = newItem.getItemMeta();
                itemStack.setItemMeta(meta);
            }

            @Override
            public boolean equals(Object o) {
                if (o == null || getClass() != o.getClass()) return false;
                CustomModelDataModern that = (CustomModelDataModern) o;
                return Objects.equals(floats, that.floats) &&
                        Objects.equals(flags, that.flags) &&
                        Objects.equals(strings, that.strings) &&
                        Objects.equals(colors, that.colors);
            }

            @Override
            public int hashCode() {
                return Objects.hash(floats, flags, strings, colors);
            }
        }

        // Unbreakable
        @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
        @RequiredArgsConstructor
        public static final class Unbreakable extends ItemPatch {

            boolean isUnbreakable;

            @Override
            @UsesMinecraft
            public void apply(ItemStack itemStack) {
                ItemMeta meta = itemStack.getItemMeta();
                meta.setUnbreakable(isUnbreakable);
                itemStack.setItemMeta(meta);
            }

            @Override
            public boolean equals(Object o) {
                if (o == null || getClass() != o.getClass()) return false;
                Unbreakable that = (Unbreakable) o;
                return isUnbreakable == that.isUnbreakable;
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(isUnbreakable);
            }
        }

        // Enchantments
        @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
        @RequiredArgsConstructor
        public static final class Enchantments extends ItemPatch {

            Map<EnchantmentWrapper, Integer> enchantments;

            @Override
            @UsesMinecraft
            public void apply(ItemStack itemStack) {
                ItemMeta meta = itemStack.getItemMeta();
                if (meta instanceof EnchantmentStorageMeta storageMeta) {
                    storageMeta.getStoredEnchants().keySet()
                            .forEach(storageMeta::removeStoredEnchant);
                    enchantments.forEach((enchantment, level) -> {
                        storageMeta.addStoredEnchant(enchantment.toBukkit(), level, true);
                    });
                } else {
                    meta.getEnchants().keySet().forEach(meta::removeEnchant);
                    enchantments.forEach((enchantment, level) -> {
                        meta.addEnchant(enchantment.toBukkit(), level, true);
                    });
                }
                itemStack.setItemMeta(meta);
            }

            @Override
            public boolean equals(Object o) {
                if (o == null || getClass() != o.getClass()) return false;
                Enchantments that = (Enchantments) o;
                return Objects.equals(enchantments, that.enchantments);
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(enchantments);
            }
        }


        // ItemFlags
        @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
        @RequiredArgsConstructor
        public static final class ItemFlags extends ItemPatch {

            Set<ItemFlagWrapper> itemFlags;

            @Override
            @UsesMinecraft
            public void apply(ItemStack itemStack) {
                ItemMeta meta = itemStack.getItemMeta();
                meta.removeItemFlags(meta.getItemFlags().toArray(ItemFlag[]::new));
                meta.addItemFlags(itemFlags.stream().map(ItemFlagWrapper::toBukkit).toArray(ItemFlag[]::new));
                itemStack.setItemMeta(meta);
            }

            @Override
            public boolean equals(Object o) {
                if (o == null || getClass() != o.getClass()) return false;
                ItemFlags itemFlags1 = (ItemFlags) o;
                return Objects.equals(itemFlags, itemFlags1.itemFlags);
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(itemFlags);
            }
        }


        // Attributes
        @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
        @RequiredArgsConstructor
        public static final class Attributes extends ItemPatch {

            List<ua.valeriishymchuk.simpleitemgenerator.common.item.Attribute> attributes;

            @Override
            @UsesMinecraft
            public void apply(ItemStack itemStack) {
                ItemMeta meta = AttributeApplier.applyOnItem(attributes, itemStack).getItemMeta();
                itemStack.setItemMeta(meta);
            }

            @Override
            public boolean equals(Object o) {
                if (o == null || getClass() != o.getClass()) return false;
                Attributes that = (Attributes) o;
                return Objects.equals(attributes, that.attributes);
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(attributes);
            }
        }


        // Color
        @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
        @RequiredArgsConstructor
        public static final class Color extends ItemPatch {

            @Nullable Integer color;

            @Override
            @UsesMinecraft
            public void apply(ItemStack itemStack) {
                ItemMeta meta = itemStack.getItemMeta();
                if (meta instanceof PotionMeta potionMeta) {
                    potionMeta.setColor(Option.of(color).map(org.bukkit.Color::fromRGB).getOrNull());
                } else {
                    LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta) meta;
                    leatherArmorMeta.setColor(Option.of(color).map(org.bukkit.Color::fromRGB).getOrNull());
                }
                itemStack.setItemMeta(meta);
            }

            @Override
            public boolean equals(Object o) {
                if (o == null || getClass() != o.getClass()) return false;
                Color color1 = (Color) o;
                return Objects.equals(color, color1.color);
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(color);
            }
        }

        // HeadTexture
        @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
        @RequiredArgsConstructor
        public static final class HeadTexture extends ItemPatch {

            @Nullable ua.valeriishymchuk.simpleitemgenerator.common.item.HeadTexture texture;
            UnaryOperator<String> valuePreProcessor;

            @Override
            @UsesMinecraft
            public void apply(ItemStack itemStack) {
                ItemMeta meta = HeadTextureApplier.apply(texture, itemStack, valuePreProcessor).getItemMeta();
                itemStack.setItemMeta(meta);
            }

            @Override
            public boolean equals(Object o) {
                if (o == null || getClass() != o.getClass()) return false;
                HeadTexture that = (HeadTexture) o;
                return Objects.equals(texture, that.texture);
            }

            @Override
            public int hashCode() {
                return Objects.hash(texture);
            }
        }


        // PersistentDataContainer patch - for cooldowns
        @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
        @RequiredArgsConstructor
        public static final class PersistentDataContainer extends ItemPatch {

            Key key;
            PersistentDataTypeWrapper dataTypeWrapper;
            @Nullable Object value;


            @SneakyThrows
            @Override
            @UsesMinecraft
            public void apply(ItemStack itemStack) {
                ItemMeta meta = itemStack.getItemMeta();
                org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
                if (value == null) {
                    pdc.remove(KyoriHelper.asMinecraftKey(key));
                } else {
                    PersistentDataType<?, ?> dataType = dataTypeWrapper.toBukkit();
                    Class<org.bukkit.persistence.PersistentDataContainer> pdtClass = org.bukkit.persistence.PersistentDataContainer.class;
                    Method pdtSetMethod = pdtClass.getMethod("set", NamespacedKey.class, PersistentDataType.class, Object.class);
                    pdtSetMethod.invoke(pdc, KyoriHelper.asMinecraftKey(key), dataType, value);
                }
                itemStack.setItemMeta(meta);
            }

            @Override
            public boolean equals(Object o) {
                if (o == null || getClass() != o.getClass()) return false;
                PersistentDataContainer that = (PersistentDataContainer) o;
                return Objects.equals(key, that.key) && Objects.equals(dataTypeWrapper, that.dataTypeWrapper) && Objects.equals(value, that.value);
            }

            @Override
            public int hashCode() {
                return Objects.hash(key, dataTypeWrapper, value);
            }
        }

    }


    // replace boolean with list of patches and remove ItemStack
    // For player there should be a wrapper for it

    // Move it to somewhere else later
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    public static class BukkitItemData {

        @Nullable ItemMaterialWrapper material;
        @Getter
        String customItemId;
        @Nullable Integer signature;
        @Nullable String lastHolder;

        public Option<ItemMaterialWrapper> getMaterial() {
            return Option.of(material);
        }

        public Option<String> getLastHolder() {
            return Option.of(lastHolder);
        }

        public Option<Integer> getSignature() {
            return Option.of(signature);
        }
    }

    // TODO continue refactoring, fix this method
    @SneakyThrows
    public List<ItemPatch> updateItem(BukkitItemData itemStack, @Nullable PlayerWrapper player) {
        String customItemId = itemStack.getCustomItemId();
        if (customItemId == null) return List.of();
        CustomItemEntity customItem = items.get(customItemId);
        if (customItem == null) return List.of();
        int configItemSignature = customItem.getSignature();
        Integer itemSignature = itemStack.getSignature().getOrNull();
        if (itemSignature != null & !customItem.autoUpdate()) return List.of();
        boolean isSameSignature = itemSignature != null && itemSignature == configItemSignature;
        String lastPlayer = itemStack.getLastHolder().getOrNull();
        String currentPlayer = Option.of(player).map(PlayerWrapper::getUsername).getOrNull();
        Set<ItemPropertyType> itemPropertyTypes = customItem.getPropertiesToUpdate();
        boolean shouldUpdateHeadTexture = itemPropertyTypes.contains(ItemPropertyType.HEAD_TEXTURE) &&
                customItem.getHeadTexture()
                        .map(t -> t.getValue().contains("%player%")).getOrElse(false)
                && !Objects.equals(lastPlayer, currentPlayer);
        if (!customItem.hasPlaceHolders() && isSameSignature && !shouldUpdateHeadTexture) return List.of();
        List<ItemPatch> patches = new ArrayList<>();
        ItemStack configItemStack = customItem.getItemStack();
        if (shouldUpdateHeadTexture) {
            HeadTexture headTexture = customItem.getHeadTexture().get();
            patches.add(new ItemPatch.HeadTexture(
                    headTexture,
                    s -> s.replace("%player%", player == null ? "n" : player.getUsername()))
            );
            patches.add(NBTCustomItem.setLastHolder(currentPlayer));
        }
        ItemMeta configItemMeta = configItemStack.getItemMeta();
        if (itemPropertyTypes.contains(ItemPropertyType.MATERIAL)) {
            itemStack.material
            if (itemStack.getType() != configItemStack.getType()) {
                itemStack.setType(configItemStack.getType());
            }
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemPropertyTypes.forEach(type -> {
            switch (type) {
                case MATERIAL:
                    break;
                case NAME:
                    Option.of(configItemMeta.displayName())
                            .map(KyoriHelper::convert)
                            .map(component -> PapiSupport.tryParseComponent(player, component))
                            .map(KyoriHelper::convert)
                            .peek(itemMeta::displayName);
                    break;
                case LORE:
                     itemMeta.lore(Option.of(configItemMeta.lore()).getOrElse(List.of()).stream()
                             .map(KyoriHelper::convert)
                             .map(line -> PapiSupport.tryParseComponent(player, line))
                             .map(KyoriHelper::convert)
                             .toList());
                    break;
                case CUSTOM_MODEL_DATA:
                    if (FeatureSupport.MODERN_CMD_SUPPORT) {
                        ReflectedRepresentations.ItemMeta.getModernCustomModelData(configItemMeta)
                                .peek(modernCmd -> {
                                    ReflectedRepresentations.ItemMeta.setModernCustomModelData(itemMeta, modernCmd);
                                });
                    } else {
                        if (configItemMeta.hasCustomModelData()) {
                            itemMeta.setCustomModelData(configItemMeta.getCustomModelData());
                        }
                    }
                    break;
                case UNBREAKABLE:
                    itemMeta.setUnbreakable(configItemMeta.isUnbreakable());
                    break;
                case ITEM_FLAGS:
                    Arrays.stream(ItemFlag.values()).forEach(itemMeta::removeItemFlags);
                    itemMeta.addItemFlags(configItemMeta.getItemFlags().toArray(new ItemFlag[0]));
                    break;
                case ENCHANTMENTS:
                    itemMeta.getEnchants().forEach((e, l) -> itemMeta.removeEnchant(e));
                    break;
                case ATTRIBUTES:
                    final Multimap<Attribute, AttributeModifier> oldAttributes = itemMeta.getAttributeModifiers();
                    if (oldAttributes != null) {
                        oldAttributes.keySet().forEach(itemMeta::removeAttributeModifier);
                    }
                    final Multimap<Attribute, AttributeModifier> newAttributes = configItemMeta.getAttributeModifiers();
                    if (newAttributes != null) {
                        newAttributes.forEach((itemMeta::addAttributeModifier));
                    }
                    break;
                case COLOR:
                    Color newColor;
                    if (configItemMeta instanceof LeatherArmorMeta leatherArmorMeta) {
                        newColor = leatherArmorMeta.getColor();
                    } else if (configItemMeta instanceof PotionMeta potionMeta) {
                        newColor = potionMeta.getColor();
                    } else newColor = null;

                    if (itemMeta instanceof LeatherArmorMeta leatherArmorMeta) {
                        leatherArmorMeta.setColor(
                                Option.of(newColor)
                                        .getOrElse(Bukkit.getItemFactory().getDefaultLeatherColor())
                        );
                    } else if (itemMeta instanceof PotionMeta potionMeta) {
                        potionMeta.setColor(newColor);
                    }
                    break;
                case POTION_EFFECTS:
                    if (configItemMeta instanceof PotionMeta configPotionMeta &&
                            itemMeta instanceof PotionMeta potionMeta) {
                        potionMeta.clearCustomEffects();
                        potionMeta.setBasePotionData(configPotionMeta.getBasePotionData());
                        configPotionMeta.getCustomEffects()
                                .forEach(effect -> potionMeta.addCustomEffect(effect, true));
                    }
                    break;
                case DURABILITY:
                    if (configItemMeta instanceof Damageable configDamageable &&
                            itemMeta instanceof Damageable itemDamageable) {
                        itemDamageable.setDamage(configDamageable.getDamage());
                    }
                    break;
            }
        });
        itemStack.setItemMeta(itemMeta);
        NBTCustomItem.setCustomItemId(itemStack, customItemId);
        NBTCustomItem.getSignature(configItemStack)
                .peek(signature -> NBTCustomItem.setSignature(itemStack, signature));
        return true;
    }

    public boolean reloadItems() {
        items.clear();
        ConfigLoadResultEntity mainConfigLoadResult = configRepository.getConfig().init();
        List<InvalidConfigurationException> rawErrors = new ArrayList<>();
        rawErrors.addAll(mainConfigLoadResult.getItemLoad().getInvalidItems().values());
        rawErrors.addAll(mainConfigLoadResult.getExceptions());
        List<InvalidConfigurationException> errors = rawErrors.stream()
                .map(e -> InvalidConfigurationException
                        .format(e, "Error in file <white>%s</white>", "config.yml"))
                .collect(Collectors.toList());

        items.putAll(configRepository.getConfig().getItems().getItems());


        Arrays.stream(itemsConfigLoader.getFolder().list())
                .map(f -> f.split("\\.")[0])
                .map(key -> Tuple.of(key, itemsConfigLoader.safeLoad(CustomItemsStorageEntity.class, key)))
                .filter(t -> {
                    final String configFile = t._1;
                    final Validation<ConfigurationError, Option<CustomItemsStorageEntity>> result = t._2;
                    if (result.isValid() && result.get().isDefined()) return true;
                    if (result.isInvalid()) {
                        errors.add(InvalidConfigurationException.format(
                                result.getError().asConfigException(),
                                "Error in <white>%s.yml</white>",
                                configFile)
                        );
                        return false;
                    }
                    return false;
                })
                .map(t -> t.map2(Validation::get).map2(Option::get))
                .forEach(t -> {
                    String key = t._1;
                    CustomItemsStorageEntity item = t._2;
                    ItemLoadResultEntity loadResult = item.init(key);
                    errors.addAll(loadResult.getInvalidItems().values().stream()
                            .map(e -> InvalidConfigurationException.format(e, "Error in file <white>%s</white>", "items/" + key + ".yml"))
                            .collect(Collectors.toList()));
                    loadResult.getValidItems().forEach((itemKey, itemConfig) -> {
                        items.compute(itemKey, (k, v) -> {
                            if (v != null) {
                                errors.add(InvalidConfigurationException.path(
                                        itemKey,
                                        InvalidConfigurationException.format("Duplicate item: <white>%s</white> in <white>%s.yml</white>", itemKey, key)
                                ));
                                return v;
                            } else {
                                return itemConfig;
                            }
                        });
                    });
                });
        errors.forEach(e -> {
            KyoriHelper.sendMessage(Bukkit.getConsoleSender(), "<red>[SimpleItemGenerator] Found error:</red>");
            errorVisitor.visitError(e);
        });
        return errors.isEmpty();
    }

}
