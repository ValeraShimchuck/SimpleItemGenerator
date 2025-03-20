package ua.valeriishymchuk.simpleitemgenerator.common.support;

import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class HeadDatabaseSupport {
    private static final String NAME = "HeadDatabase";
    private static Object API = null;

    public static ItemStack getHead(String id) {
        ensureInitialized();
        ItemStack item = getAPI().getItemHead(id);
        if (item == null) throw new IllegalArgumentException("Head not found: " + id);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(null);
        meta.setLore(null);
        item.setItemMeta(meta);
        return item;
    }

    public static String getBase64(String id) {
        ensureInitialized();
        String base64 = getAPI().getBase64(id);
        if (base64 == null) throw new IllegalArgumentException("Head not found: " + id);
        return base64;
    }

    public static HeadDatabaseAPI getAPI() {
        ensureInitialized();
        return (HeadDatabaseAPI) API;
    }

    public static void ensureEnabled() {
        if (!isPluginEnabled()) {
            throw new IllegalStateException("Plugin " + NAME + " is not enabled!");
        }
    }

    public static void ensureInitialized() {
        ensureEnabled();
        if (API == null) throw new IllegalStateException("HeadDatabaseAPI not initialized!");
    }

    public static boolean init() {
        if (API != null) throw new IllegalStateException("HeadDatabaseAPI already initialized!");
        if (!isPluginEnabled()) return false;
        API = new HeadDatabaseAPI();
        return true;
    }

    public static boolean isPluginEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled(NAME);
    }


}
