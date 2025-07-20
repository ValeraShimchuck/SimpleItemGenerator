package ua.valeriishymchuk.simpleitemgenerator.common.reflection;

import io.vavr.CheckedFunction0;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;

import java.util.Arrays;

public class MinecraftReflection {

    private static final String craftBukkit = CheckedFunction0.of(() -> {
                String[] split = Bukkit.getServer().getClass().getName().split("\\.");
                return String.join(".", Arrays.copyOf(split, split.length - 1));
            }
    ).unchecked().apply();

    @SneakyThrows
    public static Class<?> getCraftBukkit(String className) {
        return Class.forName(craftBukkit + "." + className);
    }

}
