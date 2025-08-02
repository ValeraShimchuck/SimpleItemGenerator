package ua.valeriishymchuk.simpleitemgenerator.common.config.serializer.nbt.v2;

import com.florianingerl.util.regex.Matcher;
import com.florianingerl.util.regex.Pattern;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import io.vavr.Tuple;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import ua.valeriishymchuk.libs.net.kyori.adventure.nbt.*;
import org.apache.commons.lang.ArrayUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CompoundBinaryTagTypeSerializer implements TypeSerializer<CompoundBinaryTag> {

    // format: <key>[type]: <value>

    private static final Pattern TYPE_PATTERN = Pattern.compile("(?<type>byte|short|int|long|float|double|string|byte_array|int_array|long_array|compound|list:(?<list_type>(?'type')))");
    private static final Pattern KEY_PATTERN = Pattern.compile("^(?<key>.*)\\[(?<type>byte|short|int|long|float|double|string|byte_array|int_array|long_array|compound|list:(?'type'))]$");

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(staticName = "of")
    @Getter
    public static class BinaryTagInfo {
        String key;
        BinaryTagType<? extends BinaryTag> type;
        Function<Object, BinaryTag> parser;
        Function<BinaryTag, Object> serializer;
    }


    public static final BiMap<String, BinaryTagInfo> TYPES_IDS = io.vavr.collection.List
            .of(
                    BinaryTagInfo.of(
                            "byte",
                            BinaryTagTypes.BYTE,
                            o -> ByteBinaryTag.byteBinaryTag(Byte.parseByte(o.toString())),
                            b -> ((ByteBinaryTag) b).value()
                    ),
                    BinaryTagInfo.of(
                            "short",
                            BinaryTagTypes.SHORT,
                            o -> ShortBinaryTag.shortBinaryTag(Short.parseShort(o.toString())),
                            b -> ((ShortBinaryTag) b).value()
                    ),
                    BinaryTagInfo.of(
                            "int",
                            BinaryTagTypes.INT,
                            o -> IntBinaryTag.intBinaryTag(Integer.parseInt(o.toString())),
                            b -> ((IntBinaryTag) b).value()
                    ),
                    BinaryTagInfo.of(
                            "long",
                            BinaryTagTypes.LONG,
                            o -> LongBinaryTag.longBinaryTag(Long.parseLong(o.toString())),
                            b -> ((LongBinaryTag) b).value()
                    ),
                    BinaryTagInfo.of(
                            "float",
                            BinaryTagTypes.FLOAT,
                            o -> FloatBinaryTag.floatBinaryTag(Float.parseFloat(o.toString())),
                            b -> ((FloatBinaryTag) b).value()
                    ),
                    BinaryTagInfo.of(
                            "double",
                            BinaryTagTypes.DOUBLE,
                            o -> DoubleBinaryTag.doubleBinaryTag(Double.parseDouble(o.toString())),
                            b -> ((DoubleBinaryTag) b).value()
                    ),
                    BinaryTagInfo.of(
                            "string",
                            BinaryTagTypes.STRING,
                            o -> StringBinaryTag.stringBinaryTag(o.toString()),
                            b -> ((StringBinaryTag) b).value()
                    ),
                    BinaryTagInfo.of(
                            "byte_array",
                            BinaryTagTypes.BYTE_ARRAY,
                            o -> {
                                byte[] bytes = ArrayUtils.toPrimitive(((List<Object>) o).stream()
                                        .map(Object::toString)
                                        .map(Byte::parseByte).toArray(Byte[]::new));
                                return ByteArrayBinaryTag.byteArrayBinaryTag(bytes);
                            },
                            b -> {
                                byte[] array = ((ByteArrayBinaryTag) b).value();
                                return ArrayUtils.toObject(array);
                            }
                    ),
                    BinaryTagInfo.of(
                            "int_array",
                            BinaryTagTypes.INT_ARRAY,
                            o -> {
                                int[] bytes = ArrayUtils.toPrimitive(((List<Object>) o).stream()
                                        .map(Object::toString)
                                        .map(Integer::parseInt).toArray(Integer[]::new));
                                return IntArrayBinaryTag.intArrayBinaryTag(bytes);
                            },
                            b -> {
                                int[] array = ((IntArrayBinaryTag) b).value();
                                return ArrayUtils.toObject(array);
                            }
                    ),
                    BinaryTagInfo.of(
                            "long_array",
                            BinaryTagTypes.LONG_ARRAY,
                            o -> {
                                long[] bytes = ArrayUtils.toPrimitive(((List<Object>) o).stream()
                                        .map(Object::toString)
                                        .map(Long::parseLong).toArray(Long[]::new));
                                return LongArrayBinaryTag.longArrayBinaryTag(bytes);
                            },
                            b -> {
                                long[] array = ((LongArrayBinaryTag) b).value();
                                return ArrayUtils.toObject(array);
                            }
                    ),
                    BinaryTagInfo.of(
                            "list",
                            BinaryTagTypes.LIST,
                            o -> {
                                throw new IllegalArgumentException("List");
                            },
                            b -> {
                                throw new IllegalArgumentException("List");
                            }
                    ),
                    BinaryTagInfo.of(
                            "compound",
                            BinaryTagTypes.COMPOUND,
                            o -> {
                                throw new IllegalArgumentException("Compound");
                            },
                            b -> {
                                throw new IllegalArgumentException("Compound");
                            }
                    )
            )
            .transform(m -> ImmutableBiMap.copyOf(m.toJavaMap(t -> Tuple.of(t.key, t))));


    public static final Map<? extends BinaryTagType<? extends BinaryTag>, String> TYPES_IDS_BY_TYPE = io.vavr.collection.HashMap
            .ofAll(TYPES_IDS.inverse()).mapKeys(k -> k.type).toJavaMap();

    @Override
    public CompoundBinaryTag deserialize(Type type, ConfigurationNode node) throws SerializationException {
        if (node.isNull()) return CompoundBinaryTag.empty();
        if (!node.isMap()) throw new SerializationException("Must be a map");
        Map<String, Object> rawCompound = (Map<String, Object>) node.raw();
        return parseCompound(rawCompound);
    }

    @Override
    public @Nullable CompoundBinaryTag emptyValue(Type specificType, ConfigurationOptions options) {
        return CompoundBinaryTag.empty();
    }

    private CompoundBinaryTag parseCompound(Map<String, Object> rawCompound) {
        CompoundBinaryTag.Builder compound = CompoundBinaryTag.builder();
        for (Map.Entry<String, Object> entry : rawCompound.entrySet()) {
            //BinaryTagInfo info = TYPES_IDS.get(entry.getKey());
            Matcher keyMatcher = KEY_PATTERN.matcher(entry.getKey());
            if (!keyMatcher.matches()) throw new IllegalArgumentException("Invalid compound key: " + entry.getKey());
            String internalKey = keyMatcher.group("key");
            String internalType = keyMatcher.group("type");
            //if (info == null) throw new IllegalArgumentException("Invalid compound key: " + entry.getKey());
            compound.put(internalKey, sneakyParse(internalType, entry.getValue()));
        }
        return compound.build();
    }

    @SneakyThrows
    private BinaryTag sneakyParse(String type, Object value) {
        return parse(type, value);
    }

    //private CompoundBinaryTag parseCompound(String )


    private BinaryTag parse(String type, Object value) throws SerializationException {
        if (value == null) throw new SerializationException("Must provide a value");
        Matcher matcher = TYPE_PATTERN.matcher(type);
        if (!matcher.matches()) throw new IllegalArgumentException("Invalid type: " + type);
        String listType = matcher.group("list_type");
        if (listType != null) {
            List<Object> rawList = (List<Object>) value;
            Matcher listTypeMatcher = TYPE_PATTERN.matcher(listType);
            if (!listTypeMatcher.matches()) throw new IllegalArgumentException("Invalid list type: " + listType);
            String listTypeFirst = listType.split(":")[0];
            BinaryTagType<? extends BinaryTag> binaryListType = TYPES_IDS.get(listTypeFirst).getType();
            return ListBinaryTag.listBinaryTag(
                    binaryListType,
                    rawList.stream().map(obj -> sneakyParse(listType, obj))
                            .collect(Collectors.toList())
            );
        }
        if (type.equals("compound")) {
            Map<String, Object> rawCompound = (Map<String, Object>) value;
            return parseCompound(rawCompound);
        }
        return TYPES_IDS.get(type).getParser().apply(value);
    }

    @Override
    public void serialize(Type type, @Nullable CompoundBinaryTag obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.set(null);
            return;
        }
        Map<String, Object> rawCompound = serializeCompound(obj);
        node.set(rawCompound);
    }

    private Map<String, Object> serializeCompound(CompoundBinaryTag obj) {
        Map<String, Object> rawCompound = new HashMap<>();
        obj.forEach((entry) -> {
            String key = entry.getKey();
            BinaryTag value = entry.getValue();
            rawCompound.put(key + "[" + getType(value) + "]", serializeTag(value));
        });
        return rawCompound;
    }

    private String getType(BinaryTag tag) {
        if (tag instanceof ListBinaryTag) {
            return "list:" + TYPES_IDS_BY_TYPE.get(((ListBinaryTag) tag).type());
        }
        return TYPES_IDS_BY_TYPE.get(tag.type());
    }

    private Object serializeTag(BinaryTag value) {
        if (value instanceof ListBinaryTag) {
            ListBinaryTag listBinaryTag = (ListBinaryTag) value;
            BinaryTagType<? extends BinaryTag> type = listBinaryTag.type();
            return listBinaryTag.stream().map(this::serializeTag).collect(Collectors.toList());
        }
        if (value instanceof CompoundBinaryTag) {
            CompoundBinaryTag compoundBinaryTag = (CompoundBinaryTag) value;
            return serializeCompound(compoundBinaryTag);
        }
        String type = TYPES_IDS_BY_TYPE.get(value.type());
        return  TYPES_IDS.get(type).getSerializer().apply(value);
    }
}
