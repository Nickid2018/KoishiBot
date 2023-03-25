package io.github.nickid2018.koishibot.monitor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.github.nickid2018.koishibot.network.*;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.LogUtils;
import io.github.nickid2018.koishibot.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2LongMap;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class CoreListener implements NetworkListener {

    public static final DataRegistry REGISTRY = new DataRegistry();
    public static final CoreListener INSTANCE = new CoreListener();

    private Connection connection;
    private CompletableFuture<Boolean> needUpdate = new CompletableFuture<>();

    public void send(String data) {
        connection.sendPacket(new StringData(data));
    }

    @Override
    public void connectionOpened(Connection connection) {
        this.connection = connection;
        LogUtils.info(LogUtils.FontColor.CYAN, MonitorStart.LOGGER, "Core connection opened!");
        JsonObject object = new JsonObject();
        object.addProperty("action", "start");
        object.addProperty("action_id", EnvironmentCheck.NOW_ACTION_ID);
        send(object.toString());
    }

    @Override
    public void receivePacket(Connection connection, SerializableData packet) {
        JsonObject object = JsonParser.parseString(((StringData) packet).getStr()).getAsJsonObject();
        String action = JsonUtil.getStringOrNull(object, "action");
        if (action == null)
            return;
        switch (action) {
            case "check_action_id" -> checkActionID(JsonUtil.getStringOrNull(object, "context"));
            case "update" -> updateAndRestart(JsonUtil.getStringOrNull(object, "context"));
            case "update_confirm" -> needUpdate.complete(JsonUtil.getData(object, "result", JsonPrimitive.class)
                    .map(JsonPrimitive::getAsBoolean).orElse(false));
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

    private void updateAndRestart(String context) {
        try {
            long nowActionID = GitHubWebRequests.getNowActionID();
            if (nowActionID == EnvironmentCheck.NOW_ACTION_ID)
                return;
            LogUtils.info(LogUtils.FontColor.CYAN, MonitorStart.LOGGER,
                    "Action ID {} -> {}", EnvironmentCheck.NOW_ACTION_ID, nowActionID);

            Object2LongMap<String> artifacts = GitHubWebRequests.getArtifacts(nowActionID);
            File checksums = GitHubWebRequests.getArtifact(artifacts.getLong("checksums"));
            Pair<Set<String>, Set<String>> updateModules = StatusCheck.needUpdates(checksums);

            if (updateModules.first().isEmpty() && updateModules.second().isEmpty()) {
                JsonObject object = new JsonObject();
                object.addProperty("action", "update");
                object.addProperty("result", "no_update");
                object.addProperty("action_id", nowActionID);
                object.addProperty("context", context);
                send(object.toString());
                EnvironmentCheck.NOW_ACTION_ID = nowActionID;
                return;
            } else {
                JsonObject object = new JsonObject();
                object.addProperty("action", "update");
                object.addProperty("result", "can_update");
                object.addProperty("action_id", nowActionID);
                object.addProperty("context", context);
                JsonArray updateCores = new JsonArray();
                updateModules.first().forEach(updateCores::add);
                object.add("update_cores", updateCores);
                JsonArray updateLibsArray = new JsonArray();
                updateModules.second().forEach(updateLibsArray::add);
                object.add("update_libs", updateLibsArray);
                send(object.toString());
            }

            boolean needUpdateNow = needUpdate.get();
            needUpdate = new CompletableFuture<>();
            if (!needUpdateNow)
                return;

            Thread.sleep(10_000);
            LogUtils.info(LogUtils.FontColor.CYAN, MonitorStart.LOGGER, "Shutting modules...");
            Set<String> needClose = new HashSet<>();
            needClose.addAll(updateModules.first());
            needClose.addAll(updateModules.second());
            JsonObject object = new JsonObject();
            object.addProperty("action", "stops");
            JsonArray stops = new JsonArray();
            needClose.forEach(stops::add);
            object.add("stops", stops);
            send(object.toString());

            Thread.sleep(10_000);
            LogUtils.info(LogUtils.FontColor.CYAN, MonitorStart.LOGGER, "Updating modules...");
            EnvironmentCheck.updateNotFully(artifacts, updateModules.first(), updateModules.second());
            StatusCheck.updateChecksums(checksums, needClose);
            MonitorStart.checkAndRun();
            LogUtils.info(LogUtils.FontColor.CYAN, MonitorStart.LOGGER, "Modules updated!");
            Thread.sleep(10_000);

            object = new JsonObject();
            object.addProperty("action", "update_finish");
            object.addProperty("action_id", nowActionID);
            object.addProperty("context", context);
            send(object.toString());

            EnvironmentCheck.NOW_ACTION_ID = nowActionID;
        } catch (Exception e) {
            try {
                MonitorStart.checkAndRun();
                JsonObject object = new JsonObject();
                object.addProperty("action", "update");
                object.addProperty("result", "error");
                object.addProperty("message", e.getMessage());
                object.addProperty("context", context);
                send(object.toString());
            } catch (IOException ignored) {
            }
        }
    }
}
