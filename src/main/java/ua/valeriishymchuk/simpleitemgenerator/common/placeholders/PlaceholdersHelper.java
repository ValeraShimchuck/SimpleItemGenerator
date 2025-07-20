package ua.valeriishymchuk.simpleitemgenerator.common.placeholders;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.joml.Vector3i;
import ua.valeriishymchuk.simpleitemgenerator.common.support.PapiSupport;

import java.util.HashMap;
import java.util.Map;

public class PlaceholdersHelper {

    public static String replacePlayer(String text, Player player) {
        return PapiSupport.tryParse(player, text)
                .replace("%player%", player.getName())
                .replace("%player_x%", player.getLocation().getX() + "")
                .replace("%player_y%", player.getLocation().getY() + "")
                .replace("%player_z%", player.getLocation().getZ() + "");
    }

    public static Map<String, String> placeholdersFor(Block block, BlockFace blockFace) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%target_x%", block.getX() + "");
        placeholders.put("%target_y%", block.getY() + "");
        placeholders.put("%target_z%", block.getZ() + "");
        Vector3i placedVector = new Vector3i(block.getX(), block.getY(), block.getZ());
        placedVector.add(new Vector3i(blockFace.getModX(), blockFace.getModY(), blockFace.getModZ()));
        placeholders.put("%place_x%", placedVector.x() + "");
        placeholders.put("%place_y%", placedVector.y() + "");
        placeholders.put("%place_z%", placedVector.z() + "");
        return placeholders;
    }

    public static  Map<String, String> placeholdersFor(Player player) {
        Map<String, String> placeholders = new HashMap<>(placeholdersFor((Entity) player));
        placeholders.put("%player_target%", player.getName());
        return placeholders;
    }

    public static  Map<String, String> placeholdersFor(Entity entity) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%target_x%", entity.getLocation().getX() + "");
        placeholders.put("%target_y%", entity.getLocation().getY() + "");
        placeholders.put("%target_z%", entity.getLocation().getZ() + "");
        placeholders.put("%target_uuid%", entity.getUniqueId() + "");
        return placeholders;
    }

}
