package io.github.nickid2018.koishibot.resolver;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.KoishiBotMain;
import io.github.nickid2018.koishibot.github.GitHubListener;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.WebUtil;

import java.io.IOException;
import java.util.regex.Pattern;

public class GitHubRepoResolver extends MessageResolver {

    public static final String GITHUB_API = "https://api.github.com";

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
        builder.append("拥有者: ").append(WebUtil.getDataInPathOrNull(object, "owner.login")).append("\n");
        builder.append("仓库地址: ").append(WebUtil.getDataInPathOrNull(object, "html_url")).append("\n");
        builder.append("Watch: ").append(object.get("watchers").getAsInt()).append("\n");
        builder.append("Fork: ").append(object.get("forks").getAsInt()).append("\n");
        builder.append("语言: ").append(WebUtil.getDataInPathOrNull(object, "language")).append("\n");

        JsonElement element = object.get("license");
        if (element instanceof JsonObject)
            builder.append("开源许可证: ").append(element.getAsJsonObject().get("name").getAsString()).append("\n");

        builder.append(WebUtil.getDataInPathOrNull(object, "description"));

        environment.getMessageSender().sendMessage(context, environment.newText(builder.toString()));
    }
}
