package ua.valeriishymchuk.simpleitemgenerator.common.nbt;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import net.kyori.adventure.nbt.*;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CompoundBinaryTagSerializer implements TypeSerializer<CompoundBinaryTag> {

    public static final CompoundBinaryTagSerializer INSTANCE = new CompoundBinaryTagSerializer();

    private CompoundBinaryTagSerializer() {
    }

    public static final BiMap<Class<? extends BinaryTag>, BinaryTagType<? extends BinaryTag>> CLASS_TO_TYPE = HashMap
            .ofEntries(
                    // Primitives
                    Tuple.of(ByteBinaryTag.class, BinaryTagTypes.BYTE),
                    Tuple.of(ShortBinaryTag.class, BinaryTagTypes.SHORT),
                    Tuple.of(IntBinaryTag.class, BinaryTagTypes.INT),
                    Tuple.of(LongBinaryTag.class, BinaryTagTypes.LONG),
                    Tuple.of(FloatBinaryTag.class, BinaryTagTypes.FLOAT),
                    Tuple.of(DoubleBinaryTag.class, BinaryTagTypes.DOUBLE),
                    Tuple.of(StringBinaryTag.class, BinaryTagTypes.STRING),
                    // Arrays
                    Tuple.of(ByteArrayBinaryTag.class, BinaryTagTypes.BYTE_ARRAY),
                    Tuple.of(IntArrayBinaryTag.class, BinaryTagTypes.INT_ARRAY),
                    Tuple.of(LongArrayBinaryTag.class, BinaryTagTypes.LONG_ARRAY),
                    // Large objects
                    Tuple.of(ListBinaryTag.class, BinaryTagTypes.LIST),
                    Tuple.of(CompoundBinaryTag.class, BinaryTagTypes.COMPOUND)
            )
            .transform(map -> ImmutableBiMap.copyOf(map.toJavaMap()));

    public static final Set<Class<? extends BinaryTag>> PRIMITIVE_TYPES = CLASS_TO_TYPE.keySet();

    public static TypeSerializerCollection NBT_COLLECTION = TypeSerializerCollection.builder()
            .register(ShortBinaryTag.class, ShortBinaryTagSerializer.INSTANCE)
            .register(IntBinaryTag.class, IntBinaryTagSerializer.INSTANCE)
            .register(LongBinaryTag.class, LongBinaryTagSerializer.INSTANCE)
            .register(FloatBinaryTag.class, FloatBinaryTagSerializer.INSTANCE)
            .register(DoubleBinaryTag.class, DoubleBinaryTagSerializer.INSTANCE)
            .register(StringBinaryTag.class, StringBinaryTagSerializer.INSTANCE)
            .register(ByteBinaryTag.class, ByteBinaryTagSerializer.INSTANCE)

            .register(ByteArrayBinaryTag.class, ByteArrayBinaryTagSerializer.INSTANCE)
            .register(IntArrayBinaryTag.class, IntArrayBinaryTagSerializer.INSTANCE)
            .register(LongArrayBinaryTag.class, LongArrayBinaryTagSerializer.INSTANCE)

            .register(CompoundBinaryTag.class, CompoundBinaryTagSerializer.INSTANCE)
            .register(ListBinaryTag.class, ListBinaryTagSerializer.INSTANCE)
            .build();

    public static Tuple2<Class<? extends BinaryTag>, BinaryTag> tryDeserialize(ConfigurationNode node) throws SerializationException {
        //if (result == null) {
        //    Object raw = node.raw();
        //    throw new SerializationException("Failed to deserialize " + raw);
        //}
        return
                PRIMITIVE_TYPES.stream()
                        .<Tuple2<Class<? extends BinaryTag>, BinaryTag>>map(clazz -> {
                            try {
                                return Tuple.of(clazz, node.get(clazz));
                            } catch (SerializationException e) {
                                return Tuple.of(clazz, null);
                            }
                        }).filter(t -> t._2 != null)
                        .findFirst()
                        .orElse(null);
    }


    @Override
    public CompoundBinaryTag deserialize(Type type, ConfigurationNode node) throws SerializationException {
        if (!node.isMap()) throw new SerializationException("Must be a map");
        CompoundBinaryTag.Builder compound = CompoundBinaryTag.builder();
        node.childrenMap().forEach((key, value) -> {
            String stringKey = key.toString();
            try {
                Tuple2<Class<? extends BinaryTag>, BinaryTag> deserialized = tryDeserialize(value);
                if (deserialized == null) return;
                compound.put(stringKey, deserialized._2);
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        });
        return compound.build();
    }

    @Override
    public void serialize(Type type, @Nullable CompoundBinaryTag obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.set(null);
            return;
        }

        obj.forEach((key) -> {
            try {
                node.node(key.getKey()).set(key.getValue());
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        });

    }

    @Override
    public @Nullable CompoundBinaryTag emptyValue(Type specificType, ConfigurationOptions options) {
        return CompoundBinaryTag.empty();
    }
}
