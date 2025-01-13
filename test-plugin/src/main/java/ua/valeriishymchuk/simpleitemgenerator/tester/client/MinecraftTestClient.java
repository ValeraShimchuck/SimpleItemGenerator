package ua.valeriishymchuk.simpleitemgenerator.tester.client;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.handshaking.client.WrapperHandshakingClientHandshake;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import ua.valeriishymchuk.simpleitemgenerator.tester.netty.NettyUtils;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MinecraftTestClient extends ChannelDuplexHandler {

    // TODO continue

    String username;
    @NonFinal
    Channel channel;
    @NonFinal int connectionState = 0;

    public MinecraftTestClient(String username) {
        this.username = username;
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(this);
                        }
                    });
            bootstrap.connect("localhost", 25565).sync();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            group.shutdownGracefully();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channel = ctx.channel();
        int protocol = PacketEvents.getAPI().getServerManager().getVersion().getProtocolVersion();
        WrapperHandshakingClientHandshake handshake = new WrapperHandshakingClientHandshake(
                protocol,
                "localhost",
                25565,
                WrapperHandshakingClientHandshake.ConnectionIntention.LOGIN
        );
        write(handshake);
        connectionState += 2;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof ByteBuf)) {
            throw new RuntimeException("Unknown message: " + msg);
            //return;
        }
        ByteBuf buf = (ByteBuf) msg;
        int packetId = NettyUtils.readVarInt(buf);
        System.out.println("got message: " + msg);
        //super.channelRead(ctx, msg);
    }

    public void write(Object msg) {
        if (msg instanceof PacketWrapper<?>) {
            PacketWrapper<?> wrapper = (PacketWrapper<?>) msg;
            wrapper.setBuffer(channel.alloc().directBuffer());
            wrapper.write();
            msg = wrapper.buffer;
        }
        channel.writeAndFlush(msg);
    }



}
