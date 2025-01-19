package ua.valeriishymchuk.simpleitemgenerator.tester.version;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import io.vavr.control.Option;

import java.util.function.Supplier;

public class VersionUtils {

    public static ServerVersion getCurrentVersion() {
        return PacketEvents.getAPI().getServerManager().getVersion();
    }

    public static void runOn(ServerVersion since, ServerVersion upTo, Runnable runnable) {
        if (getCurrentVersion().isNewerThanOrEquals(since) && upTo.isNewerThan(getCurrentVersion())) {
            runnable.run();
        }
    }

    public static void runSince(ServerVersion since, Runnable runnable) {
        if (getCurrentVersion().isNewerThanOrEquals(since)) {
            runnable.run();
        }
    }

    public static <T> Option<T>  applySince(ServerVersion since, Supplier<T> runnable) {
        if (getCurrentVersion().isNewerThanOrEquals(since)) {
             return Option.of(runnable.get());
        }
        return Option.none();
    }

    public static void runUpTo(ServerVersion upTo, Runnable runnable) {
        if (upTo.isNewerThan(getCurrentVersion())) {
            runnable.run();
        }
    }

}
