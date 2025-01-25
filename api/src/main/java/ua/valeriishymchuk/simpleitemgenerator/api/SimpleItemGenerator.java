package ua.valeriishymchuk.simpleitemgenerator.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public abstract class SimpleItemGenerator {

    private static SimpleItemGenerator instance = null;


    protected SimpleItemGenerator() {
        init(this);
    }


    /**
     * Gets an item from the plugin's items config.
     *
     * @param key    The key of the item
     * @param player player for whom the item will be baked, if the argument is null then item will be half-baked
     * @return The item or empty if not found
     */
    public abstract Optional<ItemStack> bakeItem(String key, @Nullable Player player);

    /**
     * Checks whether the item exists in the plugin's items config.
     *
     * @param key The key of the item
     * @return true if the item exists
     */
    public abstract boolean hasKey(String key);

    /**
     * Gets the key of the item.
     *
     * @param item The item
     * @return The key or empty if not found
     */
    public abstract Optional<String> getCustomItemKey(ItemStack item);

    /**
     * Checks whether the item is a custom item.
     *
     * @param item The item
     * @return true if the item is custom
     */
    public boolean isCustomItem(ItemStack item) {
        return getCustomItemKey(item).isPresent();
    }

    /**
     * Remakes the item with player's context.
     * Useful for packet inventories/menus, because the plugin can't reach the menu's content.
     *
     * @param item The item to be updated
     * @param player The player
     * @return true if the item was updated, else if item is not custom or wasn't updated
     * */
    public abstract boolean updateItem(ItemStack item, @Nullable Player player);

    public static SimpleItemGenerator get() {
        ensureInitialized();
        return instance;
    }

    private static void ensureInitialized() {
        if (instance == null) throw new IllegalStateException("SimpleItemGenerator not initialized!");
    }


    private static void ensureNotInitialized() {
        if (instance != null) throw new IllegalStateException("SimpleItemGenerator is already initialized!");
    }

    private static void init(SimpleItemGenerator api) {
        ensureNotInitialized();
        instance = api;
    }

}
