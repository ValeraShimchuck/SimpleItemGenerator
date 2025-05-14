package ua.valeriishymchuk.simpleitemgenerator.common.slot;

import io.vavr.control.Option;
import lombok.experimental.FieldDefaults;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import static org.bukkit.inventory.EquipmentSlot.*;


public class EquipmentToSlotConverter {

    public static int convert(EquipmentSlot equipmentSlot, int handSlot) {
        if (equipmentSlot.name().equals("OFF_HAND")) {
            return 40;
        }
        switch (equipmentSlot) {
            case HAND:
                return handSlot;
            case FEET:
                return 36;
            case LEGS:
                return 37;
            case CHEST:
                return 38;
            case HEAD:
                return 39;
            default:
                throw new IllegalStateException("Unknown equipment slot: " + equipmentSlot);
        }
    }

    public static Option<EquipmentSlot> convert(int slot, Player player) {
        switch (slot) {
            case 40:
                return Option.some(EquipmentSlot.valueOf("OFF_HAND"));
            case 36:
                return Option.some(FEET);
            case 37:
                return Option.some(LEGS);
            case 38:
                return Option.some(CHEST);
            case 39:
                return Option.some(HEAD);
            default:
                if (player.getInventory().getHeldItemSlot() == slot) return Option.of(HAND);
                return Option.none();
        }
    }

}
