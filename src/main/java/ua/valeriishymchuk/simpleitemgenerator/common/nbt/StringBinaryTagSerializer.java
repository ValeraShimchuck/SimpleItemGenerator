package ua.valeriishymchuk.simpleitemgenerator.common.nbt;

import net.kyori.adventure.nbt.StringBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;

public class StringBinaryTagSerializer implements StringSerializer<StringBinaryTag> {

    public static final StringBinaryTagSerializer INSTANCE = new StringBinaryTagSerializer();

    private StringBinaryTagSerializer() {}

    @Override
    public StringBinaryTag deserialize(String input) throws SerializationException {
        return StringBinaryTag.stringBinaryTag(input);
    }

    @Override
    public String serialize(StringBinaryTag input) throws SerializationException {
        return input == null? null : input.value();
    }

    @Override
    public @Nullable StringBinaryTag emptyValue(Type specificType, ConfigurationOptions options) {
        return StringBinaryTag.stringBinaryTag("");
    }
}
