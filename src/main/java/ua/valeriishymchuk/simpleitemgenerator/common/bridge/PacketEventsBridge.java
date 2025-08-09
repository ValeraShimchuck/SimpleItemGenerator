package ua.valeriishymchuk.simpleitemgenerator.common.bridge;

import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemAttributeModifiers;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import ua.valeriishymchuk.simpleitemgenerator.common.annotation.UsesBukkit;
import ua.valeriishymchuk.simpleitemgenerator.common.slot.EquipmentSlotWrapper;

@UsesBukkit
public class PacketEventsBridge {

    @Contract("null -> null")
    public static ItemAttributeModifiers.EquipmentSlotGroup bridge(@Nullable EquipmentSlotWrapper wrapper) {
        if (wrapper == null) return null;
        return ItemAttributeModifiers.EquipmentSlotGroup.valueOf(wrapper.name());
    }

    @Contract("null -> null")
    public static EquipmentSlotWrapper bridge(@Nullable ItemAttributeModifiers.EquipmentSlotGroup group) {
        if (group == null) return null;
        return EquipmentSlotWrapper.valueOf(group.name());
    }

}
