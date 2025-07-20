package ua.valeriishymchuk.simpleitemgenerator.common.component;

import io.vavr.Lazy;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;


// This class exists only to provide compile time distinction between my ney.kyori.text.Component and paper's one
// So black magic may occur
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class WrappedComponent {

    private static final Class<?> ROOT_KYORI_COMPONENT;
    private static final Class<?> ROOT_KYORI_SERIALIZER;

    static {
        try {
            ROOT_KYORI_COMPONENT = Class.forName(
                    "net,kyori,adventure,text,Component".replace(',','.'
                    ));
            ROOT_KYORI_SERIALIZER = Class.forName(
                    "net,kyori,adventure,text,serializer,gson,GsonComponentSerializer".replace(',','.'
                    ));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    public static Option<WrappedComponent> displayName(ItemMeta itemMeta) {
        Method method = itemMeta.getClass().getMethod("displayName");
        method.setAccessible(true);
        return Option.of(method.invoke(itemMeta)).map(WrappedComponent::fromRootComponent);
    }

    @SneakyThrows
    public static List<WrappedComponent> lore(ItemMeta itemMeta) {
        Method method = itemMeta.getClass().getMethod("lore");
        method.setAccessible(true);
        return  Option.of((List<Object>) method.invoke(itemMeta)).getOrElse(List.of()).stream()
                .map(WrappedComponent::fromRootComponent)
                .toList();
    }

    @SneakyThrows
    public static WrappedComponent fromRootComponent(Object rootComponent) {
        if (!ROOT_KYORI_COMPONENT.isInstance(rootComponent))
            throw new IllegalArgumentException("Not a kyori component: " + rootComponent.getClass());
        Object gsonSerializer = ROOT_KYORI_SERIALIZER.getMethod("gson").invoke(null);
        String rawComponent = (String) ROOT_KYORI_SERIALIZER.getMethod("serialize", ROOT_KYORI_COMPONENT)
                .invoke(gsonSerializer, rootComponent);
        return new WrappedComponent(GsonComponentSerializer.gson().deserialize(rawComponent));
    }

    @SneakyThrows
    public static Option<WrappedComponent> fromRootComponentNullable(@Nullable Object rootComponent) {
        if (rootComponent == null) return Option.none();
        return Option.some(fromRootComponent(rootComponent));
    }

    @Getter
    Component component;
    Lazy<String> jsonLazy = Lazy.of(this::asJson0);
    Lazy<Object> rootComponentLazy = Lazy.of(this::convertToRootComponent);

    public String asJson() {
        return jsonLazy.get();
    }

    private String asJson0() {
        return GsonComponentSerializer.gson().serialize(component);
    }

    @SneakyThrows
    private Object convertToRootComponent() {
        Object gsonSerializer = ROOT_KYORI_SERIALIZER.getMethod("gson").invoke(null);
        return ROOT_KYORI_SERIALIZER.getMethod("deserialize", Object.class).invoke(gsonSerializer, jsonLazy.get());
    }

    @SneakyThrows
    public void send(CommandSender sender) {
        sender.getClass().getMethod("sendMessage", ROOT_KYORI_COMPONENT)
                .invoke(sender, rootComponentLazy.get());
    }

    @SneakyThrows
    public void setDisplayName(ItemMeta meta) {
        Method setDisplayName = meta.getClass().getMethod("displayName", ROOT_KYORI_COMPONENT);
        setDisplayName.setAccessible(true);
        setDisplayName.invoke(meta, rootComponentLazy.get());
    }

    @SneakyThrows
    public static void setLore(ItemMeta meta, List<WrappedComponent> lore) {
        Method setLoreMethod = meta.getClass().getMethod("lore", List.class);
        setLoreMethod.setAccessible(true);
        setLoreMethod.invoke(meta, lore.stream().map(line -> line.rootComponentLazy.get()).toList());
    }


}
