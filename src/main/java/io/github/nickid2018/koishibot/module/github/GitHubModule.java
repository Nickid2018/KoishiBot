package io.github.nickid2018.koishibot.module.github;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.core.ErrorRecord;
import io.github.nickid2018.koishibot.module.Module;
import io.github.nickid2018.koishibot.util.DataReader;
import io.github.nickid2018.koishibot.util.web.ErrorCodeException;
import io.github.nickid2018.koishibot.util.GroupDataReader;
import io.github.nickid2018.koishibot.util.web.WebUtil;
import io.github.nickid2018.koishibot.util.value.MutableBoolean;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

public class GitHubModule extends Module {

    public static final Logger GITHUB_LOGGER = LoggerFactory.getLogger("GitHub");

    public static GitHubModule INSTANCE;

    public static final String GITHUB_API = "https://api.github.com";

    private GroupDataReader<Set<String>> groupData;
    private DataReader<Map<String, Repository>> pushData;
    private GitHubWebHookListener webHookListener;
    private GitHubAuthenticator authenticator;

    public GitHubModule() {
        super("github", List.of(
                new GitHubRepoResolver(),
                new GitHubWebHookResolver(),
                new GitHubSubscribeResolver()
        ), true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onStartInternal() throws IOException {
        groupData = new GroupDataReader<>("github",
                reader -> (Set<String>) new ObjectInputStream(reader).readObject(),
                (writer, data) -> new ObjectOutputStream(writer).writeObject(data),
                HashSet::new);
        groupData.loadAll();

        webHookListener = new GitHubWebHookListener(
                new File(groupData.getFolder(), "webhook.dat"), groupData);

        pushData = new DataReader<>(new File(groupData.getFolder(), "repo.dat"), HashMap::new);

        Thread thread = new Thread(this::queryRepoUpdate);
        thread.setDaemon(true);
        thread.start();

        INSTANCE = this;
    }

    public void onSettingReloadInternal(JsonObject settingsRoot) {
        if (authenticator == null)
            authenticator = new GitHubAuthenticator();
        authenticator.readSettings(settingsRoot);
    }

    @Override
    public void onTerminateInternal() {
    }

    @Override
    public String getDescription() {
        return "GitHub相关模块";
    }

    @Override
    public String getSummary() {
        return "提供对GitHub仓库的访问和订阅服务。";
    }

    public GitHubWebHookListener getWebHookListener() {
        return webHookListener;
    }

    public GitHubAuthenticator getAuthenticator() {
        return authenticator;
    }

    private void queryRepoUpdate() {
        while (true) {
            try {
                Thread.sleep(120_000);
            } catch (InterruptedException ignored) {
            }
            Set<String> keys = new HashSet<>(pushData.getDataSilently().keySet());
            MutableBoolean bool = new MutableBoolean(false);
            keys.forEach(key -> {
                try {
                    bool.or(refreshRepo(key));
                } catch (Exception e) {
                    ErrorRecord.enqueueError("github.refresh." + key, e);
                    try {
                        pushData.getData().remove(key);
                        pushData.saveData();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
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
        if (!pushData.getDataSilently().containsKey(repo) && !webHookListener.getHooks().contains(repo))
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

    public static JsonObject queryRepo(String key) throws IOException {
        String[] data = key.split("/");
        if (data.length != 2)
            throw new IOException("无效的仓库");
        try {
            return WebUtil.fetchDataInJson(INSTANCE.getAuthenticator().acceptGitHubJSON(
                    new HttpGet(GITHUB_API + "/repos/" + data[0] + "/" + data[1]))).getAsJsonObject();
        } catch (ErrorCodeException e) {
            if (e.code == 404)
                throw new IOException("仓库不存在或未公开");
            else throw e;
        }
    }
}
