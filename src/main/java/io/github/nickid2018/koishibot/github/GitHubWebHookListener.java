package io.github.nickid2018.koishibot.github;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.nickid2018.koishibot.util.DataReader;
import io.github.nickid2018.koishibot.core.ErrorRecord;
import io.github.nickid2018.koishibot.core.Settings;
import io.github.nickid2018.koishibot.message.Environments;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.ReflectTarget;
import io.github.nickid2018.koishibot.util.WebUtil;
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

    final GitHubListener listener;

    final DataReader<Map<String, Integer>> webHooks;

    public GitHubWebHookListener(GitHubListener listener) {
        this.listener = listener;
        webHooks = new DataReader<>(listener.webhook, HashMap::new);
    }

    public void addHook(String repo, String token) throws IOException {
        if (webHooks.getData().containsKey(repo))
            throw new IOException("????????????????????????Web Hook");
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
        HttpPost post = new HttpPost(GitHubListener.GITHUB_API + "/repos/" + repo + "/hooks");
        StringEntity entity = new StringEntity(data, StandardCharsets.UTF_8);
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        post.setEntity(entity);
        JsonObject json = WebUtil.fetchDataInJson(GitHubAuthenticator.acceptGitHubJSON(post, token)).getAsJsonObject();
        int id = JsonUtil.getIntOrZero(json, "id");
        webHooks.getData().put(repo, id);
        webHooks.saveData();
        GITHUB_WEBHOOK_LOGGER.info("Successfully added github webhook to {}.", repo);
    }

    public void deleteHook(String repo, String token) throws IOException {
        HttpDelete delete = new HttpDelete(
                GitHubListener.GITHUB_API + "/repos/" + repo + "/hooks/" + webHooks.getData().get(repo));
        WebUtil.sendReturnNoContent(GitHubAuthenticator.acceptGitHubJSON(delete, token));
        webHooks.getData().remove(repo);
        webHooks.saveData();
        GITHUB_WEBHOOK_LOGGER.info("Successfully deleted github webhook to {}.", repo);
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
                listener.groupData.getGroups().stream().filter(s -> listener.groupData.getData(s).contains(repo))
                        .forEach(group -> Environments.getEnvironments().stream().filter(e -> e.getGroup(group) != null).findFirst()
                                .ifPresent(environment -> {
                                    MessageContext context = new MessageContext(
                                            environment.getGroup(group), null, null, -1);
                                    environment.getMessageSender().sendMessage(context, environment.newText(send));
                                }));
            } catch (Exception e) {
                throw new IOException("Broadcast action but failed", e);
            }
        }
    }

    @ReflectTarget
    public String push(JsonObject object, String repo) {
        StringBuilder builder = new StringBuilder();
        builder.append(JsonUtil.getStringInPathOrNull(object, "pusher.name")).append(
                object.get("forced").getAsBoolean() ? "??????????????????????????????" : "????????????????????????").append("\n");
        builder.append("???????????????????????????:\n");
        List<JsonObject> commits = new ArrayList<>();
        object.get("commits").getAsJsonArray().forEach(e -> commits.add(e.getAsJsonObject()));
        if (commits.size() > 3) {
            builder.append("(???").append(commits.size()).append("?????????????????????????????????)\n");
            while (commits.size() > 3)
                commits.remove(0);
        }
        for (JsonObject commit : commits) {
            builder.append("??????: ");
            String message = JsonUtil.getStringOrNull(commit, "message").split("\n")[0];
            builder.append(message).append("\n");
            builder.append("  ?????????: ").append(JsonUtil.getStringInPathOrNull(commit, "committer.name")).append("\n");
            builder.append("  ??????: ").append(JsonUtil.getStringOrNull(commit, "timestamp")).append("\n");
            builder.append("  ??????").append(commit.get("added").getAsJsonArray().size())
                    .append("??????, ??????").append(commit.get("removed").getAsJsonArray().size())
                    .append("??????, ??????").append(commit.get("modified").getAsJsonArray().size()).append("?????????\n");
            try {
                JsonObject commitData = WebUtil.fetchDataInJson(GitHubAuthenticator.acceptGitHubJSON(
                        new HttpGet(GitHubListener.GITHUB_API + "/repos/" + repo + "/commits/"
                                + JsonUtil.getStringOrNull(commit, "id")))).getAsJsonObject();
                builder.append("  [??????").append(JsonUtil.getIntInPathOrZero(commitData, "stats.additions"))
                        .append("????????????").append(JsonUtil.getIntInPathOrZero(commitData, "stats.deletions")).append("???]\n");
            } catch (Exception e) {
                ErrorRecord.enqueueError("github.webhook.push", e);
            }
        }
        return builder.toString().trim();
    }

    @ReflectTarget
    public String fork(JsonObject object, String repo) {
        return JsonUtil.getStringInPathOrNull(object, "sender.login") + "?????????Fork???\n" +
                "Fork??????: " + JsonUtil.getStringInPathOrNull(object, "forkee.full_name");
    }

    @ReflectTarget
    public String star(JsonObject object, String repo) {
        return JsonUtil.getStringInPathOrNull(object, "sender.login") +
                (Objects.equals(JsonUtil.getStringOrNull(object, "action"), "created")
                        ? " star????????????" : " ??????star????????????");
    }

    @ReflectTarget
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
                builder.append(user).append("?????????????????????\n");
                builder.append("???????????????: ").append(tag);
                if (pre)
                    builder.append("(????????????)");
                if (draft)
                    builder.append("(??????)");
                builder.append("\n");
                if (name != null)
                    builder.append("???????????????: ").append(name).append("\n");
                if (body != null)
                    builder.append(body);
                break;
            case "publish":
                builder.append(user).append("?????????????????????\n");
                builder.append("???????????????: ").append(tag);
                if (pre)
                    builder.append("(????????????)");
                builder.append("\n");
                if (name != null)
                    builder.append("???????????????: ").append(name).append("\n");
                if (body != null)
                    builder.append(body);
                break;
            case "unpublished":
                builder.append(user).append("???????????????????????????\n");
                builder.append("???????????????: ").append(tag);
                break;
            default:
                return null;
        }
        return builder.toString();
    }
}
