package io.github.nickid2018.koishibot.github;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.core.ErrorRecord;
import io.github.nickid2018.koishibot.core.GroupDataReader;
import io.github.nickid2018.koishibot.core.Settings;
import io.github.nickid2018.koishibot.util.ErrorCodeException;
import io.github.nickid2018.koishibot.util.MutableBoolean;
import io.github.nickid2018.koishibot.util.WebUtil;
import io.github.nickid2018.koishibot.webhook.WebHookManager;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GitHubListener {

    public static void clinit() {
    }

    public static final GitHubListener LISTENER;

    static {
        GitHubListener tmp;
        try {
            tmp = new GitHubListener();
        } catch (Exception e) {
            e.printStackTrace();
            tmp = null;
        }
        LISTENER = tmp;
    }

    public static final String GITHUB_API = "https://api.github.com";

    final GroupDataReader<Set<String>> groupData;
    final Map<String, Repository> pushData;
    final File repo;
    final File webhook;
    final GitHubWebHookListener webHookListener;

    @SuppressWarnings("unchecked")
    public GitHubListener() throws Exception {
        groupData = new GroupDataReader<>("github",
                reader -> (Set<String>) new ObjectInputStream(reader).readObject(),
                (writer, data) -> new ObjectOutputStream(writer).writeObject(data),
                HashSet::new);
        groupData.loadAll();
        repo = new File(groupData.getFolder(), "repo.dat");
        webhook = new File(groupData.getFolder(), "webhook.dat");

        WebHookManager.addHandle("/github", webHookListener = new GitHubWebHookListener(this));

        if (repo.exists())
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(repo))) {
                pushData = (Map<String, Repository>) ois.readObject();
            }
        else {
            pushData = new HashMap<>();
            repo.createNewFile();
            savePushes();
        }

        Thread thread = new Thread(this::queryRepoUpdate);
        thread.setDaemon(true);
        thread.start();
    }

    private void savePushes() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(repo))) {
            oos.writeObject(pushData);
        }
    }

    public GitHubWebHookListener getWebHookListener() {
        return webHookListener;
    }

    private void queryRepoUpdate() {
        while (true) {
            try {
                Thread.sleep(120_000);
            } catch (InterruptedException ignored) {
            }
            Set<String> keys = new HashSet<>(pushData.keySet());
            MutableBoolean bool = new MutableBoolean(false);
            keys.forEach(key -> {
                try {
                    bool.or(refreshRepo(key));
                } catch (Exception e) {
                    ErrorRecord.enqueueError("github.refresh." + key, e);
                    pushData.remove(key);
                }
            });
            if (bool.getValue()) {
                try {
                    savePushes();
                } catch (IOException e) {
                    ErrorRecord.enqueueError("github.refresh.save", e);
                }
            }
        }
    }

    public boolean refreshRepo(String key) throws Exception {
//        JsonObject object = queryRepo(key);
//        String date = object.get("pushed_at").getAsString();
//        if (!date.equals(pushData.get(key))) {
//            groupData.getGroups().forEach(group -> {
//
//            });
//            return true;
//        } else return false;
        return false;
    }

    public void addRepo(String group, String repo) throws Exception {
        if (!pushData.containsKey(repo) && !webHookListener.webHooks.containsKey(repo))
            throw new IOException("仓库源不存在，请使用github repo进行添加");
        groupData.updateData(group, set -> {
            set.add(repo);
            return set;
        });
    }

    public void removeRepo(String group, String repo) throws Exception {
        groupData.updateData(group, set -> {
            set.remove(repo);
            return set;
        });
    }

    public static HttpUriRequest acceptJSON(HttpUriRequest request, String token) {
        request.addHeader("Authorization", "token " + token);
        request.addHeader("Accept", "application/vnd.github.v3+json");
        return request;
    }

    public static HttpUriRequest authenticate(HttpUriRequest request) {
        if (Settings.GITHUB_TOKEN != null && !Settings.GITHUB_TOKEN.isEmpty())
            request.addHeader("Authorization", "token " + Settings.GITHUB_TOKEN);
        return request;
    }

    public static HttpUriRequest acceptJSON(HttpUriRequest request) {
        request.addHeader("Accept", "application/vnd.github.v3+json");
        return authenticate(request);
    }

    public static JsonObject queryRepo(String key) throws IOException {
        String[] data = key.split("/");
        if (data.length != 2)
            throw new IOException("无效的仓库");
        try {
            return WebUtil.fetchDataInJson(
                    acceptJSON(new HttpGet(GITHUB_API + "/repos/" + data[0] + "/" + data[1]))).getAsJsonObject();
        } catch (ErrorCodeException e) {
            if (e.code == 404)
                throw new IOException("仓库不存在或未公开");
            else throw e;
        }
    }
}
