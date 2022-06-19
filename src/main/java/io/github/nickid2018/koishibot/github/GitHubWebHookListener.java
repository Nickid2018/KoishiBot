package io.github.nickid2018.koishibot.github;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.nickid2018.koishibot.core.Settings;
import io.github.nickid2018.koishibot.util.WebUtil;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class GitHubWebHookListener {

    public static final List<String> WEBHOOK_PERMISSION = Arrays.asList(
            "discussion", "discussion_comment", "fork", "issue_comment", "issues",
            "pull_request", "pull_request_review_comment", "push", "release",
            "star", "status", "watch"
    );

    private final GitHubListener listener;
    private final HttpServer httpServer;
    private final Map<String, Integer> webHooks;

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

        httpServer = HttpServer.create(new InetSocketAddress(14514), 0);
        httpServer.setExecutor(Executors.newCachedThreadPool(
                new ThreadFactoryBuilder().setDaemon(true).build()));
        httpServer.createContext("/github", this::handle);
        httpServer.start();
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
        GitHubListener.acceptJSON(post, token);
        JsonObject json = WebUtil.fetchDataInJson(post).getAsJsonObject();
        int id = json.get("id").getAsInt();
        webHooks.put(repo, id);
        saveHooks();
    }

    public void deleteHook(String repo, String token) throws IOException {
        HttpDelete delete = new HttpDelete(
                GitHubListener.GITHUB_API + "/repos/" + repo + "/hooks/" + webHooks.get(repo));
        GitHubListener.acceptJSON(delete, token);
        WebUtil.sendReturnNoContent(delete);
        webHooks.remove(repo);
        saveHooks();
    }

    private void saveHooks() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(listener.webhook))) {
            oos.writeObject(webHooks);
        }
    }

    private void handle(HttpExchange httpExchange) throws IOException {
        httpExchange.sendResponseHeaders(204, 0);
        String data = IOUtils.toString(httpExchange.getRequestBody(), StandardCharsets.UTF_8);
        System.out.println(data);
//        JsonObject json = JsonParser.parseString(data).getAsJsonObject();
    }


}
