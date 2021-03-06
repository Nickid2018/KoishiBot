package io.github.nickid2018.koishibot.resolver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.github.GitHubAuthenticator;
import io.github.nickid2018.koishibot.github.GitHubListener;
import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.ConsumerE;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.WebUtil;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class GitHubRepoResolver extends MessageResolver {

    public static final String GITHUB_API = "https://api.github.com/";

    public GitHubRepoResolver() {
        super("~github repo");
    }

    // ~github repo <name> [issue <issue>]
    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        key = key.trim();
        String[] data = key.split(" ");
        AsyncUtil.execute(() -> {
            try {
                if (data.length == 1)
                    doRepoInfoGet(data[0], context, environment);
                if (data.length == 3) {
                    switch (data[1].toLowerCase(Locale.ROOT)) {
                        case "issue" -> doRepoIssueGet(data[0], data[2], context, environment);
                        default -> {
                        }
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

        builder.append("????????????: ").append(repo).append("\n");
        builder.append("?????????: ").append(JsonUtil.getStringInPathOrNull(object, "owner.login")).append("\n");
        builder.append("????????????: ").append(JsonUtil.getStringOrNull(object, "html_url")).append("\n");
        builder.append("SSH??????: ").append(JsonUtil.getStringOrNull(object, "ssh_url")).append("\n");
        builder.append("Watch: ").append(JsonUtil.getIntOrZero(object, "watchers")).append(" | ");
        builder.append("Fork: ").append(JsonUtil.getIntOrZero(object, "forks")).append("\n");

        JsonUtil.getString(object, "language").ifPresent(
                language -> builder.append("??????????????????: ").append(language).append("\n"));

        JsonElement element = object.get("license");
        if (element instanceof JsonObject)
            builder.append("???????????????: ").append(
                    JsonUtil.getStringOrNull(element.getAsJsonObject(), "name")).append("\n");

        JsonUtil.getString(object, "description").ifPresent(builder::append);

        environment.getMessageSender().sendMessageReply(context, environment.newText(builder.toString().trim()), false,
                (source, message) -> doRepoReply(message, repo, environment, context));
    }

    private void doRepoIssueGet(String repo, String issue, MessageContext context, Environment environment) throws IOException {
        HttpGet get = new HttpGet(GITHUB_API + "repos/" + repo + "/issues/" + issue);
        GitHubAuthenticator.acceptGitHubJSON(get);
        JsonObject object = WebUtil.fetchDataInJson(get).getAsJsonObject();

        StringBuilder builder = new StringBuilder();
        builder.append(repo).append(" #").append(issue).append("\n");
        builder.append(JsonUtil.getStringOrNull(object, "title")).append("\n");
        builder.append("?????????: ").append(JsonUtil.getStringInPathOrNull(object, "user.login")).append("\n");
        builder.append("??????: ").append(JsonUtil.getStringOrNull(object, "state")).append("\n");

        JsonArray labels = object.getAsJsonArray("labels");
        List<String> labelList = new ArrayList<>();
        for (JsonElement element : labels)
            labelList.add(JsonUtil.getStringOrNull(element.getAsJsonObject(), "name"));
        builder.append("??????: ").append(String.join(", ", labelList)).append("\n");

        String[] strs = Objects.requireNonNull(
                JsonUtil.getStringOrNull(object, "body")).split("\n");
        boolean skip = false;
        for (String str : strs)
            if (builder.length() < 800) {
                builder.append(str).append("\n");
                skip = true;
                break;
            }
        if (skip)
            builder.append("(??????????????????)");

        environment.getMessageSender().sendMessageReply(context, environment.newText(builder.toString().trim()), false,
                (source, message) -> doIssueReply(message, issue, repo, environment, context));
    }

    private String dealStr(ChainMessage message) {
        String command = null;
        for (AbstractMessage content : message.getMessages()) {
            if (content instanceof TextMessage) {
                command = ((TextMessage) content).getText();
                break;
            }
        }
        if (command == null)
            return null;
        command = command.trim();
        if (command.isEmpty())
            return null;
        return command;
    }

    private void doRepoReply(ChainMessage message, String repo, Environment environment, MessageContext context) {
        String command = dealStr(message);
        if (command == null)
            return;

        String[] split = command.split(" ", 2);
        AsyncUtil.execute(() -> {
            switch (split[0].toLowerCase(Locale.ROOT)) {
                case "~star" -> doAuthenticatedOperation(new HttpPut("https://api.github.com/user/starred/" + repo),
                        environment, context, WebUtil::sendReturnNoContent,
                        environment.newText("???star??????"), "github.repo.star", "repo");
                case "~unstar" -> doAuthenticatedOperation(new HttpDelete("https://api.github.com/user/starred/" + repo),
                        environment, context, WebUtil::sendReturnNoContent,
                        environment.newText("?????????star??????"), "github.repo.unstar", "repo");
                case "~fork" -> doAuthenticatedOperation(new HttpPost("https://api.github.com/user/repos/" + repo + "/forks"),
                        environment, context, WebUtil::sendReturnNoContent,
                        environment.newText("???fork??????"), "github.repo.fork", "repo");
            }
        });
    }

    private void doIssueReply(ChainMessage message, String repo, String issue, Environment environment, MessageContext context) {
        String command = dealStr(message);
        if (command == null)
            return;

        String[] split = command.split(" ", 2);
        AsyncUtil.execute(() -> {
            switch (split[0].toLowerCase(Locale.ROOT)) {
                case "~comment" -> {
                    if (split.length != 2)
                        return;
                    HttpPost post = new HttpPost("https://api.github.com/repos/" + repo + "/issues/" + issue + "/comments");
                    JsonObject data = new JsonObject();
                    data.addProperty("body", split[1]);
                    post.setEntity(new StringEntity(data.getAsString(), StandardCharsets.UTF_8));
                    doAuthenticatedOperation(post,
                            environment, context, request -> WebUtil.sendNeedCode(request, 201),
                            environment.newText("???????????????"), "github.issue.comment", "repo");
                }
            }
        });
    }

    private void doAuthenticatedOperation(HttpUriRequest request, Environment environment, MessageContext context,
                                          ConsumerE<HttpUriRequest> requestConsumer, AbstractMessage success, String module,
                                          String... scopes) {
        GitHubAuthenticator.authenticateOperation(token -> {
            GitHubAuthenticator.acceptGitHubJSON(request, token);
            try {
                requestConsumer.accept(request);
                environment.getMessageSender().sendMessage(context, success);
            } catch (Exception e) {
                environment.getMessageSender().onError(e, module, context, false);
            }
        }, context, environment, scopes);
    }
}
