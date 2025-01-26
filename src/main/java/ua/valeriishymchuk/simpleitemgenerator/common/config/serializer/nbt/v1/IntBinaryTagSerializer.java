package ua.valeriishymchuk.simpleitemgenerator.common.config.serializer.nbt.v1;

import net.kyori.adventure.nbt.IntBinaryTag;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IntBinaryTagSerializer implements StringSerializer<IntBinaryTag> {

    private static final Pattern REGEX = Pattern.compile("^(\\d+)[iI]?$");

    public static final IntBinaryTagSerializer INSTANCE = new IntBinaryTagSerializer();

    private IntBinaryTagSerializer() {}

    @Override
    public IntBinaryTag deserialize(String input) throws SerializationException {
        Matcher matcher = REGEX.matcher(input);
        if (!matcher.matches()) throw new SerializationException("Must be in the format " + REGEX);
        return IntBinaryTag.intBinaryTag(Integer.parseInt(matcher.group(1)));
    }

    @Override
    public String serialize(IntBinaryTag input) throws SerializationException {
        return input != null? input.value() + "i" : "0i";
    }
}
