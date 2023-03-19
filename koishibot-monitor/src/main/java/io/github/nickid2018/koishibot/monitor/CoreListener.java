package io.github.nickid2018.koishibot.monitor;

import io.github.nickid2018.koishibot.network.Connection;
import io.github.nickid2018.koishibot.network.DataRegistry;
import io.github.nickid2018.koishibot.network.NetworkListener;
import io.github.nickid2018.koishibot.network.SerializableData;
import io.github.nickid2018.koishibot.util.LogUtils;

public class CoreListener implements NetworkListener {

    public static final DataRegistry REGISTRY = new DataRegistry();

    @Override
    public void connectionOpened(Connection connection) {
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
