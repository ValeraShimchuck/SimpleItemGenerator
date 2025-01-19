package ua.valeriishymchuk.simpleitemgenerator.tester.client;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.simple.PacketConfigSendEvent;
import com.github.retrooper.packetevents.event.simple.PacketLoginSendEvent;
import com.github.retrooper.packetevents.event.simple.PacketPlaySendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientConfigurationEndAck;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientSelectKnownPacks;
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerSelectKnownPacks;
import com.github.retrooper.packetevents.wrapper.handshaking.client.WrapperHandshakingClientHandshake;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginSuccessAck;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerLoginSuccess;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerSetCompression;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import ua.valeriishymchuk.simpleitemgenerator.tester.client.compressor.JavaCompressor;
import ua.valeriishymchuk.simpleitemgenerator.tester.netty.NettyUtils;
import ua.valeriishymchuk.simpleitemgenerator.tester.version.VersionUtils;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import static ua.valeriishymchuk.simpleitemgenerator.tester.netty.NettyUtils.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MinecraftTestClient extends ChannelDuplexHandler {

    // TODO continue

    private static final String FRAME_ENCODER = "frame-encoder";
    private static final String FRAME_DECODER = "frame-decoder";


    UserProfile user;
    @Getter
    @NonFinal
    Channel channel;
    @Getter
    @NonFinal
    int connectionState = 0;
    Consumer<PacketSendEvent> handler;
    NioEventLoopGroup group;
    @NonFinal int writeId = 0;
    Map<Integer, CompletableFuture<Void>> writesFuture = new ConcurrentHashMap<>();
    Multimap<PacketTypeCommon, CompletableFuture<PacketSendEvent>> nextReadFutures = Multimaps.synchronizedMultimap(
            ArrayListMultimap.create()
    );

    public MinecraftTestClient(UserProfile user, Consumer<PacketSendEvent> handler) {
        this.handler = handler;
        this.user = user;
        group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline()
                                    .addLast(FRAME_DECODER, new MinecraftVarintFrameDecoder())
                                    .addLast(FRAME_ENCODER, MinecraftVarintLengthEncoder.INSTANCE)
                                    .addLast(MinecraftTestClient.this);
                        }
                    });
            bootstrap.connect("localhost", 25565).sync();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListener() {

            private int innerReadId = 0;

            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                innerReadId++;
                writesFuture.remove(innerReadId).complete(null);
            }
        }, PacketListenerPriority.MONITOR);
    }

    public void handleNext(PacketTypeCommon type, Consumer<PacketSendEvent> consumer) {
        CompletableFuture<PacketSendEvent> future = new CompletableFuture<>();
        future.thenAccept(consumer);
        nextReadFutures.put(type , future);
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channel = ctx.channel();
        int protocol = getVersion().getProtocolVersion();
        WrapperHandshakingClientHandshake handshake = new WrapperHandshakingClientHandshake(
                protocol,
                "localhost",
                ((InetSocketAddress) channel.remoteAddress()).getPort(),
                WrapperHandshakingClientHandshake.ConnectionIntention.LOGIN
        );
        write(handshake);
        connectionState += 2;
        WrapperLoginClientLoginStart login = new WrapperLoginClientLoginStart(
                getVersion(),
                user.getName(),
                null,
                user.getUUID()
        );
        write(login);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace(System.err);
        channel.close();
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise future) throws Exception {
        super.close(ctx, future);
        group.shutdownGracefully();
    }

    private ClientVersion getVersion() {
        return PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof ByteBuf)) {
            throw new RuntimeException("Unknown message: " + msg);
            //return;
        }
        ByteBuf buf = (ByteBuf) msg;
        if (buf.refCnt() == 0) {
            throw new RuntimeException("Already freed: " + msg);
        }
        ConnectionState state = ConnectionState.getById(connectionState);
        PacketSendEvent event;
        User user = new User(
                channel, state, getVersion(), this.user
        );
        switch (state) {
            case LOGIN:
                event = new PacketLoginSendEvent(channel, user, user, buf, false);
                break;
            case CONFIGURATION:
                event = new PacketConfigSendEvent(channel, user, user, buf, false);
                break;
            case PLAY:
                event = new PacketPlaySendEvent(channel, user, user, buf, false);
                break;
            default:
                throw new RuntimeException("Unknown state: " + state);
        }
        //System.out.println("got message: " + msg);
        nextReadFutures.removeAll(event.getPacketType())
                .forEach(future -> future.complete(event));
        if (!internalHandler(event)) handler.accept(event);
        buf.release();
        //channel.close().sync();
        //super.channelRead(ctx, msg);
    }

    private boolean internalHandler(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Login.Server.SET_COMPRESSION) {
            WrapperLoginServerSetCompression setCompression = new WrapperLoginServerSetCompression(event);
            //channel.pipeline().remove(MinecraftVarintFrameDecoder.class);
            channel.pipeline().remove(FRAME_ENCODER);
            JavaCompressor compressor = new JavaCompressor(3);
            channel.pipeline().addAfter(FRAME_DECODER, "compression-decoder",
                    new MinecraftCompressDecoder(setCompression.getThreshold(), compressor)
            );
            channel.pipeline()
                    .addBefore(
                            FRAME_DECODER,
                            "compression-encoder",
                            new MinecraftCompressorAndLengthEncoder(setCompression.getThreshold(), compressor)
                    );
            return true;
        }
        if (event.getPacketType() == PacketType.Login.Server.LOGIN_SUCCESS) {
            VersionUtils.runSince(ServerVersion.V_1_20_2, () -> {
                WrapperLoginClientLoginSuccessAck loginSuccessAck = new WrapperLoginClientLoginSuccessAck();
                write(loginSuccessAck);
                connectionState += 1;
            });
            connectionState += 1;
            return true;
        }
        if (event.getPacketType() == PacketType.Configuration.Server.CONFIGURATION_END) {
            WrapperConfigClientConfigurationEndAck configurationEndAck = new WrapperConfigClientConfigurationEndAck();
            write(configurationEndAck);
            connectionState -= 1;
            return true;
        }

        if (event.getPacketType() == PacketType.Configuration.Server.SELECT_KNOWN_PACKS) {
            WrapperConfigServerSelectKnownPacks serverPacks = new WrapperConfigServerSelectKnownPacks(event);
            WrapperConfigClientSelectKnownPacks selectKnownPacks = new WrapperConfigClientSelectKnownPacks(
                    serverPacks.getKnownPacks()
            );
            write(selectKnownPacks);
            return true;
        }

        if (!(event instanceof PacketPlaySendEvent)) {
            //System.out.println("Unhandled packet: " + event.getPacketType());
            return true;
        }
        return false;
    }

    public CompletableFuture<Void> write(Object msg) {
        if (msg instanceof PacketWrapper<?>) {
            PacketWrapper<?> wrapper = (PacketWrapper<?>) msg;
            ByteBuf buffer = channel.alloc().directBuffer();
            wrapper.setBuffer(buffer);
            wrapper.write();

            ByteBuf idBuffer = channel.alloc().directBuffer();
            NettyUtils.writeVarInt(idBuffer, wrapper.getNativePacketId());
            idBuffer.writeBytes(buffer);
            buffer.release();
            msg = idBuffer;
        }
        writeId++;
        CompletableFuture<Void> writeFuture = new CompletableFuture<>();
        writesFuture.put(writeId, writeFuture);
        channel.writeAndFlush(msg);
        return writeFuture;
    }


}
