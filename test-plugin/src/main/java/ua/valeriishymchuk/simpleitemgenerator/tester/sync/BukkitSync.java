package ua.valeriishymchuk.simpleitemgenerator.tester.sync;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class BukkitSync {

    Plugin plugin;


    public CompletableFuture<Void> sync(Runnable runnable) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            runnable.run();
            future.complete(null);
        });
        return future;
    }


}
