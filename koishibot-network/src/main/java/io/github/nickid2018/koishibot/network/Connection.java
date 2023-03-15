package io.github.nickid2018.koishibot.network;

import io.github.nickid2018.koishibot.network.handler.PacketDecoder;
import io.github.nickid2018.koishibot.network.handler.PacketEncoder;
import io.github.nickid2018.koishibot.network.handler.SizePrepender;
import io.github.nickid2018.koishibot.network.handler.SplitterHandler;
import io.github.nickid2018.koishibot.util.LazyLoadedValue;
import io.github.nickid2018.koishibot.util.SimpleThreadFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.TimeoutException;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Connection extends SimpleChannelInboundHandler<SerializableData> {

    public static final LazyLoadedValue<NioEventLoopGroup> SERVER_EVENT_GROUP = new LazyLoadedValue<>(
            () -> new NioEventLoopGroup(0, new SimpleThreadFactory("Netty Server IO #%d")));

    public static final LazyLoadedValue<EpollEventLoopGroup> SERVER_EPOLL_EVENT_GROUP = new LazyLoadedValue<>(
            () -> new EpollEventLoopGroup(0, new SimpleThreadFactory("Epoll Server IO #%d")));

    public static final LazyLoadedValue<NioEventLoopGroup> NETWORK_WORKER_GROUP = new LazyLoadedValue<>(
            () -> new NioEventLoopGroup(0, new SimpleThreadFactory("Netty Client IO #%d")));

    public static final LazyLoadedValue<EpollEventLoopGroup> NETWORK_EPOLL_WORKER_GROUP = new LazyLoadedValue<>(
            () -> new EpollEventLoopGroup(0, new SimpleThreadFactory("Epoll Client IO #%d")));

    public static final Logger NETWORK_LOGGER = LoggerFactory.getLogger("Network");

    private final PacketRegistry registry;
    private final NetworkListener listener;
    private Channel channel;
    private SocketAddress address;
    private final Queue<PacketHolder> packetBuffer = new ConcurrentLinkedQueue<>();

    public Connection(PacketRegistry registry, NetworkListener listener) {
        this.registry = registry;
        this.listener = listener;
    }

    public static Connection connectToTcpServer(PacketRegistry registry, NetworkListener listener, InetAddress addr, int port) {
        return connectToTcpServer(registry, listener, addr, port, 30);
    }

    public static Connection connectToTcpServer(PacketRegistry registry, NetworkListener listener, InetAddress addr, int port, int timeout) {
        Connection connection = new Connection(registry, listener);
        Class<? extends Channel> clazz;
        LazyLoadedValue<?> lazyLoadedValue;
        if (Epoll.isAvailable()) {
            clazz = EpollSocketChannel.class;
            lazyLoadedValue = Connection.NETWORK_EPOLL_WORKER_GROUP;
        } else {
            clazz = NioSocketChannel.class;
            lazyLoadedValue = Connection.NETWORK_WORKER_GROUP;
        }
        new Bootstrap().group((EventLoopGroup) lazyLoadedValue.get()).handler(new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel channel) {
                try {
                    channel.config().setOption(ChannelOption.TCP_NODELAY, true);
                } catch (ChannelException ignored) {
                }
                channel.pipeline().addLast("timeout", new ReadTimeoutHandler(timeout))
                        .addLast("splitter", new SplitterHandler())
                        .addLast("decoder", new PacketDecoder(registry))
                        .addLast("prepender", new SizePrepender())
                        .addLast("encoder", new PacketEncoder(registry))
                        .addLast("packet_handler", connection);
            }
        }).channel(clazz).connect(addr, port).syncUninterruptibly();
        return connection;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        channel = ctx.channel();
        address = channel.remoteAddress();
        listener.connectionOpened(this);
        flushQueue();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SerializableData msg) {
        listener.receivePacket(this, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!channel.isOpen())
            return;
        if (cause instanceof TimeoutException) {
            NETWORK_LOGGER.debug("A client met a timeout", cause);
        } else {
            NETWORK_LOGGER.error("Fatal error in sending/receiving packet", cause);
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public SocketAddress getAddress() {
        return address;
    }

    public boolean isOpen() {
        return channel != null && channel.isOpen();
    }

    public boolean isConnecting() {
        return channel == null;
    }

    public void disconnect() {
        if (channel.isOpen())
            channel.close().syncUninterruptibly();
    }

    public void sendPacket(SerializableData packet) {
        sendPacket(packet, null);
    }

    public void sendPacket(SerializableData packet,
                           GenericFutureListener<? extends Future<? super Void>> listener) {
        if (!isOpen()) {
            PacketHolder holder = new PacketHolder(packet, listener);
            packetBuffer.offer(holder);
        } else {
            flushQueue();
            sendPacket0(packet, listener);
        }
    }

    private void sendPacket0(SerializableData packet,
                             GenericFutureListener<? extends Future<? super Void>> listener) {
        if (channel.eventLoop().inEventLoop()) {
            ChannelFuture channelFuture = channel.writeAndFlush(packet);
            if (listener != null)
                channelFuture.addListener(listener);
            channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        } else {
            channel.eventLoop().execute(() -> {
                ChannelFuture channelFuture = channel.writeAndFlush(packet);
                if (listener != null)
                    channelFuture.addListener(listener);
                channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            });
        }
    }

    private void flushQueue() {
        if (!isOpen())
            return;
        synchronized (packetBuffer) {
            while (!packetBuffer.isEmpty()) {
                PacketHolder holder = packetBuffer.poll();
                sendPacket0(holder.packet(), holder.listener());
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        listener.connectionClosed(this);
    }
}
