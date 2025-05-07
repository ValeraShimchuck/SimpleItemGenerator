package ua.valeriishymchuk.simpleitemgenerator.entity;

import io.vavr.Function0;
import io.vavr.control.Option;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import ua.valeriishymchuk.simpleitemgenerator.common.config.exception.InvalidConfigurationException;
import ua.valeriishymchuk.simpleitemgenerator.common.item.NBTCustomItem;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.reflection.ReflectedRepresentations;
import ua.valeriishymchuk.simpleitemgenerator.common.support.PapiSupport;
import ua.valeriishymchuk.simpleitemgenerator.entity.result.ItemLoadResultEntity;

import java.util.*;
import java.util.stream.Collectors;

@ConfigSerializable
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class CustomItemsStorageEntity {

    @Getter
    Map<String, CustomItemEntity> items;

    //private CustomItemsStorageEntity() {
    //    this(new HashMap<>());
    //}

    public CustomItemsStorageEntity(Function0<Map<String, CustomItemEntity>> supplier) {
        this(supplier.get());
    }

    public ItemLoadResultEntity init() {
        return init(null);
    }

    public ItemLoadResultEntity init(@Nullable String prefix) {
        String finalPrefix;
        if (prefix == null) finalPrefix = "";
        else finalPrefix = prefix + ":";
        Map<String, CustomItemEntity> validItems = new HashMap<>();
        Map<String, InvalidConfigurationException> invalidItems = new HashMap<>();
        items.forEach((key, value) -> {
            try {
                value.getUsages();
                value.getHeadTexture().peek(h -> {
                    h.apply(value.getItemStack(), s -> s);
                });
                value.getItemStack();
                validItems.put(finalPrefix + key, value);
            } catch (Exception e) {
                invalidItems.put(finalPrefix + key, InvalidConfigurationException.nestedPath(e, "items", key));
            }
        });
        return new ItemLoadResultEntity(validItems, invalidItems);
    }

    //public Option<InvalidConfigurationException> tryInit() {
        //    try {
            //        init();
            //        return Option.none();
            //    } catch (InvalidConfigurationException e) {
            //        return Option.some(e);
            //    }
        //}


    public List<String> getItemKeys() {
        return new ArrayList<>(items.keySet());
    }

    public Option<CustomItemEntity> getItem(String key) {
        return Option.of(items.get(key));
    }




}
