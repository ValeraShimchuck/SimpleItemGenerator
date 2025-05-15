package ua.valeriishymchuk.simpleitemgenerator.common.item;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.inventory.ItemStack;
import ua.valeriishymchuk.simpleitemgenerator.controller.EventsController;

import java.util.Objects;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class ItemCopy {
    ItemStack realItem;
    ItemStack clone;

    public static ItemCopy from(ItemStack item) {
        if (isAir(item)) return null;
        return new ItemCopy(item, item.clone());
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ItemCopy itemCopy = (ItemCopy) o;
        return Objects.equals(realItem, itemCopy.realItem);
    }

    @Override
    public int hashCode() {
        return Objects.hash(realItem);
    }

    public static boolean isAir(ItemStack item) {
        return item == null || item.getType().name().endsWith("AIR");
    }
}
