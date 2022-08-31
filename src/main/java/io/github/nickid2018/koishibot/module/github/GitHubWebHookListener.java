package io.github.nickid2018.koishibot.module.github;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.nickid2018.koishibot.module.ModuleManager;
import io.github.nickid2018.koishibot.server.ServerManager;
import io.github.nickid2018.koishibot.util.*;
import io.github.nickid2018.koishibot.core.ErrorRecord;
import io.github.nickid2018.koishibot.core.Settings;
import io.github.nickid2018.koishibot.message.Environments;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.web.WebUtil;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GitHubWebHookListener implements HttpHandler {

    public static final Logger GITHUB_WEBHOOK_LOGGER = LoggerFactory.getLogger("GitHub WebHook");

    public static final List<String> WEBHOOK_PERMISSION = Arrays.asList(
            "fork", "issue_comment", "issues", "pull_request",
            "pull_request_review_comment", "push", "release",
            "star", "status"
    );

    public static final Map<String, Method> REFLECT_HANDLE = new HashMap<>();

    static {
        WEBHOOK_PERMISSION.forEach(action -> {
            try {
                REFLECT_HANDLE.put(action, GitHubWebHookListener.class.getDeclaredMethod(
                        action, JsonObject.class, String.class));
            } catch (NoSuchMethodException ignored) {
            }
        });
    }

    private final GroupDataReader<Set<String>> groupData;

    private final DataReader<Map<String, Integer>> webHooks;

    protected GitHubWebHookListener(File webhookData, GroupDataReader<Set<String>> groupData) throws IOException {
        this.groupData = groupData;
        webHooks = new DataReader<>(webhookData, HashMap::new);
        ServerManager.addHandle("/github", this);
    }

    public void addHook(String repo, String token) throws IOException {
        if (webHooks.getData().containsKey(repo))
            throw new IOException("已经添加过此库的Web Hook");
        JsonObject object = new JsonObject();
        object.addProperty("name", "web");
        object.addProperty("active", true);
        JsonArray array = new JsonArray();
        WEBHOOK_PERMISSION.forEach(array::add);
        object.add("events", array);
        JsonObject config = new JsonObject();
        config.addProperty("content_type", "json");
        config.addProperty("insecure_ssl", "0");
        config.addProperty("url", "http://" + Settings.LOCAL_IP + ":14514/github");
        object.add("config", config);
        String data = object.toString();
        HttpPost post = new HttpPost(GitHubModule.GITHUB_API + "/repos/" + repo + "/hooks");
        StringEntity entity = new StringEntity(data, StandardCharsets.UTF_8);
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        post.setEntity(entity);
        JsonObject json = WebUtil.fetchDataInJson(GitHubModule.INSTANCE.getAuthenticator().acceptGitHubJSON(post, token)).getAsJsonObject();
        int id = JsonUtil.getIntOrZero(json, "id");
        webHooks.getData().put(repo, id);
        webHooks.saveData();
        GITHUB_WEBHOOK_LOGGER.info("Successfully added github webhook to {}.", repo);
    }

    public void deleteHook(String repo, String token) throws IOException {
        HttpDelete delete = new HttpDelete(
                GitHubModule.GITHUB_API + "/repos/" + repo + "/hooks/" + webHooks.getData().get(repo));
        WebUtil.sendReturnNoContent(GitHubModule.INSTANCE.getAuthenticator().acceptGitHubJSON(delete, token));
        webHooks.getData().remove(repo);
        webHooks.saveData();
        GITHUB_WEBHOOK_LOGGER.info("Successfully deleted github webhook to {}.", repo);
    }

    public Set<String> getHooks() {
        return webHooks.getDataSilently().keySet();
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String data = IOUtils.toString(httpExchange.getRequestBody(), StandardCharsets.UTF_8);
        String function = httpExchange.getRequestHeaders().getFirst("X-GitHub-Event");
        httpExchange.sendResponseHeaders(204, -1);
        GITHUB_WEBHOOK_LOGGER.debug(data);
        JsonObject json = JsonParser.parseString(data).getAsJsonObject();
        String repo = JsonUtil.getStringInPathOrNull(json, "repository.full_name");
        Method method = REFLECT_HANDLE.get(function);
        if (method != null) {
            try {
                Object inv = method.invoke(this, json, repo);
                if (inv == null)
                    return;
                String send = "[WebHook]" + repo + ":\n" + ((String) inv).trim();
                groupData.getGroups().stream()
                        .filter(s -> ModuleManager.isOpened(s, "github"))
                        .filter(s -> groupData.getData(s).contains(repo))
                        .forEach(group -> Environments.getEnvironments().stream()
                                .filter(e -> e.getGroup(group) != null).findFirst()
                                .ifPresent(environment -> {
                                    MessageContext context = new MessageContext(
                                            environment.getGroup(group), null, null, -1);
                                    environment.getMessageSender().sendMessage(context, environment.newText(send));
                                }));
            } catch (Exception e) {
                GITHUB_WEBHOOK_LOGGER.error("Broadcast action but failed", e);
            }
        }
    }

    public String push(JsonObject object, String repo) {
        StringBuilder builder = new StringBuilder();
        builder.append(JsonUtil.getStringInPathOrNull(object, "pusher.name")).append(
                object.get("forced").getAsBoolean() ? "进行了强制推送操作。" : "进行了推送操作。").append("\n");
        builder.append("本次推送的提交信息:\n");
        List<JsonObject> commits = new ArrayList<>();
        object.get("commits").getAsJsonArray().forEach(e -> commits.add(e.getAsJsonObject()));
        if (commits.size() > 3) {
            builder.append("(共").append(commits.size()).append("次提交，仅显示最后三次)\n");
            while (commits.size() > 3)
                commits.remove(0);
        }
        for (JsonObject commit : commits) {
            builder.append("提交: ");
            String message = JsonUtil.getStringOrNull(commit, "message").split("\n")[0];
            builder.append(message).append("\n");
            builder.append("  提交人: ").append(JsonUtil.getStringInPathOrNull(commit, "committer.name")).append("\n");
            builder.append("  时间: ").append(JsonUtil.getStringOrNull(commit, "timestamp")).append("\n");
            builder.append("  添加").append(commit.get("added").getAsJsonArray().size())
                    .append("文件, 删除").append(commit.get("removed").getAsJsonArray().size())
                    .append("文件, 修改").append(commit.get("modified").getAsJsonArray().size()).append("文件。\n");
            try {
                JsonObject commitData = WebUtil.fetchDataInJson(GitHubModule.INSTANCE.getAuthenticator().acceptGitHubJSON(
                        new HttpGet(GitHubModule.GITHUB_API + "/repos/" + repo + "/commits/"
                                + JsonUtil.getStringOrNull(commit, "id")))).getAsJsonObject();
                builder.append("  [增加").append(JsonUtil.getIntInPathOrZero(commitData, "stats.additions"))
                        .append("行，删除").append(JsonUtil.getIntInPathOrZero(commitData, "stats.deletions")).append("行]\n");
            } catch (Exception e) {
                ErrorRecord.enqueueError("github.webhook.push", e);
            }
        }
        return builder.toString().trim();
    }

    public String fork(JsonObject object, String repo) {
        return JsonUtil.getStringInPathOrNull(object, "sender.login") + "进行了Fork。\n" +
                "Fork仓库: " + JsonUtil.getStringInPathOrNull(object, "forkee.full_name");
    }

    public String star(JsonObject object, String repo) {
        return JsonUtil.getStringInPathOrNull(object, "sender.login") +
                (Objects.equals(JsonUtil.getStringOrNull(object, "action"), "created")
                        ? " star了此仓库" : " 取消star了此仓库");
    }

    public String release(JsonObject object, String repo) {
        StringBuilder builder = new StringBuilder();
        String action = JsonUtil.getStringOrNull(object, "action");
        String name = JsonUtil.getStringInPathOrNull(object, "release.name");
        String tag = JsonUtil.getStringInPathOrNull(object, "release.tag_name");
        String body = JsonUtil.getStringInPathOrNull(object, "release.body");
        boolean pre = JsonUtil.getDataInPath(
                object, "release.prerelease", JsonPrimitive.class).map(JsonPrimitive::getAsBoolean).orElse(false);
        boolean draft = JsonUtil.getDataInPath(
                object, "release.draft", JsonPrimitive.class).map(JsonPrimitive::getAsBoolean).orElse(false);
        String user = JsonUtil.getStringInPathOrNull(object, "sender.login");
        switch (Objects.requireNonNull(action)) {
            case "created":
                builder.append(user).append("创建了发行包。\n");
                builder.append("发行包标签: ").append(tag);
                if (pre)
                    builder.append("(预发布版)");
                if (draft)
                    builder.append("(草稿)");
                builder.append("\n");
                if (name != null)
                    builder.append("发行包名称: ").append(name).append("\n");
                if (body != null)
                    builder.append(body);
                break;
            case "publish":
                builder.append(user).append("发布了发行包。\n");
                builder.append("发行包标签: ").append(tag);
                if (pre)
                    builder.append("(预发布版)");
                builder.append("\n");
                if (name != null)
                    builder.append("发行包名称: ").append(name).append("\n");
                if (body != null)
                    builder.append(body);
                break;
            case "unpublished":
                builder.append(user).append("取消发布了发行包。\n");
                builder.append("发行包标签: ").append(tag);
                break;
            default:
                return null;
        }
        return builder.toString();
    }
}
