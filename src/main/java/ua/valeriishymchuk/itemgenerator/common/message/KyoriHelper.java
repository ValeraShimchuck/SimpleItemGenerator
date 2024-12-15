package ua.valeriishymchuk.itemgenerator.common.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.command.CommandSender;

public class KyoriHelper {

    public static void sendMessage(CommandSender sender, Component message) {
        sender.sendMessage(GsonComponentSerializer.gson().serialize(message));
    }

    public static String parseMiniMessage(String miniMessage) {
        return GsonComponentSerializer.gson().serialize(MiniMessage.miniMessage().deserialize(miniMessage));
    }

    public static String toMiniMessage(String message) {
        return MiniMessage.miniMessage().serialize(GsonComponentSerializer.gson().deserialize(message));
    }

}
