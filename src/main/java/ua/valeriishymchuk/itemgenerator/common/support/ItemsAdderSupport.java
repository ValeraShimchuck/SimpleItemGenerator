package ua.valeriishymchuk.itemgenerator.common.support;

import dev.lone.itemsadder.api.CustomStack;
import io.vavr.control.Option;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

public class ItemsAdderSupport {

    private static final String NAME = "ItemsAdder";


    public static ItemStack getItem(String key) {
        ensureEnabled();
        return Option.of(CustomStack.getInstance(key)).map(CustomStack::getItemStack)
                .getOrElseThrow(() -> new IllegalArgumentException("Unknown item: " + key));

    }

    public static void ensureEnabled() {
        if (!isPluginEnabled()) {
            throw new IllegalStateException("Plugin " + NAME + " is not enabled!");
        }
    }

    public static boolean isPluginEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled(NAME);
    }


}
