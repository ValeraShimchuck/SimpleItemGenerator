package ua.valeriishymchuk.simpleitemgenerator.common.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import ua.valeriishymchuk.simpleitemgenerator.common.reflection.ReflectedRepresentations;

public class KyoriHelper {

    public static void sendMessage(CommandSender sender, Component message) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            player.spigot().sendMessage(convert(message));
        } else if (sender instanceof ConsoleCommandSender) {
            ReflectedRepresentations.ConsoleCommandSender.sendComponentMessage(
                    (ConsoleCommandSender) sender, message
            );
        } else sender.sendMessage(KyoriHelper.toLegacy(message));
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
