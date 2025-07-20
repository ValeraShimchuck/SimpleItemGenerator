package ua.valeriishymchuk.simpleitemgenerator.common.support;

import io.vavr.Lazy;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class NexoSupport {


    private static final String NAME = "Nexo";
    private static final String NEXO_PACKAGE = "com.nexomc.nexo";
    private static final String NEXO_API_PACKAGE;
    private static final Lazy<Class<?>> NEXO_ITEMS_CLASS_LAZY;
    static {
        NEXO_API_PACKAGE = NEXO_PACKAGE + ".api";
        NEXO_ITEMS_CLASS_LAZY = Lazy.of(() -> {
            try {
                return Class.forName(NEXO_API_PACKAGE + ".NexoItems");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    @SneakyThrows
    public static ItemStack getItem(String key) {
        ensureEnabled();
        Class<?> nexoItemsClass = NEXO_ITEMS_CLASS_LAZY.get();
        Optional<ItemStack> instance = ((Optional<Object>) nexoItemsClass
                .getMethod("optionalItemFromId", String.class).invoke(null, key))
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
