package ua.valeriishymchuk.simpleitemgenerator.common.item;

import com.github.retrooper.packetevents.protocol.attribute.AttributeOperation;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemAttributeModifiers;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.inventory.ItemStack;
import ua.valeriishymchuk.simpleitemgenerator.common.annotation.UsesBukkit;
import ua.valeriishymchuk.simpleitemgenerator.common.bridge.PacketEventsBridge;
import ua.valeriishymchuk.simpleitemgenerator.common.slot.EquipmentSlotWrapper;
import ua.valeriishymchuk.simpleitemgenerator.common.version.FeatureSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.version.SemanticVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static ua.valeriishymchuk.simpleitemgenerator.common.item.Attribute.*;

@UsesBukkit
public class AttributeApplier {

    private static ReadWriteNBT toNbt(Attribute attribute, int entropy) {
        ReadWriteNBT nbt = NBT.createNBTObject();
        boolean is1_21 = SemanticVersion.CURRENT_MINECRAFT.isAtLeast(1, 21);
        boolean supportsComponents = FeatureSupport.ITEM_COMPONENTS_SUPPORT;
        boolean is1_9 = SemanticVersion.CURRENT_MINECRAFT.isAtLeast(1, 9);
        nbt.setString(
                supportsComponents ? ATTRIBUTE_NAME_1_21 : ATTRIBUTE_NAME,
                attribute.getModifier().getNotation(SemanticVersion.CURRENT_MINECRAFT)
        );
        String name = attribute.getName() == null ? entropy + "" : attribute.getName();
        UUID uuid = attribute.getUuid() == null ? java.util.UUID.nameUUIDFromBytes(name.getBytes()) : attribute.getUuid();
        if (!is1_21) {
            nbt.setString(supportsComponents ? NAME.toLowerCase() : NAME, name);
            writeUUID(supportsComponents ? UUID.toLowerCase() : UUID, uuid, nbt);
        } else nbt.setString(ID, name);
        if (is1_9 && attribute.getSlot() != null && attribute.getSlot() != EquipmentSlotWrapper.ANY) {
            if (io.vavr.collection.List.of(
                    EquipmentSlotWrapper.HAND,
                    EquipmentSlotWrapper.ARMOR,
                    EquipmentSlotWrapper.BODY
            ).contains(attribute.getSlot())) throw new UnsupportedOperationException(
                    "Slot " + attribute.getSlot() + " is supported since 1.20.5>. Current version is " + SemanticVersion.CURRENT_MINECRAFT
            );
            nbt.setString(SLOT, attribute.getSlot().name().toLowerCase());
        }
        nbt.setDouble(supportsComponents ? AMOUNT.toLowerCase() : AMOUNT, attribute.getAmount());
        if (supportsComponents) {
            nbt.setString(OPERATION.toLowerCase(), attribute.getOperation().name().toLowerCase());
        } else nbt.setInteger(OPERATION, attribute.getOperation().ordinal());
        return nbt;
    }

    public static ItemStack applyOnItem(List<Attribute> attributes, ItemStack itemStack) {
        boolean supportsComponents = FeatureSupport.ITEM_COMPONENTS_SUPPORT;
        if (!supportsComponents) {
            NBT.modify(itemStack, nbt -> {
                attributes.forEach(attribute -> {
                    nbt.getCompoundList(ATTRIBUTE_MODIFIERS).addCompound(toNbt(attribute, nbt.getCompoundList(ATTRIBUTE_MODIFIERS).size()));
                });

            });
            return itemStack;
        }
        com.github.retrooper.packetevents.protocol.item.ItemStack peStack = SpigotConversionUtil.fromBukkitItemStack(itemStack);
        ItemAttributeModifiers modifiers = new ItemAttributeModifiers(new ArrayList<>(), true);
        AtomicInteger atomicEntropy = new AtomicInteger(0);
        attributes.forEach(attribute -> {
            int entropy = atomicEntropy.getAndIncrement();
            String name = attribute.getName() == null ? entropy + "" : attribute.getName();
            java.util.UUID uuid = attribute.getUuid() == null ? java.util.UUID.nameUUIDFromBytes(name.getBytes()) : attribute.getUuid();
            modifiers.addModifier(new ItemAttributeModifiers.ModifierEntry(
                    Attributes.getByName(attribute.getModifier().getNotation(SemanticVersion.CURRENT_MINECRAFT)),
                    new ItemAttributeModifiers.Modifier(
                            uuid,
                            name,
                            attribute.getAmount(),
                            AttributeOperation.values()[attribute.getOperation().ordinal()]
                    ),
                    attribute.getSlot() == null ? ItemAttributeModifiers.EquipmentSlotGroup.ANY : PacketEventsBridge.bridge(attribute.getSlot())
            ));
        });
        peStack.getComponents().set(ComponentTypes.ATTRIBUTE_MODIFIERS, modifiers);
        return SpigotConversionUtil.toBukkitItemStack(peStack);
    }


    private static void writeUUID(String key, UUID uuid, ReadWriteNBT nbt) {
        if (FeatureSupport.NAMESPACED_ATTRIBUTES_SUPPORT) {
            nbt.setIntArray(key, uuidToIntArray(uuid));
        } else {
            nbt.setLong(key + "Most", uuid.getMostSignificantBits());
            nbt.setLong(key + "Least", uuid.getLeastSignificantBits());
        }
    }

    public static int[] uuidToIntArray(UUID uuid) {
        long l = uuid.getMostSignificantBits();
        long m = uuid.getLeastSignificantBits();
        return leastMostToIntArray(l, m);
    }

    private static int[] leastMostToIntArray(long uuidMost, long uuidLeast) {
        return new int[]{(int) (uuidMost >> 32), (int) uuidMost, (int) (uuidLeast >> 32), (int) uuidLeast};
    }

}
