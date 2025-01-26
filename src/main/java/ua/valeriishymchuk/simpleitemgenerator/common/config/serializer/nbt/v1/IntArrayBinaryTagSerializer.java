package ua.valeriishymchuk.simpleitemgenerator.common.config.serializer.nbt.v1;

import net.kyori.adventure.nbt.IntArrayBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import org.apache.commons.lang.ArrayUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IntArrayBinaryTagSerializer implements TypeSerializer<IntArrayBinaryTag> {

    public static final IntArrayBinaryTagSerializer INSTANCE = new IntArrayBinaryTagSerializer();

    private IntArrayBinaryTagSerializer() {}

    @Override
    public IntArrayBinaryTag deserialize(Type type, ConfigurationNode node) throws SerializationException {
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
        if (!Objects.equals(children.get(0), "I"))
            throw new SerializationException("For IntArray first element must be equals to I");
        return IntArrayBinaryTag.intArrayBinaryTag(
                children.stream()
                        .skip(1)
                        .filter(Objects::nonNull)
                        .map(v -> IntBinaryTagSerializer.INSTANCE.sneakyDeserialize(v).value())
                        .mapToInt(i -> i).toArray()
        );
    }

    @Override
    public void serialize(Type type, @Nullable IntArrayBinaryTag obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.set(null);
            return;
        }
        node.setList(
                String.class,
                Stream.of(
                        Stream.of("I"),
                        Arrays.stream(ArrayUtils.toObject(obj.value()))
                                .map(v -> IntBinaryTagSerializer.INSTANCE.sneakySerialize(IntBinaryTag.intBinaryTag(v)))
                ).flatMap(s -> s).collect(Collectors.toList())
        );
    }

    @Override
    public @Nullable IntArrayBinaryTag emptyValue(Type specificType, ConfigurationOptions options) {
        return IntArrayBinaryTag.intArrayBinaryTag();
    }
}
