package io.github.nickid2018.koishibot.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetworkTester {

    public static void main(String[] args) throws UnknownHostException {
        DataRegistry registry = new DataRegistry();
        registry.registerData(StringData.class);
        Logger logger = LoggerFactory.getLogger("Test");
        new Thread(() -> {
            KoishiBotServer server = new KoishiBotServer(25565, registry, new NetworkListener() {
                @Override
                public void connectionOpened(Connection connection) {
                    logger.info("Server connection opened.");
                }

                @Override
                public void receivePacket(Connection connection, SerializableData packet) {
                    logger.info("Server received packet: " + ((StringData) packet).data);
                    connection.sendPacket(new StringData("Hello, client!"));
                }

                @Override
                public void connectionClosed(Connection connection) {
                    logger.info("Server connection closed.");
                }
            });
            server.start(30);
            long lastCheckHaveClients = System.currentTimeMillis();
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                server.checkConnections();
                if (server.getConnections().size() == 0 && System.currentTimeMillis() - lastCheckHaveClients > 30000) {
                    logger.info("No client connected, stopping server.");
                    server.stop();
                    break;
                } else if (server.getConnections().size() > 0)
                    lastCheckHaveClients = System.currentTimeMillis();
            }
        }).start();
        Connection connection = Connection.connectToTcpServer(registry, new NetworkListener() {
            @Override
            public void connectionOpened(Connection connection) {
                logger.info("Client connection opened.");
                connection.sendPacket(new StringData("Hello, server!"));
            }

            @Override
            public void receivePacket(Connection connection, SerializableData packet) {
                logger.info("Client received packet: " + ((StringData) packet).data);
            }

            @Override
            public void connectionClosed(Connection connection) {
                logger.info("Client connection closed.");
            }
        }, InetAddress.getLocalHost(), 25565);
        for (int i = 0; i < 100; i++)
            connection.sendPacket(new StringData("Hello, server!" + i));
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
        }
        connection.disconnect();
    }
}
