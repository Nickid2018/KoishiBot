package io.github.nickid2018.koishibot.core;

import io.github.nickid2018.koishibot.network.*;
import io.github.nickid2018.koishibot.util.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

public class MonitorListener implements NetworkListener {

    public static final Logger LOGGER = LoggerFactory.getLogger("Monitor Connection");
    private static Connection connection;

    public static void startLink() {
        try {
            connection = Connection.connectToTcpServer(
                    new DataRegistry(), new MonitorListener(), InetAddress.getLocalHost(), Settings.CORE_PORT);
        } catch (Exception e) {
            LogUtils.error(LOGGER, "Can't link to monitor!", e);
        }
    }

    public static void sendCommand(String str) {
        connection.sendPacket(new StringData(str));
    }

    @Override
    public void connectionOpened(Connection connection) {
        LogUtils.info(LogUtils.FontColor.CYAN, LOGGER, "Core connection opened!");
    }

    @Override
    public void receivePacket(Connection connection, SerializableData packet) {

    }

    @Override
    public void connectionClosed(Connection connection) {
        LogUtils.error(LOGGER, "Core connection closed!", null);
    }
}
