package ua.valeriishymchuk.simpleitemgenerator.common.custommodeldata;

import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemCustomModelData;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.inventory.ItemStack;

public class CustomModelDataHelper {

    public static ItemStack applyModernCmd(ItemStack stack, ItemCustomModelData cmd) {
        com.github.retrooper.packetevents.protocol.item.ItemStack peStack = SpigotConversionUtil.fromBukkitItemStack(stack);
        peStack.setComponent(ComponentTypes.CUSTOM_MODEL_DATA_LISTS, cmd);
        return SpigotConversionUtil.toBukkitItemStack(peStack);
    }

}
