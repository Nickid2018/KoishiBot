package io.github.nickid2018.koishibot.resolver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.KoishiBotMain;
import io.github.nickid2018.koishibot.github.GitHubListener;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.WebUtil;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class GitHubRepoResolver extends MessageResolver {

    public static final String GITHUB_API = "https://api.github.com/";

    public GitHubRepoResolver() {
        super("~github repo");
    }

    // ~github repo <name> [issue <issue>]
    @Override
    public boolean resolveInternal(String key, MessageContext context, Pattern pattern, Environment environment) {
        key = key.trim();
        String[] data = key.split(" ");
        KoishiBotMain.INSTANCE.executor.execute(() -> {
            try {
                if (data.length == 1)
                    doRepoInfoGet(data[0], context, environment);
                if (data.length == 3) {
                    switch (data[1].toLowerCase(Locale.ROOT)) {
                        case "issue":
                            doRepoIssueGet(data[0], data[2], context, environment);
                            break;
                        default:
                    }
                }
            } catch (Exception e) {
                environment.getMessageSender().onError(e, "github.repo", context, false);
            }
        });
        return true;
    }

    private void doRepoInfoGet(String repo, MessageContext context, Environment environment) throws IOException {
        JsonObject object = GitHubListener.queryRepo(repo);
        StringBuilder builder = new StringBuilder();

        builder.append("仓库名称: ").append(repo).append("\n");
        builder.append("拥有者: ").append(JsonUtil.getStringInPathOrNull(object, "owner.login")).append("\n");
        builder.append("仓库地址: ").append(JsonUtil.getStringInPathOrNull(object, "html_url")).append("\n");
        builder.append("SSH地址: ").append(JsonUtil.getStringInPathOrNull(object, "ssh_url")).append("\n");
        builder.append("Watch: ").append(object.get("watchers").getAsInt()).append(" | ");
        builder.append("Fork: ").append(object.get("forks").getAsInt()).append("\n");

        String language = JsonUtil.getStringInPathOrNull(object, "language");
        if (language != null)
            builder.append("主要编写语言: ").append(language).append("\n");

        JsonElement element = object.get("license");
        if (element instanceof JsonObject)
            builder.append("开源许可证: ").append(element.getAsJsonObject().get("name").getAsString()).append("\n");

        String desc = JsonUtil.getStringInPathOrNull(object, "description");
        if (desc != null)
            builder.append(desc);

        environment.getMessageSender().sendMessage(context, environment.newText(builder.toString().trim()));
    }

    private void doRepoIssueGet(String repo, String issue, MessageContext context, Environment environment) throws IOException {
        HttpGet get = new HttpGet(GITHUB_API + "repos/" + repo + "/issues/" + issue);
        GitHubListener.acceptJSON(get);
        JsonObject object = WebUtil.fetchDataInJson(get).getAsJsonObject();

        StringBuilder builder = new StringBuilder();
        builder.append(repo).append(" #").append(issue).append("\n");
        builder.append(JsonUtil.getStringInPathOrNull(object, "title")).append("\n");
        builder.append("创建者: ").append(JsonUtil.getStringInPathOrNull(object, "user.login")).append("\n");
        builder.append("状态: ").append(JsonUtil.getStringInPathOrNull(object, "state")).append("\n");

        JsonArray labels = object.getAsJsonArray("labels");
        List<String> labelList = new ArrayList<>();
        for (JsonElement element : labels)
            labelList.add(element.getAsJsonObject().get("name").getAsString());
        builder.append("标签: ").append(String.join(", ", labelList)).append("\n");

        String[] strs = Objects.requireNonNull(
                JsonUtil.getStringInPathOrNull(object, "body")).split("\n");
        boolean skip = false;
        for (String str : strs)
            if (builder.length() < 500) {
                builder.append(str).append("\n");
                skip = true;
                break;
            }
        if (skip)
            builder.append("(描述过长截断)");

        environment.getMessageSender().sendMessage(context, environment.newText(builder.toString().trim()));
    }
}
