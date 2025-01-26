package ua.valeriishymchuk.simpleitemgenerator.common.nbt;

import net.kyori.adventure.nbt.ShortBinaryTag;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShortBinaryTagSerializer implements StringSerializer<ShortBinaryTag> {

    private static final Pattern REGEX = Pattern.compile("^(\\d+)[sS]$");

    public static final ShortBinaryTagSerializer INSTANCE = new ShortBinaryTagSerializer();

    private ShortBinaryTagSerializer() {}

    @Override
    public ShortBinaryTag deserialize(String input) throws SerializationException {
        Matcher matcher = REGEX.matcher(input);
        if (!matcher.matches()) throw new SerializationException("Must be in the format " + REGEX);
        return ShortBinaryTag.shortBinaryTag(Short.parseShort(matcher.group(1)));
    }

    @Override
    public String serialize(ShortBinaryTag input) throws SerializationException {
        return input != null? input.value() + "s" : "0s";
    }
}
