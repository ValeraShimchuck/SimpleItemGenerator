package ua.valeriishymchuk.simpleitemgenerator.common.nbt;

import net.kyori.adventure.nbt.DoubleBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DoubleBinaryTagSerializer implements StringSerializer<DoubleBinaryTag> {

    private static final Pattern REGEX = Pattern.compile("^(?:(\\d*[,.]\\d+|\\d+[,.]\\d*)[dD]?|(\\d+)[dD])$");

    public static final DoubleBinaryTagSerializer INSTANCE = new DoubleBinaryTagSerializer();

    private DoubleBinaryTagSerializer() {}

    @Override
    public DoubleBinaryTag deserialize(String input) throws SerializationException {
        Matcher matcher = REGEX.matcher(input);
        if (!matcher.matches()) throw new SerializationException("Must be in the format " + REGEX);
        String firstGroup = matcher.group(1);
        String secondGroup = matcher.groupCount() > 2? matcher.group(2) : null;
        double result;
        if (firstGroup != null) result = Double.parseDouble(firstGroup);
        else if (secondGroup != null) result = Double.parseDouble(secondGroup);
        else result = 0.0;
        return DoubleBinaryTag.doubleBinaryTag(result);
    }

    @Override
    public String serialize(DoubleBinaryTag input) throws SerializationException {
        return input != null? input.value() + "d" : "0d";
    }

    @Override
    public @Nullable DoubleBinaryTag emptyValue(Type specificType, ConfigurationOptions options) {
        return DoubleBinaryTag.doubleBinaryTag(0.0);
    }
}
