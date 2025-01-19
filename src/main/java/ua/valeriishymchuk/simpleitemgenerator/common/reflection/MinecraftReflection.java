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

    private static final Class<?> CRAFT_CHAT_MESSAGE = getCraftBukkit("util.CraftChatMessage");

    @SneakyThrows
    public static ReflectionObject toMinecraftComponent(Component component) {
        ReflectionObject craftChatMessage = ReflectionObject.ofStatic(CRAFT_CHAT_MESSAGE);
        if (craftChatMessage.hasPublicMethod("fromJSON", String.class))
            return craftChatMessage.invokePublic("fromJSON", KyoriHelper.toJson(component))
                .get();

        Class<?> nmsComponent = craftChatMessage.getClazz().getMethod("fromString", String.class).getReturnType();
        Class<?> serializer = Class.forName(nmsComponent.getComponentType().getName()+ "$ChatSerializer");
        return ReflectionObject.ofStatic(serializer)
                .invokePublic("a", KyoriHelper.toJson(component))
                .get();
    }

    public static Component fromMinecraftComponent(ReflectionObject component) {
        ReflectionObject craftChatMessage = ReflectionObject.ofStatic(CRAFT_CHAT_MESSAGE);
        String json = craftChatMessage.invokePublic("toJSON", component)
                .map(ReflectionObject::getObject)
                .map(o -> (String) o)
                .get();
        return KyoriHelper.fromJson(json);
    }





}
