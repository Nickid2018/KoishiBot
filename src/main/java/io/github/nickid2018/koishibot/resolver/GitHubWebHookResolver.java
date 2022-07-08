package io.github.nickid2018.koishibot.resolver;

import io.github.nickid2018.koishibot.github.GitHubAuthenticator;
import io.github.nickid2018.koishibot.github.GitHubListener;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

public class GitHubWebHookResolver extends MessageResolver {

    public GitHubWebHookResolver() {
        super("~github webhook");
    }

    @Override
    public boolean groupTempChat() {
        return true;
    }

    @Override
    // ~github webhook add/del <repo>
    public boolean resolveInternal(String key, MessageContext context, Pattern pattern, Environment environment) {
        key = key.trim();
        String[] split = key.split(" ", 2);
        if (split.length != 2)
            return false;
        switch (split[0].toLowerCase(Locale.ROOT)) {
            case "add":
                addHook(split[1], context, environment);
                return true;
            case "del":
                deleteHook(split[1], context, environment);
                return true;
            default:
                return false;
        }
    }

    private static void addHook(String repo, MessageContext context, Environment environment) {
        GitHubAuthenticator.authenticateOperation(token -> {
            try {
                GitHubListener.LISTENER.getWebHookListener().addHook(repo, token);
                environment.getMessageSender().sendMessage(context,
                        environment.newText("已成功创建Web Hook，请不要在仓库的Webhooks手动删除bot的hook。"));
            } catch (IOException e) {
                environment.getMessageSender().onError(e, "github.webhook.create", context, false);
            }
        }, context, environment, "repo", "admin:repo_hook");
    }

    private static void deleteHook(String repo, MessageContext context, Environment environment) {
        GitHubAuthenticator.authenticateOperation(token -> {
            try {
                GitHubListener.LISTENER.getWebHookListener().deleteHook(repo, token);
                environment.getMessageSender().sendMessage(context,
                        environment.newText("已成功删除Web Hook。"));
            } catch (IOException e) {
                environment.getMessageSender().onError(e, "github.webhook.delete", context, false);
            }
        }, context, environment, "repo", "admin:repo_hook");
    }
}
