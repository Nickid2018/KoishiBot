package io.github.nickid2018.koishibot.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.nickid2018.koishibot.message.DelegateEnvironment;
import io.github.nickid2018.koishibot.message.Environments;
import io.github.nickid2018.koishibot.message.action.StopAction;
import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.message.api.TextMessage;
import io.github.nickid2018.koishibot.network.*;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.LogUtils;
import io.github.nickid2018.koishibot.util.Pair;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MonitorListener implements NetworkListener {

    public static final Logger LOGGER = LoggerFactory.getLogger("Monitor Connection");
    private static Connection connection;
    private static long ACTION_ID = -1;

    public static final Map<String, String> BACKEND_TO_NAME_MAP = Map.of(
            "qq-backend", "QQ",
            "telegram-backend", "Telegram",
            "kook-backend", "开黑啦"
    );

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

    public static void doUpdate(DelegateEnvironment environment, MessageContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("action", "update");
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
                case "start" -> ACTION_ID = JsonUtil.getLongOrElse(obj, "action_id", -1);
                case "exit" -> doExit();
                case "stops" -> stopModule(obj);
                case "check_action_id" -> doCheckActionIDCallback(obj);
                case "update" -> doUpdateCallback(obj);
                case "update_finish" -> doUpdateFinishedCallback(obj);
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
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            LogUtils.error(LOGGER, "Can't wait for bot to stop!", e);
        }
        BotStart.terminate();
    }

    private void stopModule(JsonObject data) {
        JsonArray stops = data.getAsJsonArray("stops");
        Set<String> stopSet = new HashSet<>();
        for (int i = 0; i < stops.size(); i++) {
            String name = stops.get(i).getAsString();
            stopSet.add(BACKEND_TO_NAME_MAP.getOrDefault(name, name));
        }
        LogUtils.info(LogUtils.FontColor.RED, LOGGER, "Stopping modules: {}", stopSet);
        Environments.getEnvironments().stream()
                .filter(env -> stopSet.contains(env.getEnvironmentName()))
                .map(Environment::getConnection)
                .forEach(con -> con.sendPacket(StopAction.INSTANCE));
        if (stopSet.contains("core")) {
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException e) {
                LogUtils.error(LOGGER, "Can't wait for bot to stop!", e);
            }
            BotStart.terminate();
        }
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
                            JsonUtil.getStringOrNull(data, "message")
                    );
                    default -> "<发生了错误>";
                }));
    }

    private void doUpdateCallback(JsonObject data) {
        LogUtils.info(LogUtils.FontColor.CYAN, LOGGER, "Updated bot...");
        Pair<DelegateEnvironment, MessageContext> pair = parseContext(JsonUtil.getStringOrNull(data, "context"));
        DelegateEnvironment environment = pair.first();
        MessageContext context = pair.second();
        switch (JsonUtil.getStringOrNull(data, "result")) {
            case "no_update" -> {
                environment.getMessageSender().sendMessage(context, environment.newText("目前bot已是最新。"));
                ACTION_ID = JsonUtil.getLongOrZero(data, "action_id");
            }
            case "can_update" -> {
                StringBuilder builder = new StringBuilder();
                builder.append("bot可更新到最新版本%d\n".formatted(JsonUtil.getLongOrZero(data, "action_id")));
                builder.append("将要更新的模块为:\n");
                JsonArray modules = data.getAsJsonArray("update_cores");
                builder.append(StreamSupport.stream(modules.spliterator(), false)
                        .map(JsonElement::getAsString).collect(Collectors.joining(", ")));
                builder.append("\n将要更新的支持库为:\n");
                JsonArray libs = data.getAsJsonArray("update_libs");
                builder.append(StreamSupport.stream(libs.spliterator(), false)
                        .map(JsonElement::getAsString).collect(Collectors.joining(", ")));
                builder.append("\n是否更新？(打y确认)");
                environment.getMessageSender().sendMessageAwait(context, environment.newText(builder.toString()), (m, c) -> {
                    boolean accept = false;
                    for (AbstractMessage message : c.getMessages()) {
                        if (!(message instanceof TextMessage))
                            continue;
                        if (((TextMessage) message).getText().equalsIgnoreCase("y")) {
                            accept = true;
                            break;
                        }
                    }
                    JsonObject obj = new JsonObject();
                    obj.addProperty("action", "update_confirm");
                    obj.addProperty("result", accept);
                    connection.sendPacket(new StringData(obj.toString()));
                    if (!accept)
                        environment.getMessageSender().sendMessage(context, environment.newText("已取消更新。"));
                });
            }
            case "error" -> environment.getMessageSender().sendMessage(context, environment.newText(
                    "无法更新。具体报错信息为: %s".formatted(JsonUtil.getStringOrNull(data, "message"))
            ));
            default -> environment.getMessageSender().sendMessage(context, environment.newText("<发生了错误>"));
        }
    }

    private void doUpdateFinishedCallback(JsonObject data) {
        Pair<DelegateEnvironment, MessageContext> pair = parseContext(JsonUtil.getStringOrNull(data, "context"));
        DelegateEnvironment environment = pair.first();
        MessageContext context = pair.second();
        environment.getMessageSender().sendMessage(context, environment.newText(
                "更新完成。目前bot版本为: %d".formatted(JsonUtil.getLongOrZero(data, "action_id"))
        ));
        ACTION_ID = JsonUtil.getLongOrZero(data, "action_id");
    }
}
