package ua.valeriishymchuk.simpleitemgenerator.tester.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.List;

import static ua.valeriishymchuk.simpleitemgenerator.tester.netty.NettyUtils.varIntBytes;
import static ua.valeriishymchuk.simpleitemgenerator.tester.netty.NettyUtils.writeVarInt;

/**
 * Handler for appending a length for Minecraft packets.
 */
@ChannelHandler.Sharable
public class MinecraftVarintLengthEncoder extends MessageToMessageEncoder<ByteBuf> {

  public static final MinecraftVarintLengthEncoder INSTANCE = new MinecraftVarintLengthEncoder();

  private MinecraftVarintLengthEncoder() {
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> list) throws Exception {
    final int length = buf.readableBytes();
    final int varintLength = varIntBytes(length);

    final ByteBuf lenBuf = ctx.alloc().heapBuffer(varintLength);

    writeVarInt(lenBuf, length);
    list.add(lenBuf);
    list.add(buf.retain());
  }
}
