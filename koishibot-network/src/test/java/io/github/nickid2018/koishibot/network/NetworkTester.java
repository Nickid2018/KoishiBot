package io.github.nickid2018.koishibot.network;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetworkTester {

    public static void main(String[] args) throws UnknownHostException {
        PacketRegistry registry = new PacketRegistry();
        registry.registerPacket(StringData.class);
        new Thread(() -> {
            KoishiBotServer server = new KoishiBotServer(25565, registry, new NetworkListener() {
                @Override
                public void connectionOpened(Connection connection) {
                    System.out.println("Server connection opened.");
                }

                @Override
                public void receivePacket(Connection connection, SerializableData packet) {
                    System.out.println("Server received packet: " + ((StringData) packet).data);
                    connection.sendPacket(new StringData("Hello, client!"));
                }

                @Override
                public void connectionClosed(Connection connection) {
                    System.out.println("Server connection closed.");
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
                    System.out.println("No client connected, stopping server.");
                    server.stop();
                    break;
                } else if (server.getConnections().size() > 0)
                    lastCheckHaveClients = System.currentTimeMillis();
            }
        }).start();
        Connection connection = Connection.connectToTcpServer(registry, new NetworkListener() {
            @Override
            public void connectionOpened(Connection connection) {
                System.out.println("Client connection opened.");
                connection.sendPacket(new StringData("Hello, server!"));
            }

            @Override
            public void receivePacket(Connection connection, SerializableData packet) {
                System.out.println("Client received packet: " + ((StringData) packet).data);
            }

            @Override
            public void connectionClosed(Connection connection) {
                System.out.println("Client connection closed.");
            }
        }, InetAddress.getLocalHost(), 25565);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
        }
        connection.disconnect();
    }
}
