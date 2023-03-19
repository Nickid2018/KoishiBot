package io.github.nickid2018.koishibot.monitor;

import io.github.nickid2018.koishibot.network.*;
import io.github.nickid2018.koishibot.util.LogUtils;

public class CoreListener implements NetworkListener {

    public static final DataRegistry REGISTRY = new DataRegistry();
    public static final CoreListener INSTANCE = new CoreListener();

    private Connection connection;

    public void send(String data) {
        connection.sendPacket(new StringData(data));
    }

    @Override
    public void connectionOpened(Connection connection) {
        this.connection = connection;
        LogUtils.info(LogUtils.FontColor.CYAN, MonitorStart.LOGGER, "Core connection opened!");
    }

    @Override
    public void receivePacket(Connection connection, SerializableData packet) {

    }

    @Override
    public void connectionClosed(Connection connection) {
        LogUtils.error(MonitorStart.LOGGER, "Core connection closed!", null);
    }
}
