package ua.valeriishymchuk.simpleitemgenerator.common.item;

import com.github.retrooper.packetevents.protocol.attribute.AttributeOperation;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemAttributeModifiers;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.vavr.API;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.key.Key;
import org.bukkit.inventory.ItemStack;
import org.spongepowered.configurate.ConfigurationNode;
import ua.valeriishymchuk.simpleitemgenerator.common.config.DefaultLoader;
import ua.valeriishymchuk.simpleitemgenerator.common.config.exception.InvalidConfigurationException;
import ua.valeriishymchuk.simpleitemgenerator.common.text.StringSimilarityUtils;
import ua.valeriishymchuk.simpleitemgenerator.common.version.FeatureSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.version.SemanticVersion;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

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
    @Nullable
    UUID uuid;
    @Nullable
    String name;
    @Nullable
    ItemAttributeModifiers.EquipmentSlotGroup slot;

    @SneakyThrows
    public ConfigurationNode toNode() {
        ConfigurationNode node = DefaultLoader.yaml().createNode();
        node.node("operation").set(operation.name());
        node.node("amount").set(amount);
        boolean is1_9 = SemanticVersion.CURRENT_MINECRAFT.isAtLeast(1, 9);
        if (modifier.isSupported())
            node.node("attribute").set(modifier.name());
        if (name != null) node.node("name").set(name);
        if (is1_9 && slot != null) node.node("slot").set(slot.name());
        return node;
    }

    private static Operation getOperation(ConfigurationNode node) throws InvalidConfigurationException {
        return Try.ofSupplier(() -> node.node("operation").getString())
                .filter(Objects::nonNull, () -> new InvalidConfigurationException("Operation is not present."))
                .map(String::toUpperCase)
                .mapTry(Operation::valueOf)
                .mapFailure(API.Case(API.$(e -> e instanceof IllegalArgumentException), e -> {
                    List<String> suggestions = StringSimilarityUtils.getSuggestions(
                            node.node("operation").getString(),
                            Arrays.stream(Operation.values())
                                    .map(Operation::name)
                    );
                    return InvalidConfigurationException.unknownOption("operation", node.node("operation").getString(), suggestions);
                }))
                .getOrElseThrow(x -> InvalidConfigurationException.path("operation", x));
    }

    private static Modifiers getModifier(ConfigurationNode node) throws InvalidConfigurationException {
        return Try.ofSupplier(() -> node.node("attribute").getString())
                .filter(Objects::nonNull, () -> new InvalidConfigurationException("Attribute is not present."))
                .map(String::toUpperCase)
                .mapTry(Modifiers::valueOf)
                .mapFailure(API.Case(API.$(e -> e instanceof IllegalArgumentException), e -> {
                    List<String> suggestions = StringSimilarityUtils.getSuggestions(
                            node.node("attribute").getString(),
                            Arrays.stream(Modifiers.values())
                                    .map(Modifiers::name)
                    );
                    return InvalidConfigurationException.unknownOption("attribute", node.node("attribute").getString(), suggestions);
                }))
                .andThenTry(modifier -> {
                    if (!modifier.isSupported()) throw new InvalidConfigurationException(
                            "Attribute " + modifier + " is supported since "
                                    + modifier.availableSince + ". Current version is " + SemanticVersion.CURRENT_MINECRAFT
                    );
                })
                .getOrElseThrow(x -> InvalidConfigurationException.path("attribute", x));
    }

    private static Option<ItemAttributeModifiers.EquipmentSlotGroup> getSlot(ConfigurationNode node) throws InvalidConfigurationException {
        return Try.ofSupplier(() -> node.node("slot").getString())
                .map(Option::of)
                .map(slotOpt -> slotOpt.map(String::toUpperCase))
                .mapTry(slotOpt -> slotOpt.map(ItemAttributeModifiers.EquipmentSlotGroup::valueOf))
                .mapFailure(API.Case(API.$(e -> e instanceof IllegalArgumentException), e -> {
                    List<String> suggestions = StringSimilarityUtils.getSuggestions(
                            node.node("slot").getString(),
                            Arrays.stream(ItemAttributeModifiers.EquipmentSlotGroup.values())
                                    .map(ItemAttributeModifiers.EquipmentSlotGroup::name)
                    );
                    return InvalidConfigurationException.unknownOption("slot", node.node("slot").getString(), suggestions);
                }))
                .andThenTry(slot -> {
                    if (slot.isEmpty()) return;
                    if (!FeatureSupport.MODERN_COMBAT) {
                        throw new InvalidConfigurationException("Slot is supported since 1.9. Current version is " + SemanticVersion.CURRENT_MINECRAFT);
                    }
                    if (FeatureSupport.SLOT_GROUP_SUPPORT) return;
                    List<ItemAttributeModifiers.EquipmentSlotGroup> forbiddenSlots = Arrays.asList(
                            ItemAttributeModifiers.EquipmentSlotGroup.BODY,
                            ItemAttributeModifiers.EquipmentSlotGroup.ARMOR,
                            ItemAttributeModifiers.EquipmentSlotGroup.HAND
                    );
                    if (forbiddenSlots.contains(slot.get())) throw new InvalidConfigurationException(
                            "Slot " + slot.get() + " is not available up until 1.20.5. Current version is " + SemanticVersion.CURRENT_MINECRAFT
                    );
                })
                .getOrElseThrow(x -> InvalidConfigurationException.path("slot", x));
    }

    public static Attribute fromNode(ConfigurationNode node) throws InvalidConfigurationException {
        Operation operation = getOperation(node);
        String name = node.node("name").getString();
        Modifiers modifier = getModifier(node);
        double amount = Try.ofSupplier(() -> node.node("amount").getString())
                .filter(Objects::nonNull, () -> new InvalidConfigurationException("Amount is not present."))
                .mapTry(Double::parseDouble)
                .mapFailure(API.Case(API.$(e -> e instanceof NumberFormatException), e -> InvalidConfigurationException.format("Not a number: <white>%s</white>.", node.node("amount").getString())))
                .getOrElseThrow(x -> InvalidConfigurationException.path("amount", x));
        ItemAttributeModifiers.EquipmentSlotGroup slot = getSlot(node).getOrElse(ItemAttributeModifiers.EquipmentSlotGroup.ANY);
        return new Attribute(operation, modifier, amount, null, name, slot);
    }

    private ReadWriteNBT toNbt(int entropy) {
        ReadWriteNBT nbt = NBT.createNBTObject();
        boolean is1_21 = SemanticVersion.CURRENT_MINECRAFT.isAtLeast(1, 21);
        boolean supportsComponents = FeatureSupport.ITEM_COMPONENTS_SUPPORT;
        boolean is1_9 = SemanticVersion.CURRENT_MINECRAFT.isAtLeast(1, 9);
        nbt.setString(supportsComponents ? ATTRIBUTE_NAME_1_21 : ATTRIBUTE_NAME, modifier.getNotation(SemanticVersion.CURRENT_MINECRAFT));
        String name = this.name == null ? entropy + "" : this.name;
        UUID uuid = this.uuid == null ? java.util.UUID.nameUUIDFromBytes(name.getBytes()) : this.uuid;
        if (!is1_21) {
            nbt.setString(supportsComponents ? NAME.toLowerCase() : NAME, name);
            writeUUID(supportsComponents ? UUID.toLowerCase() : UUID, uuid, nbt);
        } else nbt.setString(ID, name);
        if (is1_9 && slot != null && slot != ItemAttributeModifiers.EquipmentSlotGroup.ANY) {
            if (io.vavr.collection.List.of(
                    ItemAttributeModifiers.EquipmentSlotGroup.HAND,
                    ItemAttributeModifiers.EquipmentSlotGroup.ARMOR,
                    ItemAttributeModifiers.EquipmentSlotGroup.BODY
            ).contains(slot)) throw new UnsupportedOperationException(
                    "Slot " + slot + " is supported since 1.20.5>. Current version is " + SemanticVersion.CURRENT_MINECRAFT
            );
            nbt.setString(SLOT, slot.name().toLowerCase());
        }
        nbt.setDouble(supportsComponents ? AMOUNT.toLowerCase() : AMOUNT, amount);
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
        ItemAttributeModifiers modifiers = new ItemAttributeModifiers(new ArrayList<>(), true);
        int entropy = modifiers.getModifiers().size();
        String name = this.name == null ? entropy + "" : this.name;
        java.util.UUID uuid = this.uuid == null ? java.util.UUID.nameUUIDFromBytes(name.getBytes()) : this.uuid;
        modifiers.addModifier(new ItemAttributeModifiers.ModifierEntry(
                Attributes.getByName(modifier.getNotation(SemanticVersion.CURRENT_MINECRAFT)),
                new ItemAttributeModifiers.Modifier(
                        uuid,
                        name,
                        amount,
                        AttributeOperation.values()[operation.ordinal()]
                ),
                slot == null ? ItemAttributeModifiers.EquipmentSlotGroup.ANY : slot
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
        return new int[]{(int) (uuidMost >> 32), (int) uuidMost, (int) (uuidLeast >> 32), (int) uuidLeast};
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
                new SemanticVersion(1, 8),
                io.vavr.collection.HashMap.of(
                        new SemanticVersion(1, 20, 5), Key.key("generic.jump_strength").asString()
                ).toJavaMap()
        ),
        ZOMBIE_SPAWN_REINFORCEMENTS("zombie.spawnReinforcements"),

        ATTACK_SPEED("generic.attackSpeed", new SemanticVersion(1, 9)),
        ARMOR("generic.armor", new SemanticVersion(1, 9)),
        ARMOR_TOUGHNESS("generic.armorToughness", new SemanticVersion(1, 9)),
        LUCK("generic.luck", new SemanticVersion(1, 9)),

        FLYING_SPEED("generic.flyingSpeed", new SemanticVersion(1, 12)),

        ATTACK_KNOCKBACK("generic.attackKnockback", new SemanticVersion(1, 14)),

        MAX_ABSORPTION("generic.max_absorption", new SemanticVersion(1, 20, 2)),

        SCALE("generic.scale", new SemanticVersion(1, 20, 5)),
        STEP_HEIGHT("generic.step_height", new SemanticVersion(1, 20, 5)),
        ENTITY_INTERACTION_RANGE("player.entity_interaction_range", new SemanticVersion(1, 20, 5)),
        BLOCK_INTERACTION_RANGE("player.block_interaction_range", new SemanticVersion(1, 20, 5)),
        GRAVITY("generic.gravity", new SemanticVersion(1, 20, 5)),
        SAFE_FALL_DISTANCE("generic.safe_fall_distance", new SemanticVersion(1, 20, 5)),
        BLOCK_BREAK_SPEED("player.block_break_speed", new SemanticVersion(1, 20, 5)),

        BURNING_TIME("generic.burning_time", new SemanticVersion(1, 21)),
        EXPLOSION_KNOCKBACK_RESISTANCE("generic.explosion_knockback_resistance", new SemanticVersion(1, 21)),
        MINING_EFFICIENCY("player.mining_efficiency", new SemanticVersion(1, 21)),
        MOVEMENT_EFFICIENCY("generic.movement_efficiency", new SemanticVersion(1, 21)),
        OXYGEN_BONUS("generic.oxygen_bonus", new SemanticVersion(1, 21)),
        SNEAKING_SPEED("player.sneaking_speed", new SemanticVersion(1, 21)),
        SUBMERGED_MINING_SPEED("player.submerged_mining_speed", new SemanticVersion(1, 21)),
        SWEEPING_DAMAGE_RATION("player.sweeping_damage_ration", new SemanticVersion(1, 21)),
        WATER_MOVEMENT_EFFICIENCY("generic.water_movement_efficiency", new SemanticVersion(1, 21)),

        TEMPT_RANGE(".tempt_range", new SemanticVersion(1, 21, 2)),

        ;
        String oldNotation;
        SemanticVersion availableSince;
        Map<SemanticVersion, String> notationPerVersion;

        Modifiers(String oldNotation) {
            this(oldNotation, new SemanticVersion(1, 8));
        }

        Modifiers(String oldNotation, SemanticVersion availableSince) {
            this.oldNotation = oldNotation;
            this.availableSince = availableSince;
            this.notationPerVersion = new HashMap<>();
            Key _116Notation = Key.key(to116Notation(oldNotation));
            notationPerVersion.put(new SemanticVersion(1, 16), _116Notation.asString());
            notationPerVersion.put(new SemanticVersion(1, 21, 2), to1212Notation(_116Notation));
        }

        Modifiers(String oldNotation, SemanticVersion availableSince, Map<SemanticVersion, String> notationPerVersion) {
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

        public String getNotation(SemanticVersion version) {
            if (version.compareTo(new SemanticVersion(1, 16)) < 0) return oldNotation;
            return notationPerVersion.entrySet().stream()
                    .filter(entry -> version.isAtLeast(entry.getKey()))
                    .max(Map.Entry.comparingByKey())
                    .get().getValue();
        }

        public boolean isSupported() {
            return SemanticVersion.CURRENT_MINECRAFT.isAtLeast(availableSince);
        }

    }

}
