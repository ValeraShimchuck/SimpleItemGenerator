package ua.valeriishymchuk.itemgenerator.common.metrics;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public class MetricsHelper {

    private static final int PLUGIN_ID = 24157;
    private static Metrics METRICS = null;

    public static void init(JavaPlugin plugin) {
        if (METRICS != null) throw new IllegalStateException("Metrics already initialized!");
        METRICS = new Metrics(plugin, PLUGIN_ID);
    }

    private static void ensureInitialized() {
        if (METRICS == null) throw new IllegalStateException("Metrics not initialized!");
    }

    public static void shutdown() {
        if (METRICS != null) METRICS.shutdown();
    }

}
