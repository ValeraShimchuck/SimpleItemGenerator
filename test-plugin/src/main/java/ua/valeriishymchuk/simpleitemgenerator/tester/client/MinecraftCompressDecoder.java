package ua.valeriishymchuk.simpleitemgenerator.tester.client;

import static ua.valeriishymchuk.simpleitemgenerator.tester.netty.NettyUtils.ensureCompatible;
import static ua.valeriishymchuk.simpleitemgenerator.tester.netty.NettyUtils.readVarInt;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import ua.valeriishymchuk.simpleitemgenerator.tester.client.compressor.JavaCompressor;

import java.util.List;

/**
 * Decompresses a Minecraft packet.
 */
public class MinecraftCompressDecoder extends MessageToMessageDecoder<ByteBuf> {

  private static final int VANILLA_MAXIMUM_UNCOMPRESSED_SIZE = 8 * 1024 * 1024; // 8MiB

  private int threshold;
  private final JavaCompressor compressor;

  public MinecraftCompressDecoder(int threshold, JavaCompressor compressor) {
    this.threshold = threshold;
    this.compressor = compressor;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    int claimedUncompressedSize = readVarInt(in);
    if (claimedUncompressedSize == 0) {
      // This message is not compressed.
      out.add(in.retain());
      return;
    }

    ByteBuf compatibleIn = ensureCompatible(ctx.alloc(), in);
    ByteBuf uncompressed = ctx.alloc().directBuffer(claimedUncompressedSize);
    try {
      compressor.inflate(compatibleIn, uncompressed, claimedUncompressedSize);
      out.add(uncompressed);
    } catch (Exception e) {
      uncompressed.release();
      throw e;
    } finally {
      compatibleIn.release();
    }
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    compressor.close();
  }

  public void setThreshold(int threshold) {
    this.threshold = threshold;
  }
}
