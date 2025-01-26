package ua.valeriishymchuk.simpleitemgenerator.common.config.serializer.nbt.v1;

import net.kyori.adventure.nbt.FloatBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FloatBinaryTagSerializer implements StringSerializer<FloatBinaryTag> {

    private static final Pattern REGEX = Pattern.compile("^(?:(\\d*[,.]\\d+|\\d+[,.]\\d*)[fF]|(\\d+)[fF])$");

    public static final FloatBinaryTagSerializer INSTANCE = new FloatBinaryTagSerializer();

    private FloatBinaryTagSerializer() {}


    @Override
    public FloatBinaryTag deserialize(String input) throws SerializationException {
        Matcher matcher = REGEX.matcher(input);
        if (!matcher.matches()) throw new SerializationException("Must be in the format " + REGEX);
        String firstGroup = matcher.group(1);
        String secondGroup = matcher.groupCount() > 2? matcher.group(2) : null;
        float result;
        if (firstGroup != null) result = Float.parseFloat(firstGroup);
        else if (secondGroup != null) result = Float.parseFloat(secondGroup);
        else result = 0.0f;
        return FloatBinaryTag.floatBinaryTag(result);
    }

    @Override
    public String serialize(FloatBinaryTag input) throws SerializationException {
        return input != null? input.value() + "f" : "0f";
    }

    @Override
    public @Nullable FloatBinaryTag emptyValue(Type specificType, ConfigurationOptions options) {
        return FloatBinaryTag.floatBinaryTag(0f);
    }
}
