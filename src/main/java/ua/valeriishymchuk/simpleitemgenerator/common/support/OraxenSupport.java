package ua.valeriishymchuk.simpleitemgenerator.common.support;

import io.vavr.Lazy;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class OraxenSupport {

    private static final String NAME = "Oraxen";
    private static final String MAIN_PACKAGE = "io.th0rgal.oraxen";
    private static final String MAIN_API_PACKAGE;
    private static final Lazy<Class<?>> ORAXEN_ITEMS_CLASS_LAZY;
    static {
        MAIN_API_PACKAGE = MAIN_PACKAGE + ".api";
        ORAXEN_ITEMS_CLASS_LAZY = Lazy.of(() -> {
            try {
                return Class.forName(MAIN_API_PACKAGE + ".OraxenItems");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    @SneakyThrows
    public static ItemStack getItem(String key) {
        ensureEnabled();
        Class<?> nexoItemsClass = ORAXEN_ITEMS_CLASS_LAZY.get();
        Optional<ItemStack> instance = ((Optional<Object>) nexoItemsClass
                .getMethod("getOptionalItemById", String.class).invoke(null, key))
                .map(itemBuilder -> {
                    try {
                        return (ItemStack) (itemBuilder.getClass().getMethod("build").invoke(itemBuilder));
                    } catch (Throwable e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                });

        return instance
                .orElseThrow(() -> new IllegalArgumentException("Unknown item: " + key));

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
