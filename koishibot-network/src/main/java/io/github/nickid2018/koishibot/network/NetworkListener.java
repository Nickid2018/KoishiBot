package io.github.nickid2018.koishibot.network;

public interface NetworkListener {

    void connectionOpened(Connection connection);

    void receivePacket(Connection connection, SerializableData packet);

    void connectionClosed(Connection connection);
}
