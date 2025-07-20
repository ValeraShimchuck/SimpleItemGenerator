package ua.valeriishymchuk.simpleitemgenerator.common.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.command.CommandSender;
import ua.valeriishymchuk.simpleitemgenerator.common.component.WrappedComponent;

public class KyoriHelper {

    public static void sendMessage(CommandSender sender, WrappedComponent message) {
        message.send(sender);
    }

    public static void sendMessage(CommandSender sender, String minimessageText) {
        sendMessage(sender, parseMiniMessage(minimessageText));
    }

    public static BaseComponent[] convert(Component component) {
        return ComponentSerializer.parse(GsonComponentSerializer.gson().serialize(component));
    }

    public static String mimiMessageToJson(String miniMessage) {
        return toJson(parseMiniMessage(miniMessage));
    }

    public static WrappedComponent parseMiniMessage(String miniMessage) {
        Component component = MiniMessage.miniMessage().deserialize(miniMessage);
        if (component.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET)
            return new WrappedComponent(component.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        return new WrappedComponent(component);
    }

    public static String toJson(WrappedComponent component) {
        return component.asJson();
    }

    public static String toLegacy(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public static WrappedComponent fromLegacy(String legacy) {
        return new WrappedComponent(LegacyComponentSerializer.legacySection().deserialize(legacy));
    }

    public static WrappedComponent fromJson(String json) {
        return new WrappedComponent(GsonComponentSerializer.gson().deserialize(json));
    }

    public static String serializeMiniMessage(WrappedComponent component) {
        return MiniMessage.miniMessage().serialize(component.getComponent());
    }

    public static String jsonToMiniMessage(String message) {
        return serializeMiniMessage(fromJson(message));
    }

}
