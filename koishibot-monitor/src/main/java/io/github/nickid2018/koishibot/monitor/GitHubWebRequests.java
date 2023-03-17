package io.github.nickid2018.koishibot.monitor;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.util.JsonUtil;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class GitHubWebRequests {

    public static long getNowActionID() throws IOException {
        String[] args = Settings.ACTION_REPO.split(";");
        if (args.length != 2)
            throw new IOException("Invalid action repo.");
        HttpGet get = new HttpGet("https://api.github.com/repos/%s/actions/workflows/%s/runs".formatted(args[0], args[1]));
        WebRequests.addGitHubHeaders(get);
        JsonObject json = WebRequests.fetchDataInJson(get).getAsJsonObject();
        int count = JsonUtil.getIntOrZero(json, "total_count");
        if (count == 0)
            throw new IOException("No action runs.");
        return JsonUtil.getLongInPathOrZero(json, "workflow_runs.0.id");
    }

    public static Object2LongMap<String> getArtifacts(long id) throws IOException {
        String[] args = Settings.ACTION_REPO.split(";");
        HttpGet get = new HttpGet("https://api.github.com/repos/%s/actions/runs/%d/artifacts".formatted(args[0], id));
        WebRequests.addGitHubHeaders(get);
        JsonObject json = WebRequests.fetchDataInJson(get).getAsJsonObject();
        int count = JsonUtil.getIntOrZero(json, "total_count");
        Object2LongMap<String> map = new Object2LongOpenHashMap<>();
        for (int i = 0; i < count; i++) {
            String name = JsonUtil.getStringInPathOrNull(json, "artifacts.%d.name".formatted(i));
            long fileID = JsonUtil.getLongInPathOrZero(json, "artifacts.%d.id".formatted(i));
            map.put(name, fileID);
        }
        return map;
    }

    public static File getArtifact(long id) throws IOException {
        String[] args = Settings.ACTION_REPO.split(";");
        HttpGet get = new HttpGet("https://api.github.com/repos/%s/actions/artifacts/%d/zip".formatted(args[0], id));
        WebRequests.addGitHubHeadersNoAccepts(get);
        byte[] data = WebRequests.fetchDataInBytes(get);
        File file = File.createTempFile("koishibot", ".zip");
        file.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            IOUtils.write(data, fos);
        }
        return file;
    }
}
