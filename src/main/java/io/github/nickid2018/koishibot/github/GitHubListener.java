package io.github.nickid2018.koishibot.github;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.core.ErrorRecord;
import io.github.nickid2018.koishibot.core.GroupDataReader;
import io.github.nickid2018.koishibot.core.Settings;
import io.github.nickid2018.koishibot.util.ErrorCodeException;
import io.github.nickid2018.koishibot.util.MutableBoolean;
import io.github.nickid2018.koishibot.util.WebUtil;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.*;
import java.util.*;

public class GitHubListener {

    public static final String GITHUB_API = "https://api.github.com";

    private final GroupDataReader<Map<String, String>> groupData;
    private final Map<String, Repository> pushData;
    private final File repo;

    @SuppressWarnings("unchecked")
    public GitHubListener() throws IOException, ClassNotFoundException {
        groupData = new GroupDataReader<>("github",
                reader -> (Map<String, String>) new ObjectInputStream(reader).readObject(),
                (writer, data) -> new ObjectOutputStream(writer).writeObject(data),
                HashMap::new);
        repo = new File(groupData.getFolder(), "repo.dat");
        if (repo.exists())
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(repo))) {
                pushData = (Map<String, Repository>) ois.readObject();
            }
        else {
            pushData = new HashMap<>();
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
        JsonObject object = queryRepo(key);
        String date = object.get("pushed_at").getAsString();
        if (!date.equals(pushData.get(key))) {
            groupData.getGroups().forEach(group -> {

            });
            return true;
        } else return false;
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
