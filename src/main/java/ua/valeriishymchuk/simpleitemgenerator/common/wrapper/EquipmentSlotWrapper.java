package ua.valeriishymchuk.simpleitemgenerator.common.wrapper;

import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemAttributeModifiers;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import ua.valeriishymchuk.simpleitemgenerator.common.annotation.UsesMinecraft;

public enum EquipmentSlotWrapper {
    ANY,
    MAINHAND,
    OFFHAND,
    HAND,
    FEET,
    LEGS,
    CHEST,
    HEAD,
    ARMOR,
    BODY,
    SADDLE;

    @UsesMinecraft
    public ItemAttributeModifiers.EquipmentSlotGroup toPacketEvents() {
        return ItemAttributeModifiers.EquipmentSlotGroup.valueOf(name());
    }

    @UsesMinecraft
    @Contract("null -> null")
    public static EquipmentSlotWrapper fromPacketEvents(@Nullable ItemAttributeModifiers.EquipmentSlotGroup group) {
        if (group == null) return null;
        return EquipmentSlotWrapper.valueOf(group.name());
    }

}
