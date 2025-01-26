package ua.valeriishymchuk.simpleitemgenerator.common.config.serializer.nbt.v1;

import net.kyori.adventure.nbt.LongBinaryTag;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LongBinaryTagSerializer implements StringSerializer<LongBinaryTag> {

    private static final Pattern REGEX = Pattern.compile("^(\\d+)[lL]$");

    public static final LongBinaryTagSerializer INSTANCE = new LongBinaryTagSerializer();

    private LongBinaryTagSerializer() {}

    @Override
    public LongBinaryTag deserialize(String input) throws SerializationException {
        Matcher matcher = REGEX.matcher(input);
        if (!matcher.matches()) throw new SerializationException("Must be in the format " + REGEX);
        return LongBinaryTag.longBinaryTag(Long.parseLong(matcher.group(1)));
    }

    @Override
    public String serialize(LongBinaryTag input) throws SerializationException {
        return input != null? input.value() + "l" : "0l";
    }
}
