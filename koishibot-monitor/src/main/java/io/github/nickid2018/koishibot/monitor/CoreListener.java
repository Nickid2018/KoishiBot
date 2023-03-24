package io.github.nickid2018.koishibot.monitor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.nickid2018.koishibot.network.*;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.LogUtils;
import io.github.nickid2018.koishibot.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2LongMap;

import java.io.File;
import java.io.IOException;
import java.util.Set;

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
        JsonObject object = JsonParser.parseString(((StringData) packet).getStr()).getAsJsonObject();
        String action = JsonUtil.getStringOrNull(object, "action");
        if (action == null)
            return;
        switch (action) {
            case "check_action_id" -> checkActionID(JsonUtil.getStringOrNull(object, "context"));
        }
    }

    @Override
    public void connectionClosed(Connection connection) {
        LogUtils.error(MonitorStart.LOGGER, "Core connection closed!", null);
    }

    private void checkActionID(String context) {
        try {
            long nowActionID = GitHubWebRequests.getNowActionID();
            if (nowActionID == EnvironmentCheck.NOW_ACTION_ID) {
                JsonObject object = new JsonObject();
                object.addProperty("action", "check_action_id");
                object.addProperty("result", "no_update");
                object.addProperty("context", context);
                send(object.toString());
            } else {
                JsonObject object = new JsonObject();
                object.addProperty("action", "check_action_id");
                object.addProperty("result", "update");
                object.addProperty("now_action_id", nowActionID);
                object.addProperty("context", context);
                send(object.toString());
            }
        } catch (IOException e) {
            try {
                MonitorStart.checkAndRun();
                JsonObject object = new JsonObject();
                object.addProperty("action", "check_action_id");
                object.addProperty("result", "error");
                object.addProperty("message", e.getMessage());
                object.addProperty("context", context);
                send(object.toString());
            } catch (IOException ignored) {
            }
        }
    }

    private void updateAndRestart() throws IOException {
        long nowActionID = GitHubWebRequests.getNowActionID();
        if (nowActionID == EnvironmentCheck.NOW_ACTION_ID)
            return;
        LogUtils.info(LogUtils.FontColor.CYAN, MonitorStart.LOGGER,
                "Action ID {} -> {}", EnvironmentCheck.NOW_ACTION_ID, nowActionID);

        Object2LongMap<String> artifacts = GitHubWebRequests.getArtifacts(nowActionID);
        File checksums = GitHubWebRequests.getArtifact(artifacts.getLong("checksums"));
        Pair<Set<String>, Set<String>> updateModules = StatusCheck.needUpdates(checksums);

        EnvironmentCheck.NOW_ACTION_ID = nowActionID;
    }
}
