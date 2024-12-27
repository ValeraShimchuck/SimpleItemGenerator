package ua.valeriishymchuk.simpleitemgenerator.common.item;

import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.key.Key;
import ua.valeriishymchuk.simpleitemgenerator.common.version.FeatureSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.version.MinecraftVersion;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class Attribute {

    private static final String ATTRIBUTE_MODIFIERS = "AttributeModifiers";
    private static final String ATTRIBUTE_NAME = "AttributeName";
    private static final String ID = "id";
    private static final String SLOT = "slot";

    private static final String NAME = "Name";
    private static final String Amount = "Amount";
    private static final String Operation = "Operation";
    private static final String UUID = "UUID";



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
        ADD_NUMBER,
        ADD_SCALAR,
        MULTIPLY_SCALAR
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public enum Modifiers {
        ATTACK_DAMAGE("general.attackDamage"),
        MAX_HEALTH("general.maxHealth"),
        FOLLOW_RANGE("general.followRange"),
        KNOCKBACK_RESISTANCE("general.knockbackResistance"),
        MOVEMENT_SPEED("general.movementSpeed"),
        JUMP_STRENGTH(
                "horse.jumpStrength",
                new MinecraftVersion(1, 8),
                io.vavr.collection.HashMap.of(
                        new MinecraftVersion(1, 20,5), Key.key("generic.jump_strength").asString()
                ).toJavaMap()
        ),
        ZOMBIE_SPAWN_REINFORCEMENTS("zombie.spawnReinforcements"),

        ATTACK_SPEED("general.attackSpeed", new MinecraftVersion(1, 9)),
        ARMOR("general.armor", new MinecraftVersion(1, 9)),
        ARMOR_TOUGHNESS("general.armorToughness", new MinecraftVersion(1, 9)),
        LUCK("general.luck", new MinecraftVersion(1, 9)),

        FLYING_SPEED("general.flyingSpeed", new MinecraftVersion(1,12)),

        ATTACK_KNOCKBACK("general.attackKnockback", new MinecraftVersion(1,14)),

        MAX_ABSORPTION("general.max_absorption", new MinecraftVersion(1,20,2)),

        SCALE("general.scale", new MinecraftVersion(1,20,5)),
        STEP_HEIGHT("general.step_height", new MinecraftVersion(1,20,5)),
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
