package ua.valeriishymchuk.simpleitemgenerator.common.support;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;
import ua.valeriishymchuk.simpleitemgenerator.common.component.WrappedComponent;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;

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

    public static String tryParse(@Nullable OfflinePlayer player, String text) {
        if (!isPluginEnabled()) return text;
        // even though PAPI handles null, it is probably error-prone,
        // due to the PAPI documentation not being explicit about player's nullability
        if (player == null) return text;
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    public static WrappedComponent tryParseComponent(@Nullable OfflinePlayer player, WrappedComponent text) {
        if (!isPluginEnabled()) return text;
        if (player == null) return text;
        String rawText = KyoriHelper.toJson(text);
        String output = PlaceholderAPI.setPlaceholders(player, rawText);
        return KyoriHelper.fromJson(output);
    }

}
