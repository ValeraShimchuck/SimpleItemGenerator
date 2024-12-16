package ua.valeriishymchuk.simpleitemgenerator.common.scheduler;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class BukkitTaskScheduler {

    Plugin plugin;

    public BukkitTask runTask(Runnable runnable) {
        return Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public BukkitTask runTaskAsync(Runnable runnable) {
        return Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public BukkitTask runTaskLater(Runnable runnable, long delay) {
        return Bukkit.getServer().getScheduler().runTaskLater(plugin, runnable, delay);
    }

    public BukkitTask runTaskLaterAsynchronously(Runnable runnable, long delay) {
        return Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
    }

    public BukkitTask runTaskTimer(Runnable runnable, long delay, long period) {
        return Bukkit.getServer().getScheduler().runTaskTimer(plugin, runnable, delay, period);
    }


    public BukkitTask runTaskTimerAsynchronously(Runnable runnable, long delay, long period) {
        return Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period);
    }
}
