package io.github.nickid2018.koishibot.github;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.nickid2018.koishibot.core.Settings;
import io.github.nickid2018.koishibot.message.Environments;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.WebUtil;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GitHubWebHookListener implements HttpHandler {

    public static final List<String> WEBHOOK_PERMISSION = Arrays.asList(
            "fork", "issue_comment", "issues", "pull_request",
            "pull_request_review_comment", "push", "release",
            "star", "status", "watch"
    );

    final GitHubListener listener;
    final Map<String, Integer> webHooks;

    @SuppressWarnings("unchecked")
    public GitHubWebHookListener(GitHubListener listener) throws IOException, ClassNotFoundException {
        this.listener = listener;
        if (listener.webhook.exists())
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(listener.webhook))) {
                webHooks = (Map<String, Integer>) ois.readObject();
            }
        else {
            webHooks = new HashMap<>();
            listener.webhook.createNewFile();
            saveHooks();
        }
    }

    public void addHook(String repo, String token) throws IOException {
        if (webHooks.containsKey(repo))
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
        HttpPost post = new HttpPost(GitHubListener.GITHUB_API + "/repos/" + repo + "/hooks");
        StringEntity entity = new StringEntity(data, StandardCharsets.UTF_8);
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        post.setEntity(entity);
        JsonObject json = WebUtil.fetchDataInJson(GitHubListener.acceptJSON(post, token)).getAsJsonObject();
        int id = json.get("id").getAsInt();
        webHooks.put(repo, id);
        saveHooks();
    }

    public void deleteHook(String repo, String token) throws IOException {
        HttpDelete delete = new HttpDelete(
                GitHubListener.GITHUB_API + "/repos/" + repo + "/hooks/" + webHooks.get(repo));
        WebUtil.sendReturnNoContent(GitHubListener.acceptJSON(delete, token));
        webHooks.remove(repo);
        saveHooks();
    }

    private void saveHooks() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(listener.webhook))) {
            oos.writeObject(webHooks);
        }
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String data = IOUtils.toString(httpExchange.getRequestBody(), StandardCharsets.UTF_8);
        String function = (String) httpExchange.getAttribute("X-GitHub-Event");
        httpExchange.sendResponseHeaders(204, 0);
        System.out.println(data);
        JsonObject json = JsonParser.parseString(data).getAsJsonObject();
        String repo = WebUtil.getDataInPathOrNull(json, "repository.full_name");
        String send = null;
        switch (function) {
            case "push":
                send = push(json, repo);
                break;
            case "fork":
                send = fork(json, repo);
                break;
        }
        if (send != null) {
            String finalSend = "[WebHook]" + send;
            listener.groupData.getGroups().stream().filter(s -> listener.groupData.getData(s).contains(repo))
                    .forEach(group -> {
                        Optional<Environment> environmentOp =
                                Environments.getEnvironments().stream().filter(e -> e.getGroup(group) != null).findFirst();
                        if (!environmentOp.isPresent())
                            return;
                        Environment environment = environmentOp.get();
                        MessageContext context = new MessageContext(environment.getGroup(group), null, null, -1);
                        environment.getMessageSender().sendMessage(context, environment.newText(finalSend));
                    });
        }
    }

    private String push(JsonObject object, String repo) {
        StringBuilder builder = new StringBuilder();
        builder.append(repo).append(":\n");
        builder.append(WebUtil.getDataInPathOrNull(object, "pusher.name")).append(
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
            String message = commit.get("message").getAsString().split("\n")[0];
            builder.append(message).append("\n");
            builder.append("  提交人: ").append(WebUtil.getDataInPathOrNull(commit, "committer.name")).append("\n");
            builder.append("  时间: ").append(commit.get("timestamp").getAsString()).append("\n");
            builder.append("  添加").append(commit.get("added").getAsJsonArray().size())
                    .append("文件, 删除").append(commit.get("removed").getAsJsonArray().size())
                    .append("文件, 修改").append(commit.get("modified").getAsJsonArray().size()).append("文件。\n");
        }
        return builder.toString().trim();
    }

    private String fork(JsonObject object, String repo) {
        StringBuilder builder = new StringBuilder();
        builder.append(repo).append(":\n");
        builder.append(WebUtil.getDataInPathOrNull(object, "sender.login")).append("进行了Fork。\n");
        builder.append("Fork仓库: ").append(WebUtil.getDataInPathOrNull(object, "forkee.full_name"));
        return builder.toString();
    }
}