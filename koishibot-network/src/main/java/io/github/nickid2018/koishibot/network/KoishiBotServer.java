package io.github.nickid2018.koishibot.network;

import io.github.nickid2018.koishibot.network.handler.PacketDecoder;
import io.github.nickid2018.koishibot.network.handler.PacketEncoder;
import io.github.nickid2018.koishibot.network.handler.SizePrepender;
import io.github.nickid2018.koishibot.network.handler.SplitterHandler;
import io.github.nickid2018.koishibot.util.LazyLoadedValue;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class KoishiBotServer {

    private static final Logger SERVER_LOGGER = LoggerFactory.getLogger("Server");

    private ChannelFuture future;
    private final List<Connection> connections = Collections.synchronizedList(new ArrayList<>());
    private final int port;
    private final DataRegistry registry;
    private final NetworkListener listener;
    private boolean active;

    public KoishiBotServer(int port, DataRegistry registry, NetworkListener listener) {
        this.port = port;
        this.registry = registry;
        this.listener = listener;
    }

    public void start(int timeout) {
        Class<? extends ServerChannel> clazz;
        LazyLoadedValue<?> lazyLoadedValue;
        if (Epoll.isAvailable()) {
            clazz = EpollServerSocketChannel.class;
            lazyLoadedValue = Connection.SERVER_EPOLL_EVENT_GROUP;
            SERVER_LOGGER.info("Using epoll channel type");
        } else {
            clazz = NioServerSocketChannel.class;
            lazyLoadedValue = Connection.SERVER_EVENT_GROUP;
            SERVER_LOGGER.info("Using default channel type");
        }
        future = new ServerBootstrap().channel(clazz).childHandler(new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel channel) {
                Connection connection = new Connection(registry, listener);
                connections.add(connection);
                try {
                    channel.config().setOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
                } catch (ChannelException ignored) {
                }
                channel.pipeline()
                        .addLast("timeout", new ReadTimeoutHandler(timeout))
                        .addLast("splitter", new SplitterHandler())
                        .addLast("decoder", new PacketDecoder(connection))
                        .addLast("prepender", new SizePrepender())
                        .addLast("encoder", new PacketEncoder(registry));
                channel.pipeline().addLast("packet_handler", connection);
            }
        }).group((EventLoopGroup) lazyLoadedValue.get()).localAddress((InetAddress) null, port).bind().syncUninterruptibly();
        active = true;
    }

    public void stop() {
        if (!active)
            return;
        connections.forEach(Connection::disconnect);
        connections.clear();
        try {
            future.channel().close().sync();
        } catch (InterruptedException e) {
            SERVER_LOGGER.error("Interrupted whilst closing channel");
        }
    }

    public void checkConnections() {
        if (!active)
            return;
        connections.removeIf(Predicate.not(Connection::isOpen));
    }

    public List<Connection> getConnections() {
        return connections;
    }
}
