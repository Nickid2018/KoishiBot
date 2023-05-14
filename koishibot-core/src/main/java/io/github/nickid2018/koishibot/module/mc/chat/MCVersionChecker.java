package io.github.nickid2018.koishibot.module.mc.chat;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.core.ErrorRecord;
import io.github.nickid2018.koishibot.message.Environments;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.module.mc.MCModule;
import io.github.nickid2018.koishibot.util.DataReader;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.web.WebUtil;
import org.apache.hc.client5.http.classic.methods.HttpGet;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MCVersionChecker {

    private final MCModule module;
    private final DataReader<String> versionReader;
    private Timer timer;

    public MCVersionChecker(MCModule module) {
        this.module = module;
        versionReader = new DataReader<>(new File("data/mcversion.dat"), () -> "");
    }

    public void start() {
        timer = new Timer("MCVersionChecker", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    checkVersion();
                } catch (Throwable e) {
                    ErrorRecord.enqueueError("mc.version.checker", e);
                }
            }
        }, 0, 1000 * 60 * 10);
    }

    public void stop() {
        timer.cancel();
        timer = null;
    }

    private void checkVersion() throws IOException {
        JsonObject versionData = WebUtil.fetchDataInJson(
                new HttpGet("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")).getAsJsonObject();
        String latestSnapshot = JsonUtil.getStringInPathOrNull(versionData, "latest.snapshot");
        if (versionReader.getDataSilently().equals(latestSnapshot))
            return;
        StringBuilder builder = new StringBuilder();
        builder.append("Minecraft 版本已更新：%s\n".formatted(latestSnapshot));
        String url = JsonUtil.getDataInPath(versionData, "versions.0", JsonObject.class).map(obj -> {
            String entry = JsonUtil.getStringOrNull(obj, "id");
            if (!latestSnapshot.equals(entry))
                return null;
            builder.append("发布时间：%s\n".formatted(JsonUtil.getStringOrNull(obj, "releaseTime")));
            return JsonUtil.getStringOrNull(obj, "url");
        }).orElse(null);
        if (url != null) {
            JsonObject versionInfo = WebUtil.fetchDataInJson(new HttpGet(url)).getAsJsonObject();
            builder.append("客户端：%s（%fMiB）\n".formatted(
                    JsonUtil.getStringInPathOrNull(versionInfo, "downloads.client.sha1"),
                    JsonUtil.getIntInPathOrZero(versionInfo, "downloads.client.size") / 1024.0 / 1024.0));
            builder.append("服务端：%s（%fMiB）\n".formatted(
                    JsonUtil.getStringInPathOrNull(versionInfo, "downloads.server.sha1"),
                    JsonUtil.getIntInPathOrZero(versionInfo, "downloads.server.size") / 1024.0 / 1024.0));
            builder.append("客户端混淆映射表：%s（%fMiB）\n".formatted(
                    JsonUtil.getStringInPathOrNull(versionInfo, "downloads.client_mappings.sha1"),
                    JsonUtil.getIntInPathOrZero(versionInfo, "downloads.client_mappings.size") / 1024.0 / 1024.0));
            builder.append("服务端混淆映射表：%s（%fMiB）\n".formatted(
                    JsonUtil.getStringInPathOrNull(versionInfo, "downloads.server_mappings.sha1"),
                    JsonUtil.getIntInPathOrZero(versionInfo, "downloads.server_mappings.size") / 1024.0 / 1024.0));
        }
        String send = builder.toString();
        module.subscribedGroups.getGroups().stream()
                .filter(group -> module.subscribedGroups.getData(group).contains("version"))
                .forEach(group -> Environments.getEnvironments().stream()
                        .filter(e -> e.getGroup(group) != null)
                        .findFirst()
                        .ifPresent(environment -> {
                            MessageContext context = new MessageContext(environment,
                                    environment.getGroup(group), null, environment.newChain(), -1);
                            environment.getMessageSender().sendMessage(context, environment.newText(send));
                        }));
        versionReader.setData(latestSnapshot);
    }
}
