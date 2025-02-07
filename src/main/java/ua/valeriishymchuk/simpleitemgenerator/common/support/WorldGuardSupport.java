package ua.valeriishymchuk.simpleitemgenerator.common.support;

import com.sk89q.worldedit.foundation.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.bukkit.RegionQuery;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import io.vavr.control.Validation;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ua.valeriishymchuk.simpleitemgenerator.common.config.exception.InvalidConfigurationException;
import ua.valeriishymchuk.simpleitemgenerator.common.reflection.ReflectionObject;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class WorldGuardSupport {

    private static final String NAME = "WorldGuard";


    public static boolean isPluginEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled(NAME);
    }

    //public static boolean canBuild(Player player, Location location) {
    //    if (!isPluginEnabled()) return true;
    //    return test(player, location, DefaultFlag.BUILD);
    //}

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
    private static Object getSessionManager() {
        try {
            return WorldGuardPlugin.inst().getSessionManager();
        } catch (Throwable e) {
            Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Method getInstance = worldGuardClass.getMethod("getInstance");
            Object instance = getInstance.invoke(null);
            Method getPlatform = worldGuardClass.getMethod("getPlatform");
            Object platform = getPlatform.invoke(instance);
            Method getSessionManager = platform.getClass().getMethod("getSessionManager");
            return getSessionManager.invoke(platform);
        }
    }

    @SneakyThrows
    private static Flag<?> getFlag(String flagName) {
        try {
            return DefaultFlag.fuzzyMatchFlag(flagName);
        } catch (Throwable e) {
            Class<?> flagsClass = Class.forName("com.sk89q.worldguard.protection.flags.Flags");
            Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Method getInstance = worldGuardClass.getMethod("getInstance");
            Object instance = getInstance.invoke(null);
            Method flagRegistryMethod = worldGuardClass.getMethod("getFlagRegistry");
            Object flagRegistry = flagRegistryMethod.invoke(instance);
            Method fuzzyMatchMethod = Arrays.stream(flagsClass.getMethods())
                    .filter(m -> (m.getModifiers() & Modifier.STATIC) != 0)
                    .filter(m -> m.getName().equals("fuzzyMatchFlag")).findFirst()
                    .get();
            return (Flag<?>) fuzzyMatchMethod.invoke(null, flagRegistry, flagName);
        }
    }


    @SneakyThrows
    private static boolean test(@Nullable Player player, Location location, StateFlag flag) {
        if (hasBypass(player)) return true;
        LocalPlayer localPlayer = player == null? null : WorldGuardPlugin.inst().wrapPlayer(player);
        try {
            RegionContainer regionContainer = WorldGuardPlugin.inst().getRegionContainer();
            RegionQuery query = regionContainer.createQuery();
            return query.testState(location, localPlayer, flag);
        } catch (Throwable e) {
            ReflectionObject worldGuardInstance = ReflectionObject.ofStatic(Class.forName("com.sk89q.worldguard.WorldGuard"));
            worldGuardInstance = worldGuardInstance.invokePublic("getInstance").get();
            ReflectionObject platform = worldGuardInstance.invokePublic("getPlatform").get();
            ReflectionObject regionContainer = platform.invokePublic("getRegionContainer").get();
            ReflectionObject query = regionContainer.invokePublic("createQuery").get();
            ReflectionObject bukkitAdapter = ReflectionObject.ofStatic(Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter"));
            ReflectionObject weLocation = bukkitAdapter.invokePublic("adapt", location).get();
            Class<?> stateFlagClass = StateFlag.class;
            //Object stateFlagArray =
            return (boolean) query.invokePublic(
                    "testState",
                    weLocation,
                    new ReflectionObject(LocalPlayer.class, localPlayer),
                    new ReflectionObject(Class.forName("[Lcom.sk89q.worldguard.protection.flags.StateFlag;"), new StateFlag[]{flag})
            ).get().getObject();

        }
    }

    @SneakyThrows
    private static boolean hasBypass(Player player) {
        ReflectionObject sessionManager = ReflectionObject.of(getSessionManager());
        try {
            return (boolean) sessionManager.invokePublic("hasBypass", player, player.getWorld())
                    .get().getObject();
        } catch (Throwable e) {
            LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            ReflectionObject bukkitAdapter = ReflectionObject.ofStatic(Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter"));
            ReflectionObject weWorld = bukkitAdapter.invokePublic("adapt", player.getWorld()).get();
            return (boolean) sessionManager.invokePublic("hasBypass", ReflectionObject.of(localPlayer), weWorld).get().getObject();
        }
    }


}
