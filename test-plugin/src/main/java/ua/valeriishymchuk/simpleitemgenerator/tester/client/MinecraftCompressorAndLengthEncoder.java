package ua.valeriishymchuk.simpleitemgenerator.tester.client;

import static ua.valeriishymchuk.simpleitemgenerator.tester.netty.NettyUtils.*;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.Setter;
import ua.valeriishymchuk.simpleitemgenerator.tester.client.compressor.JavaCompressor;

import java.util.zip.DataFormatException;

/**
 * Handler for compressing Minecraft packets.
 */
public class MinecraftCompressorAndLengthEncoder extends MessageToByteEncoder<ByteBuf> {

    @Setter
    private int threshold;
    private final JavaCompressor compressor;

    public MinecraftCompressorAndLengthEncoder(int threshold, JavaCompressor compressor) {
        this.threshold = threshold;
        this.compressor = compressor;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        int uncompressed = msg.readableBytes();
        if (uncompressed < threshold) {
            // Under the threshold, there is nothing to do.
            writeVarInt(out, uncompressed + 1);
            writeVarInt(out, 0);
            out.writeBytes(msg);
        } else {
            handleCompressed(ctx, msg, out);
        }
    }

    private void handleCompressed(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out)
            throws DataFormatException {
        int uncompressed = msg.readableBytes();

        write21BitVarInt(out, 0); // Dummy packet length
        writeVarInt(out, uncompressed);
        ByteBuf compatibleIn = ensureCompatible(ctx.alloc(), msg);

        int startCompressed = out.writerIndex();
        try {
            compressor.deflate(compatibleIn, out);
        } finally {
            compatibleIn.release();
        }
        int compressedLength = out.writerIndex() - startCompressed;
        if (compressedLength >= 1 << 21) {
            throw new DataFormatException("The server sent a very large (over 2MiB compressed) packet.");
        }

        int writerIndex = out.writerIndex();
        int packetLength = out.readableBytes() - 3;
        out.writerIndex(0);
        write21BitVarInt(out, packetLength); // Rewrite packet length
        out.writerIndex(writerIndex);
    }


    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        compressor.close();
    }

}
