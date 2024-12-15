package ua.valeriishymchuk.itemgenerator.common.support;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class PapiSupport {

    private static final String NAME = "PlaceholderAPI";

    public static boolean isPluginEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled(NAME);
    }

    public static void ensureEnabled() {
        if (!isPluginEnabled()) {
            throw new IllegalStateException("Plugin " + NAME + " is not enabled!");
        }
    }

    public static String parse(OfflinePlayer player, String text) {
        ensureEnabled();
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    public static String tryParse(OfflinePlayer player, String text) {
        if (!isPluginEnabled()) return text;
        return PlaceholderAPI.setPlaceholders(player, text);
    }

}
