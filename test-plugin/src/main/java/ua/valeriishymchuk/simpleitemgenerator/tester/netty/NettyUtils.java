package ua.valeriishymchuk.simpleitemgenerator.tester.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledDirectByteBuf;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.DecoderException;

public class NettyUtils {


    private static final int[] VAR_INT_LENGTHS = new int[65];
    private static final int MAXIMUM_VARINT_SIZE = 5;

    public static int getIntLE(ByteBuf byteBuf, int index) {
        return byteBuf.getByte(index) & 0xff        |
                (byteBuf.getByte(index + 1) & 0xff) << 8  |
                (byteBuf.getByte(index + 2) & 0xff) << 16 |
                (byteBuf.getByte(index + 3) & 0xff) << 24;
    }

    public static ByteBuf readRetainedSlice(ByteBuf byteBuf, int length) {
        ByteBuf slice = byteBuf.slice(byteBuf.readerIndex(), length);
        byteBuf.readerIndex(byteBuf.readerIndex() + length);
        slice.retain();
        return slice;
    }

    private static DecoderException badVarint() {
        return new CorruptedFrameException("Bad VarInt decoded");
    }

    public static int readVarInt(ByteBuf buf) {
        int readable = buf.readableBytes();
        if (readable == 0) {
            // special case for empty buffer
            throw badVarint();
        }

        // we can read at least one byte, and this should be a common case
        int k = buf.readByte();
        if ((k & 0x80) != 128) {
            return k;
        }

        // in case decoding one byte was not enough, use a loop to decode up to the next 4 bytes
        int maxRead = Math.min(MAXIMUM_VARINT_SIZE, readable);
        int i = k & 0x7F;
        for (int j = 1; j < maxRead; j++) {
            k = buf.readByte();
            i |= (k & 0x7F) << j * 7;
            if ((k & 0x80) != 128) {
                return i;
            }
        }
        throw badVarint();
    }

    /**
     * Returns the exact byte size of {@code value} if it were encoded as a VarInt.
     *
     * @param value the value to encode
     * @return the byte size of {@code value} if encoded as a VarInt
     */
    public static int varIntBytes(int value) {
        return VAR_INT_LENGTHS[Integer.numberOfLeadingZeros(value)];
    }

    /**
     * Writes a Minecraft-style VarInt to the specified {@code buf}.
     *
     * @param buf   the buffer to read from
     * @param value the integer to write
     */
    public static void writeVarInt(ByteBuf buf, int value) {
        // Peel the one and two byte count cases explicitly as they are the most common VarInt sizes
        // that the proxy will write, to improve inlining.
        if ((value & (0xFFFFFFFF << 7)) == 0) {
            buf.writeByte(value);
        } else if ((value & (0xFFFFFFFF << 14)) == 0) {
            int w = (value & 0x7F | 0x80) << 8 | (value >>> 7);
            buf.writeShort(w);
        } else {
            writeVarIntFull(buf, value);
        }
    }

    private static void writeVarIntFull(ByteBuf buf, int value) {
        // See https://steinborn.me/posts/performance/how-fast-can-you-write-a-varint/

        // This essentially is an unrolled version of the "traditional" VarInt encoding.
        if ((value & (0xFFFFFFFF << 7)) == 0) {
            buf.writeByte(value);
        } else if ((value & (0xFFFFFFFF << 14)) == 0) {
            int w = (value & 0x7F | 0x80) << 8 | (value >>> 7);
            buf.writeShort(w);
        } else if ((value & (0xFFFFFFFF << 21)) == 0) {
            int w = (value & 0x7F | 0x80) << 16 | ((value >>> 7) & 0x7F | 0x80) << 8 | (value >>> 14);
            buf.writeMedium(w);
        } else if ((value & (0xFFFFFFFF << 28)) == 0) {
            int w = (value & 0x7F | 0x80) << 24 | (((value >>> 7) & 0x7F | 0x80) << 16)
                    | ((value >>> 14) & 0x7F | 0x80) << 8 | (value >>> 21);
            buf.writeInt(w);
        } else {
            int w = (value & 0x7F | 0x80) << 24 | ((value >>> 7) & 0x7F | 0x80) << 16
                    | ((value >>> 14) & 0x7F | 0x80) << 8 | ((value >>> 21) & 0x7F | 0x80);
            buf.writeInt(w);
            buf.writeByte(value >>> 28);
        }
    }

    /**
     * Writes the specified {@code value} as a 21-bit Minecraft VarInt to the specified {@code buf}.
     * The upper 11 bits will be discarded.
     *
     * @param buf   the buffer to read from
     * @param value the integer to write
     */
    public static void write21BitVarInt(ByteBuf buf, int value) {
        // See https://steinborn.me/posts/performance/how-fast-can-you-write-a-varint/
        int w = (value & 0x7F | 0x80) << 16 | ((value >>> 7) & 0x7F | 0x80) << 8 | (value >>> 14);
        buf.writeMedium(w);
    }


    public static ByteBuf ensureCompatible(ByteBufAllocator alloc, ByteBuf buf) {
        // It's not, so we must make a direct copy.
        ByteBuf newBuf = alloc.directBuffer(buf.readableBytes());
        newBuf.writeBytes(buf);
        return newBuf;
    }

    public static void dumpBuf(ByteBuf buf) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buf.readableBytes(); i++) {
            sb.append(String.format("%02x ", buf.getUnsignedByte(i)));
        }
        System.out.println(sb);
    }

    static {
        for (int i = 0; i <= 32; ++i) {
            VAR_INT_LENGTHS[i] = (int) Math.ceil((31d - (i - 1)) / 7d);
        }
        VAR_INT_LENGTHS[32] = 1; // Special case for the number 0.
    }

}
