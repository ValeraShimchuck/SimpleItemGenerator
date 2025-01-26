package ua.valeriishymchuk.simpleitemgenerator.common.nbt;

import io.vavr.Tuple2;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

public class ListBinaryTagSerializer implements TypeSerializer<ListBinaryTag> {

    public static final ListBinaryTagSerializer INSTANCE = new ListBinaryTagSerializer();

    private ListBinaryTagSerializer() {}

    @Override
    public ListBinaryTag deserialize(Type type, ConfigurationNode node) throws SerializationException {
        if (!node.isList()) throw new SerializationException("Must be a list");
        List<? extends ConfigurationNode> children = node.childrenList();
        if (children.isEmpty()) return ListBinaryTag.empty();
        Tuple2<Class<? extends BinaryTag>, BinaryTag> deserializedFirst =
                CompoundBinaryTagSerializer.tryDeserialize(children.get(0));
        if (deserializedFirst == null) return ListBinaryTag.empty();
        return ListBinaryTag.listBinaryTag(
                CompoundBinaryTagSerializer.CLASS_TO_TYPE.get(deserializedFirst._1),
                (List<BinaryTag>) node.getList(deserializedFirst._1)
        );
    }

    @Override
    public void serialize(Type type, @Nullable ListBinaryTag obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.set(null);
            return;
        }

        Class<? extends BinaryTag> clazz = CompoundBinaryTagSerializer.CLASS_TO_TYPE.inverse().get(obj.type());

        node.setList(
                (Class<? super BinaryTag>) clazz,
                obj.stream().collect(Collectors.toList())
        );
    }

    @Override
    public @Nullable ListBinaryTag emptyValue(Type specificType, ConfigurationOptions options) {
        return ListBinaryTag.empty();
    }
}
