package ua.valeriishymchuk.simpleitemgenerator.common.message;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Contract;
import ua.valeriishymchuk.libs.net.kyori.adventure.key.Key;
import ua.valeriishymchuk.libs.net.kyori.adventure.text.Component;
import ua.valeriishymchuk.libs.net.kyori.adventure.text.format.TextDecoration;
import ua.valeriishymchuk.libs.net.kyori.adventure.text.minimessage.MiniMessage;
import ua.valeriishymchuk.libs.net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import ua.valeriishymchuk.libs.net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import ua.valeriishymchuk.simpleitemgenerator.common.annotation.UsesMinecraft;

public class KyoriHelper {

    public static void sendMessage(CommandSender sender, Component message) {
        sender.sendMessage(convert(message));
    }

    public static void sendMessage(CommandSender sender, String minimessageText) {
        sendMessage(sender, parseMiniMessage(minimessageText));
    }

    public static Key convert(net.kyori.adventure.key.Key key) {
        return Key.key(key.asString());
    }

    public static net.kyori.adventure.key.Key convert(Key key) {
        return net.kyori.adventure.key.Key.key(key.asString());
    }

    @UsesMinecraft
    public static NamespacedKey asMinecraftKey(Key key) {
        return NamespacedKey.fromString(key.asString());
    }

    public static String mimiMessageToJson(String miniMessage) {
        return toJson(parseMiniMessage(miniMessage));
    }

    @Contract("null -> null")
    public static net.kyori.adventure.text.Component convert(Component component) {
        if (component == null) return null;
        String str = GsonComponentSerializer.gson().serialize(component);
        return net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().deserialize(str);
    }

    public static Component convert(net.kyori.adventure.text.Component component) {
        String str = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(component);
        return GsonComponentSerializer.gson().deserialize(str);
    }

    @Contract("null -> null")
    public static Component parseMiniMessage(String miniMessage) {
        if (miniMessage == null) return null;
        Component component = MiniMessage.miniMessage().deserialize(miniMessage);
        if (component.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET)
            return component.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        return component;
    }

    public static String toJson(Component component) {
        return GsonComponentSerializer.gson().serialize(component);
    }

    public static String toLegacy(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public static Component fromLegacy(String legacy) {
        return LegacyComponentSerializer.legacySection().deserialize(legacy);
    }

    public static Component fromJson(String json) {
        return GsonComponentSerializer.gson().deserialize(json);
    }

    public static String serializeMiniMessage(Component component) {
        return MiniMessage.miniMessage().serialize(component);
    }

    public static String jsonToMiniMessage(String message) {
        return serializeMiniMessage(fromJson(message));
    }

}
