package ua.valeriishymchuk.simpleitemgenerator.common.message;

import ua.valeriishymchuk.libs.net.kyori.adventure.text.Component;
import ua.valeriishymchuk.libs.net.kyori.adventure.text.format.TextDecoration;
import ua.valeriishymchuk.libs.net.kyori.adventure.text.minimessage.MiniMessage;
import ua.valeriishymchuk.libs.net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import ua.valeriishymchuk.libs.net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public class KyoriHelper {

    public static void sendMessage(CommandSender sender, Component message) {
        sender.sendMessage(convert(message));
    }

    public static void sendMessage(CommandSender sender, String minimessageText) {
        sendMessage(sender, parseMiniMessage(minimessageText));
    }


    public static String mimiMessageToJson(String miniMessage) {
        return toJson(parseMiniMessage(miniMessage));
    }

    public static net.kyori.adventure.text.Component convert(Component component) {
        String str = GsonComponentSerializer.gson().serialize(component);
        return net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().deserialize(str);
    }

    public static Component convert(net.kyori.adventure.text.Component component) {
        String str = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(component);
        return GsonComponentSerializer.gson().deserialize(str);
    }

    public static Component parseMiniMessage(String miniMessage) {
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
