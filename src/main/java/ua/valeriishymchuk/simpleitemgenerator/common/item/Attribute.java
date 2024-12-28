package ua.valeriishymchuk.simpleitemgenerator.common.item;

import com.github.retrooper.packetevents.protocol.attribute.AttributeOperation;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemAttributeModifiers;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.key.Key;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.spongepowered.configurate.ConfigurationNode;
import ua.valeriishymchuk.simpleitemgenerator.common.config.DefaultLoader;
import ua.valeriishymchuk.simpleitemgenerator.common.version.FeatureSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.version.MinecraftVersion;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class Attribute {

    private static final String ATTRIBUTE_MODIFIERS = "AttributeModifiers";
    private static final String ATTRIBUTE_MODIFIERS_1_21 = "attribute_modifiers";
    private static final String ATTRIBUTE_NAME = "AttributeName";
    private static final String ATTRIBUTE_NAME_1_21 = "type";
    private static final String ID = "id"; // 1.21 string
    private static final String SLOT = "Slot"; // 1.9 str

    private static final String NAME = "Name"; // until 1.21 string
    private static final String AMOUNT = "Amount"; // double
    private static final String OPERATION = "Operation"; // int
    private static final String UUID = "UUID";  // until 1.21 varies


    Operation operation;
    Modifiers modifier;
    double amount;
    @Nullable UUID uuid;
    @Nullable
    String name;
    @Nullable
    ItemAttributeModifiers.EquipmentSlotGroup slot;

    @SneakyThrows
    public ConfigurationNode toNode() {
        ConfigurationNode node = DefaultLoader.yaml().createNode();
        node.node("operation").set(operation.name());
        node.node("amount").set(amount);
        boolean is1_9 = MinecraftVersion.CURRENT.isAtLeast(1, 9);
        if (modifier.isSupported())
            node.node("attribute").set(modifier.name());
        if (name != null) node.node("name").set(name);
        if (is1_9 && slot != null) node.node("slot").set(slot.name());
        return node;
    }

    public static Attribute fromNode(ConfigurationNode node) {
        Operation operation = Option.of(node.node("operation").getString())
                .map(String::toUpperCase).toTry(() -> new IllegalStateException("Not present."))
                .mapTry(Operation::valueOf)
                .getOrElseThrow(x -> new IllegalStateException("Can't get operation from node", x));
        String name = node.node("name").getString();
        Modifiers modifier = Option.of(node.node("attribute").getString())
                .map(String::toUpperCase).toTry(() -> new IllegalStateException("Not present."))
                .mapTry(Modifiers::valueOf)
                .getOrElseThrow(x -> new IllegalStateException("Can't get modifier from node", x));
        if (!modifier.isSupported()) throw new IllegalStateException(
                "Modifier " + modifier + " is supported since "
                        + modifier.availableSince + ". Current version is " + MinecraftVersion.CURRENT
        );
        if (node.node("amount").isNull()) throw new IllegalStateException("Amount is not present.");
        double amount = node.node("amount").getDouble();
        ItemAttributeModifiers.EquipmentSlotGroup slot = Option.of(node.node("slot").getString()).map(String::toUpperCase)
                .map(ItemAttributeModifiers.EquipmentSlotGroup::valueOf).getOrElse(ItemAttributeModifiers.EquipmentSlotGroup.ANY);
        return new Attribute(operation, modifier, amount, null, name, slot);
    }

    private ReadWriteNBT toNbt(int entropy) {
        ReadWriteNBT nbt = NBT.createNBTObject();
        boolean is1_21 = MinecraftVersion.CURRENT.isAtLeast(1, 21);
        boolean supportsComponents = FeatureSupport.ITEM_COMPONENTS_SUPPORT;
        boolean is1_9 = MinecraftVersion.CURRENT.isAtLeast(1, 9);
        nbt.setString(supportsComponents? ATTRIBUTE_NAME_1_21 : ATTRIBUTE_NAME, modifier.getNotation(MinecraftVersion.CURRENT));
        String name = this.name == null? entropy + "" : this.name;
        UUID uuid = this.uuid == null? java.util.UUID.nameUUIDFromBytes(name.getBytes()) : this.uuid;
        if (!is1_21) {
            nbt.setString(supportsComponents? NAME.toLowerCase() : NAME, name);
            writeUUID(supportsComponents? UUID.toLowerCase() : UUID, uuid, nbt);
        } else nbt.setString(ID, name);
        if (is1_9 && slot != null && slot != ItemAttributeModifiers.EquipmentSlotGroup.ANY) {
            if (io.vavr.collection.List.of(
                    ItemAttributeModifiers.EquipmentSlotGroup.HAND,
                    ItemAttributeModifiers.EquipmentSlotGroup.ARMOR,
                    ItemAttributeModifiers.EquipmentSlotGroup.BODY
            ).contains(slot)) throw new UnsupportedOperationException(
                        "Slot " + slot + " is supported since 1.20.5>. Current version is " + MinecraftVersion.CURRENT
            );
            nbt.setString(SLOT, slot.name().toLowerCase());
        }
        nbt.setDouble(supportsComponents? AMOUNT.toLowerCase() : AMOUNT, amount);
        if (supportsComponents) {
            nbt.setString(OPERATION.toLowerCase(), operation.name().toLowerCase());
        } else nbt.setInteger(OPERATION, operation.ordinal());
        return nbt;
    }

    public ItemStack applyOnItem(ItemStack itemStack) {
        boolean supportsComponents = FeatureSupport.ITEM_COMPONENTS_SUPPORT;
        if (!supportsComponents) {
            NBT.modify(itemStack, nbt -> {
                nbt.getCompoundList(ATTRIBUTE_MODIFIERS).addCompound(toNbt(nbt.getCompoundList(ATTRIBUTE_MODIFIERS).size()));
            });
            return itemStack;
        }
        com.github.retrooper.packetevents.protocol.item.ItemStack peStack = SpigotConversionUtil.fromBukkitItemStack(itemStack);
        ItemAttributeModifiers modifiers = peStack.getComponent(ComponentTypes.ATTRIBUTE_MODIFIERS)
                .map(i -> new ItemAttributeModifiers(new ArrayList<>(i.getModifiers()), i.isShowInTooltip()))
                .orElse(new ItemAttributeModifiers(new ArrayList<>(), true));
        int entropy = modifiers.getModifiers().size();
        String name = this.name == null? entropy + "" : this.name;
        java.util.UUID uuid = this.uuid == null? java.util.UUID.nameUUIDFromBytes(name.getBytes()) : this.uuid;
        modifiers.addModifier(new ItemAttributeModifiers.ModifierEntry(
                Attributes.getByName(modifier.getNotation(MinecraftVersion.CURRENT)),
                new ItemAttributeModifiers.Modifier(
                        uuid,
                        name,
                        amount,
                        AttributeOperation.values()[operation.ordinal()]
                        ),
                slot == null? ItemAttributeModifiers.EquipmentSlotGroup.ANY : slot
        ));
        peStack.setComponent(ComponentTypes.ATTRIBUTE_MODIFIERS, modifiers);
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
        return new int[]{(int)(uuidMost >> 32), (int)uuidMost, (int)(uuidLeast >> 32), (int)uuidLeast};
    }

    public enum Operation {
        ADD_VALUE,
        ADD_MULTIPLIED_BASE,
        ADD_MULTIPLIED_TOTAL
    }


    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public enum Modifiers {
        ATTACK_DAMAGE("generic.attackDamage"),
        MAX_HEALTH("generic.maxHealth"),
        FOLLOW_RANGE("generic.followRange"),
        KNOCKBACK_RESISTANCE("generic.knockbackResistance"),
        MOVEMENT_SPEED("generic.movementSpeed"),
        JUMP_STRENGTH(
                "horse.jumpStrength",
                new MinecraftVersion(1, 8),
                io.vavr.collection.HashMap.of(
                        new MinecraftVersion(1, 20,5), Key.key("generic.jump_strength").asString()
                ).toJavaMap()
        ),
        ZOMBIE_SPAWN_REINFORCEMENTS("zombie.spawnReinforcements"),

        ATTACK_SPEED("generic.attackSpeed", new MinecraftVersion(1, 9)),
        ARMOR("generic.armor", new MinecraftVersion(1, 9)),
        ARMOR_TOUGHNESS("generic.armorToughness", new MinecraftVersion(1, 9)),
        LUCK("generic.luck", new MinecraftVersion(1, 9)),

        FLYING_SPEED("generic.flyingSpeed", new MinecraftVersion(1,12)),

        ATTACK_KNOCKBACK("generic.attackKnockback", new MinecraftVersion(1,14)),

        MAX_ABSORPTION("generic.max_absorption", new MinecraftVersion(1,20,2)),

        SCALE("generic.scale", new MinecraftVersion(1,20,5)),
        STEP_HEIGHT("generic.step_height", new MinecraftVersion(1,20,5)),
        ENTITY_INTERACTION_RANGE("player.entity_interaction_range", new MinecraftVersion(1,20,5)),
        BLOCK_INTERACTION_RANGE("player.block_interaction_range", new MinecraftVersion(1,20,5)),
        GRAVITY("generic.gravity", new MinecraftVersion(1,20,5)),
        SAFE_FALL_DISTANCE("generic.safe_fall_distance", new MinecraftVersion(1,20,5)),
        BLOCK_BREAK_SPEED("player.block_break_speed", new MinecraftVersion(1,20,5)),

        BURNING_TIME("generic.burning_time", new MinecraftVersion(1,21)),
        EXPLOSION_KNOCKBACK_RESISTANCE("generic.explosion_knockback_resistance", new MinecraftVersion(1,21)),
        MINING_EFFICIENCY("player.mining_efficiency", new MinecraftVersion(1,21)),
        MOVEMENT_EFFICIENCY("generic.movement_efficiency", new MinecraftVersion(1,21)),
        OXYGEN_BONUS("generic.oxygen_bonus", new MinecraftVersion(1,21)),
        SNEAKING_SPEED("player.sneaking_speed", new MinecraftVersion(1,21)),
        SUBMERGED_MINING_SPEED("player.submerged_mining_speed", new MinecraftVersion(1,21)),
        SWEEPING_DAMAGE_RATION("player.sweeping_damage_ration", new MinecraftVersion(1,21)),
        WATER_MOVEMENT_EFFICIENCY("generic.water_movement_efficiency", new MinecraftVersion(1,21)),

        TEMPT_RANGE(".tempt_range", new MinecraftVersion(1,21,2)),

        ;
        String oldNotation;
        MinecraftVersion availableSince;
        Map<MinecraftVersion, String> notationPerVersion;

        Modifiers(String oldNotation) {
            this(oldNotation, new MinecraftVersion(1,8));
        }

        Modifiers(String oldNotation, MinecraftVersion availableSince) {
            this.oldNotation = oldNotation;
            this.availableSince = availableSince;
            this.notationPerVersion = new HashMap<>();
            Key _116Notation = Key.key(to116Notation(oldNotation));
            notationPerVersion.put(new MinecraftVersion(1, 16), _116Notation.asString());
            notationPerVersion.put(new MinecraftVersion(1,21,2), to1212Notation(_116Notation));
        }

        Modifiers(String oldNotation, MinecraftVersion availableSince, Map<MinecraftVersion, String> notationPerVersion) {
            this(oldNotation, availableSince);
            this.notationPerVersion.putAll(notationPerVersion);
        }

        private static String to116Notation(String oldNotation) {
            StringBuilder sb = new StringBuilder();
            oldNotation.chars().mapToObj(c -> (char) c)
                    .forEach(c -> {
                        if (Character.isUpperCase(c)) sb.append("_").append(Character.toLowerCase(c));
                        else sb.append(c);
                    });
            return sb.toString();
        }

        private static String to1212Notation(Key oldNotation) {
            String value = oldNotation.value();
            String[] splitParts = value.split("\\.");
            return splitParts[1];
        }

        public String getNotation(MinecraftVersion version) {
            if (version.compareTo(new MinecraftVersion(1,16)) < 0) return oldNotation;
            return notationPerVersion.entrySet().stream()
                    .filter(entry -> version.isAtLeast(entry.getKey()))
                    .max(Map.Entry.comparingByKey())
                    .get().getValue();
        }

        public boolean isSupported() {
            return MinecraftVersion.CURRENT.isAtLeast(availableSince);
        }

    }

}
