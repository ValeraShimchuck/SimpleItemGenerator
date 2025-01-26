package ua.valeriishymchuk.simpleitemgenerator.common.config.serializer.nbt.v1;

import net.kyori.adventure.nbt.LongArrayBinaryTag;
import net.kyori.adventure.nbt.LongBinaryTag;
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

public class LongArrayBinaryTagSerializer implements TypeSerializer<LongArrayBinaryTag> {


    public static final LongArrayBinaryTagSerializer INSTANCE = new LongArrayBinaryTagSerializer();

    private LongArrayBinaryTagSerializer() {}

    @Override
    public LongArrayBinaryTag deserialize(Type type, ConfigurationNode node) throws SerializationException {
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
        if (!Objects.equals(children.get(0), "L"))
            throw new SerializationException("For LongArray first element must be equals to L");
        return LongArrayBinaryTag.longArrayBinaryTag(
                children.stream()
                        .skip(1)
                        .filter(Objects::nonNull)
                        .map(v -> LongBinaryTagSerializer.INSTANCE.sneakyDeserialize(v).value())
                        .mapToLong(l -> l).toArray()
        );
    }

    @Override
    public void serialize(Type type, @Nullable LongArrayBinaryTag obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.set(null);
            return;
        }
        node.setList(
                String.class,
                Stream.of(
                        Stream.of("L"),
                        Arrays.stream(ArrayUtils.toObject(obj.value()))
                                .map(v -> LongBinaryTag.longBinaryTag(v))
                                .map(LongBinaryTagSerializer.INSTANCE::sneakySerialize)
                ).flatMap(s -> s).collect(Collectors.toList())
        );
    }

    @Override
    public @Nullable LongArrayBinaryTag emptyValue(Type specificType, ConfigurationOptions options) {
        return LongArrayBinaryTag.longArrayBinaryTag();
    }
}
