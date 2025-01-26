package ua.valeriishymchuk.simpleitemgenerator.common.config.serializer.nbt.v1;

import net.kyori.adventure.nbt.ByteBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ByteBinaryTagSerializer implements StringSerializer<ByteBinaryTag> {

    private static final Pattern REGEX = Pattern.compile("^(\\d+)[bB]|(true|false)$");
    public static final ByteBinaryTagSerializer INSTANCE = new ByteBinaryTagSerializer();

    private ByteBinaryTagSerializer() {}

    @Override
    public @Nullable ByteBinaryTag emptyValue(Type specificType, ConfigurationOptions options) {
        return ByteBinaryTag.ZERO;
    }

    @Override
    public ByteBinaryTag deserialize(String input) throws SerializationException {
        Matcher matcher = REGEX.matcher(input);
        if (!matcher.matches()) throw new SerializationException("Must be in the format " + REGEX);
        String numericGroup = matcher.groupCount() > 1?  matcher.group(1) : null;
        String booleanGroup = matcher.groupCount() > 2? matcher.group(2) : null;
        ByteBinaryTag result;
        if (numericGroup != null) result = ByteBinaryTag.byteBinaryTag(Byte.parseByte(numericGroup));
        else if (booleanGroup != null) {
            result = ByteBinaryTag.byteBinaryTag((byte) (Boolean.parseBoolean(booleanGroup) ? 1 : 0));
        } else result = ByteBinaryTag.ZERO;
        return result;
    }

    @Override
    public String serialize(ByteBinaryTag input) throws SerializationException {
        if (input == null) return "0b";
        return input.value() + "b";
    }
}
