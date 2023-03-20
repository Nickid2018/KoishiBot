package io.github.nickid2018.koishibot.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.nickid2018.koishibot.message.Environments;
import io.github.nickid2018.koishibot.message.action.StopAction;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.network.*;
import io.github.nickid2018.koishibot.util.JsonUtil;
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
        if (packet instanceof StringData strData) {
            String str = strData.getStr();
            JsonObject obj = JsonParser.parseString(str).getAsJsonObject();
            String action = JsonUtil.getStringOrNull(obj, "action");
            switch (action) {
                case "exit" -> doExit();
            }
        }
    }

    @Override
    public void connectionClosed(Connection connection) {
        LogUtils.error(LOGGER, "Core connection closed!", null);
    }

    private void doExit() {
        Environments.getEnvironments().stream()
                .map(Environment::getConnection)
                .forEach(con -> con.sendPacket(StopAction.INSTANCE));
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            LogUtils.error(LOGGER, "Can't wait for bot to stop!", e);
        }
        BotStart.terminate();
    }
}
