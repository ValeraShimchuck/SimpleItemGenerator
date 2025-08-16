package ua.valeriishymchuk.simpleitemgenerator.common.wrapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import ua.valeriishymchuk.simpleitemgenerator.common.annotation.UsesMinecraft;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class PersistentDataTypeWrapper {

    @UsesMinecraft
    public abstract <I, O> PersistentDataType<I, O> toBukkit();


    public abstract int hashCode();

    public static final class Byte extends PersistentDataTypeWrapper {
        @Override
        @UsesMinecraft
        public PersistentDataType<java.lang.Byte, java.lang.Byte> toBukkit() {
            return PersistentDataType.BYTE;
        }

        @Override
        public int hashCode() {
            return 100;
        }
    }

    public static final class Short extends PersistentDataTypeWrapper {
        @Override
        @UsesMinecraft
        public PersistentDataType<java.lang.Short, java.lang.Short> toBukkit() {
            return PersistentDataType.SHORT;
        }

        @Override
        public int hashCode() {
            return 1111;
        }
    }

    public static final class Integer extends PersistentDataTypeWrapper {
        @Override
        @UsesMinecraft
        public PersistentDataType<java.lang.Integer, java.lang.Integer> toBukkit() {
            return PersistentDataType.INTEGER;
        }

        @Override
        public int hashCode() {
            return 2222;
        }
    }

    public static final class Long extends PersistentDataTypeWrapper {
        @Override
        @UsesMinecraft
        public PersistentDataType<java.lang.Long, java.lang.Long> toBukkit() {
            return PersistentDataType.LONG;
        }

        @Override
        public int hashCode() {
            return 3333;
        }
    }

    public static final class Float extends PersistentDataTypeWrapper {
        @Override
        @UsesMinecraft
        public PersistentDataType<java.lang.Float, java.lang.Float> toBukkit() {
            return PersistentDataType.FLOAT;
        }

        @Override
        public int hashCode() {
            return 4444;
        }
    }

    public static final class Double extends PersistentDataTypeWrapper {
        @Override
        @UsesMinecraft
        public PersistentDataType<java.lang.Double, java.lang.Double> toBukkit() {
            return PersistentDataType.DOUBLE;
        }

        @Override
        public int hashCode() {
            return 5555;
        }
    }

    public static final class String extends PersistentDataTypeWrapper {
        @Override
        @UsesMinecraft
        public PersistentDataType<java.lang.String, java.lang.String> toBukkit() {
            return PersistentDataType.STRING;
        }

        @Override
        public int hashCode() {
            return 6666;
        }
    }

    public static final class ByteArray extends PersistentDataTypeWrapper {
        @Override
        @UsesMinecraft
        public PersistentDataType<byte[], byte[]> toBukkit() {
            return PersistentDataType.BYTE_ARRAY;
        }

        @Override
        public int hashCode() {
            return 7777;
        }
    }

    public static final class IntegerArray extends PersistentDataTypeWrapper {
        @Override
        @UsesMinecraft
        public PersistentDataType<int[], int[]> toBukkit() {
            return PersistentDataType.INTEGER_ARRAY;
        }

        @Override
        public int hashCode() {
            return 8888;
        }
    }

    public static final class LongArray extends PersistentDataTypeWrapper {
        @Override
        @UsesMinecraft
        public PersistentDataType<long[], long[]> toBukkit() {
            return PersistentDataType.LONG_ARRAY;
        }

        @Override
        public int hashCode() {
            return 9999;
        }
    }

    public static final class TagContainer extends PersistentDataTypeWrapper {
        @Override
        @UsesMinecraft
        public PersistentDataType<PersistentDataContainer, PersistentDataContainer> toBukkit() {
            return PersistentDataType.TAG_CONTAINER;
        }

        @Override
        public int hashCode() {
            return 10101010;
        }
    }

    public static final class TagContainerArray extends PersistentDataTypeWrapper {
        @Override
        @UsesMinecraft
        public PersistentDataType<PersistentDataContainer[], PersistentDataContainer[]> toBukkit() {
            return PersistentDataType.TAG_CONTAINER_ARRAY;
        }

        @Override
        public int hashCode() {
            return 11111111;
        }
    }







}
