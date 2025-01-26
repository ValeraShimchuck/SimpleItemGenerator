package ua.valeriishymchuk.simpleitemgenerator.common.nbt;

import net.kyori.adventure.nbt.ByteArrayBinaryTag;
import net.kyori.adventure.nbt.ByteBinaryTag;
import org.apache.commons.lang.ArrayUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ByteArrayBinaryTagSerializer implements TypeSerializer<ByteArrayBinaryTag> {


    public static final ByteArrayBinaryTagSerializer INSTANCE = new ByteArrayBinaryTagSerializer();

    private ByteArrayBinaryTagSerializer() {}

    public ByteArrayBinaryTag deserialize(Type type, ConfigurationNode node) throws SerializationException {
        if (!node.isList()) throw new SerializationException("Must be a list");
        List<String> children = node.childrenList().stream()
                .map(it -> {
                    try {
                        return it.get(String.class);
                    } catch (SerializationException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
        if (children.isEmpty()) throw new SerializationException("Must not be empty");
        if (!Objects.equals(children.get(0), "B"))
            throw new SerializationException("For ByteArray first element must be equals to B");
        return ByteArrayBinaryTag.byteArrayBinaryTag(
                ArrayUtils.toPrimitive(children.stream()
                        .skip(1)
                        .filter(Objects::nonNull)
                        .map(v -> ByteBinaryTagSerializer.INSTANCE.sneakyDeserialize(v).value())
                        .toArray(Byte[]::new))
        );
    }

    public void serialize(Type type, ByteArrayBinaryTag obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.set(null);
            return;
        }
        node.setList(
                String.class,
                Stream.of(
                        Stream.of("B"),
                        Arrays.stream(ArrayUtils.toObject(obj.value()))
                                .map(v -> ByteBinaryTag.byteBinaryTag(v))
                                .map(ByteBinaryTagSerializer.INSTANCE::sneakySerialize)
                ).flatMap(s -> s).collect(Collectors.toList())
        );
    }

    @Override
    public @Nullable ByteArrayBinaryTag emptyValue(Type specificType, ConfigurationOptions options) {
        return ByteArrayBinaryTag.byteArrayBinaryTag();
    }
}
