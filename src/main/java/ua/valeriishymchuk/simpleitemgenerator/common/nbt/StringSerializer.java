package ua.valeriishymchuk.simpleitemgenerator.common.nbt;

import lombok.SneakyThrows;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public interface StringSerializer<T> extends TypeSerializer<T> {


    @Override
    default T deserialize(Type type, ConfigurationNode node) throws SerializationException {
        String input = node.getString();
        if (input == null) throw new SerializationException("Must provide a string");
        return deserialize(input);
    }

    @Override
    default void serialize(Type type, @Nullable T obj, ConfigurationNode node) throws SerializationException {
        node.set(serialize(obj));
    }

    T deserialize(String input) throws SerializationException;

    @SneakyThrows
    default T  sneakyDeserialize(String input) {
        return deserialize(input);
    }

    @SneakyThrows
    String serialize(T input) throws SerializationException;

    @SneakyThrows
    default String sneakySerialize(T input) {
        return serialize(input);
    }

}
