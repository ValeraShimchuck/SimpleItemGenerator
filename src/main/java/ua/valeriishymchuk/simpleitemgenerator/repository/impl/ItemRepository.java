package ua.valeriishymchuk.simpleitemgenerator.repository.impl;

import io.vavr.Tuple;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import ua.valeriishymchuk.simpleitemgenerator.common.config.ConfigLoader;
import ua.valeriishymchuk.simpleitemgenerator.common.config.exception.InvalidConfigurationException;
import ua.valeriishymchuk.simpleitemgenerator.common.error.ErrorVisitor;
import ua.valeriishymchuk.simpleitemgenerator.common.item.NBTCustomItem;
import ua.valeriishymchuk.simpleitemgenerator.common.item.RawItem;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.reflection.ReflectedRepresentations;
import ua.valeriishymchuk.simpleitemgenerator.common.support.PapiSupport;
import ua.valeriishymchuk.simpleitemgenerator.entity.CustomItemEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.CustomItemsStorageEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.result.ConfigLoadResultEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.result.ItemLoadResultEntity;
import ua.valeriishymchuk.simpleitemgenerator.repository.IConfigRepository;

import java.util.*;
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
    public Option<ItemStack> bakeItem(String key, @Nullable Player player) {
        CustomItemEntity customItem = getItem(key).getOrNull();
        if (customItem == null) return Option.none();
        ItemStack itemStack = customItem.getItemStack();
        NBTCustomItem.setCustomItemId(itemStack, key);
        updateItem(itemStack, player);
        return Option.some(itemStack);
    }

    @SneakyThrows
    public boolean updateItem(ItemStack itemStack, @Nullable Player player) {
        String customItemId = NBTCustomItem.getCustomItemId(itemStack).getOrNull();
        if (customItemId == null) return false;
        CustomItemEntity customItem = items.get(customItemId);
        if (customItem == null) return false;
        int configItemSignature = customItem.getSignature();
        Integer itemSignature = NBTCustomItem.getSignature(itemStack).getOrNull();
        if (itemSignature != null & !customItem.autoUpdate()) return false;
        boolean isSameSignature = itemSignature != null && itemSignature == configItemSignature;
        String lastPlayer = NBTCustomItem.getLastHolder(itemStack).getOrNull();
        String currentPlayer = Option.of(player).map(Player::getName).getOrNull();
        boolean shouldUpdateHeadTexture = customItem.getHeadTexture()
                .map(t -> t.getValue().contains("%player%")).getOrElse(false)
                && !Objects.equals(lastPlayer, currentPlayer);
        if (!customItem.hasPlaceHolders() && isSameSignature && !shouldUpdateHeadTexture) return false;
        ItemStack configItemStack = customItem.getItemStack();
        if (shouldUpdateHeadTexture) {
            customItem.getHeadTexture().get()
                    .apply(configItemStack, s -> s.replace("%player%", player == null ? "n" : player.getName()));
            NBTCustomItem.setLastHolder(configItemStack, currentPlayer);
        }
        ItemMeta configItemMeta = configItemStack.getItemMeta();
        itemStack.setType(configItemStack.getType());
        ReflectedRepresentations.ItemMeta.getDisplayName(configItemMeta)
                .map(line -> PapiSupport.tryParseComponent(player, line))
                .peek(line -> ReflectedRepresentations.ItemMeta.setDisplayName(configItemMeta, line));
        ReflectedRepresentations.ItemMeta.setLore(
                configItemMeta,
                ReflectedRepresentations.ItemMeta.getLore(configItemMeta).stream()
                        .map(line -> PapiSupport.tryParseComponent(player, line))
                        .collect(Collectors.toList())
        );
        itemStack.setItemMeta(configItemMeta);
        NBTCustomItem.setCustomItemId(itemStack, customItemId);
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
                    if (t._2.isValid() && t._2.get().isDefined()) return true;
                    if (t._2.isInvalid()) {
                        errors.add(InvalidConfigurationException.format("Error in <white>%s.yml</white>", t._1, t._2.getError()));
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
                            .map(e -> InvalidConfigurationException.format(e, "Error in file <white>%s</white>","items/" + key + ".yml"))
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
