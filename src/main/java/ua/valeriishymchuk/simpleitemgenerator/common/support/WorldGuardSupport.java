package ua.valeriishymchuk.simpleitemgenerator.common.support;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.session.SessionManager;
import io.vavr.control.Validation;
import lombok.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ua.valeriishymchuk.simpleitemgenerator.common.config.exception.InvalidConfigurationException;


public class WorldGuardSupport {

    private static final String NAME = "WorldGuard";


    public static boolean isPluginEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled(NAME);
    }

    public enum StateTestError {
        PLUGIN_NOT_ENABLED, UNKNOWN_FLAG, NOT_A_STATE_FLAG;

        public <R> Validation<StateTestError, R> asInvalid() {
            return Validation.invalid(this);
        }
    }

    public static void ensureStateFlagIsValid(String flagName) {
        if (!isPluginEnabled()) throw new InvalidConfigurationException("Plugin WorldGuard is not enabled!");
        Flag<?> flag = getFlag(flagName);
        if (flag == null) throw new InvalidConfigurationException("Unknown state flag: " + flagName);
        if (!(flag instanceof StateFlag)) throw new InvalidConfigurationException("Flag " + flagName + " is not a state flag!");
    }

    public static Validation<StateTestError, Boolean> checkState(@Nullable Player player, Location location, String flagName) {
        if (!isPluginEnabled()) return StateTestError.PLUGIN_NOT_ENABLED.asInvalid();
        Flag<?> flag = getFlag(flagName);
        if (flag == null) return StateTestError.UNKNOWN_FLAG.asInvalid();
        if (flag instanceof StateFlag) {
            return Validation.valid(test(player, location, (StateFlag) flag));
        }
        return StateTestError.NOT_A_STATE_FLAG.asInvalid();
    }

    @SneakyThrows
    private static SessionManager getSessionManager() {
        return WorldGuard.getInstance().getPlatform().getSessionManager();
    }

    @SneakyThrows
    private static Flag<?> getFlag(String flagName) {
        return Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), flagName);
    }


    @SneakyThrows
    private static boolean test(@Nullable Player player, Location location, StateFlag flag) {
        if (hasBypass(player)) return true;
        LocalPlayer localPlayer = player == null? null : WorldGuardPlugin.inst().wrapPlayer(player);
        return WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()
                .testState(BukkitAdapter.adapt(location), localPlayer, flag);
    }

    @SneakyThrows
    private static boolean hasBypass(Player player) {
        return getSessionManager()
                .hasBypass(
                        WorldGuardPlugin.inst().wrapPlayer(player),
                        BukkitAdapter.adapt(player.getWorld())
                );
    }


}
