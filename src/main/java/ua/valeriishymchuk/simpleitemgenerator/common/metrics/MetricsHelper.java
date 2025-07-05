package ua.valeriishymchuk.simpleitemgenerator.common.metrics;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class MetricsHelper {

    private static final int PLUGIN_ID = 24157;
    private static Metrics METRICS = null;

    public static void init(JavaPlugin plugin) {
        if (METRICS != null) throw new IllegalStateException("Metrics already initialized!");
        METRICS = new Metrics(plugin, PLUGIN_ID);
    }

    public static void initTotalItemsChart(Supplier<Integer> itemAmountCallback) {
        ensureInitialized();
        METRICS.addCustomChart(new SingleLineChart("custom-items", itemAmountCallback::get));
    }

    public static void initPluginActivityChart(Supplier<Boolean> callback) {
        ensureInitialized();
        METRICS.addCustomChart(new SingleLineChart("plugin_activity", () -> callback.get()? 1 : 0));
    }

    private static void ensureInitialized() {
        if (METRICS == null) throw new IllegalStateException("Metrics not initialized!");
    }

    public static void shutdown() {
        if (METRICS != null) METRICS.shutdown();
    }

}
