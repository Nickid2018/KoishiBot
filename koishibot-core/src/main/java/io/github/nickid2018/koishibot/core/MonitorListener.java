package io.github.nickid2018.koishibot.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.nickid2018.koishibot.message.DelegateEnvironment;
import io.github.nickid2018.koishibot.message.Environments;
import io.github.nickid2018.koishibot.message.action.StopAction;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.network.*;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.LogUtils;
import io.github.nickid2018.koishibot.util.Pair;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Base64;

public class MonitorListener implements NetworkListener {

    public static final Logger LOGGER = LoggerFactory.getLogger("Monitor Connection");
    private static Connection connection;
    private static long ACTION_ID = -1;

    public static void startLink() {
        try {
            connection = Connection.connectToTcpServer(
                    new DataRegistry(), new MonitorListener(), InetAddress.getLocalHost(), Settings.CORE_PORT);
        } catch (Exception e) {
            LogUtils.error(LOGGER, "Can't link to monitor!", e);
        }
    }

    public static String formatContext(DelegateEnvironment environment, MessageContext context) {
        ByteData data = new ByteData(Unpooled.buffer());
        data.writeString(environment.getEnvironmentName());
        data.writeSerializableData(context);
        byte[] bytes = data.toByteArray();
        data.release();
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static Pair<DelegateEnvironment, MessageContext> parseContext(String context) {
        LOGGER.info("Context: " + context);
        byte[] bytes = Base64.getDecoder().decode(context);
        ByteData data = new ByteData(Unpooled.wrappedBuffer(bytes));
        String envName = data.readString();
        DelegateEnvironment environment = Environments.getEnvironment(envName);
        MessageContext messageContext = data.readSerializableData(environment.getConnection(), MessageContext.class);
        data.release();
        return new Pair<>(environment, messageContext);
    }

    public static void checkActionID(DelegateEnvironment environment, MessageContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("action", "check_action_id");
        obj.addProperty("context", formatContext(environment, context));
        connection.sendPacket(new StringData(obj.toString()));
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
                case "check_action_id" -> doCheckActionIDCallback(obj);
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

    private void doCheckActionIDCallback(JsonObject data) {
        Pair<DelegateEnvironment, MessageContext> pair = parseContext(JsonUtil.getStringOrNull(data, "context"));
        DelegateEnvironment environment = pair.first();
        MessageContext context = pair.second();
        environment.getMessageSender().sendMessage(context, environment.newText(
                switch (JsonUtil.getStringOrNull(data, "result")) {
                    case "no_update" -> "目前bot已是最新。";
                    case "update" -> "目前bot有更新。Action ID: %d -> %d".formatted(
                            ACTION_ID, JsonUtil.getLongOrZero(data, "now_action_id")
                    );
                    case "message" -> "无法获取更新信息。具体报错信息为: %s".formatted(
                            JsonUtil.getStringOrNull(data, "error")
                    );
                    default -> "<发生了错误>";
                }));
    }
}
