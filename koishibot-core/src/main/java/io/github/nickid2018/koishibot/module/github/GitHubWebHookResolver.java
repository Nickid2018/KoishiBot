package io.github.nickid2018.koishibot.module.github;

import io.github.nickid2018.koishibot.message.DelegateEnvironment;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.permission.PermissionLevel;

import java.io.IOException;
import java.util.Locale;

@ResolverName("github-webhook")
@Syntax(syntax = "~github webhook add [仓库]", help = "添加仓库的WebHook")
@Syntax(syntax = "~github webhook del [仓库]", help = "删除仓库的WebHook")
public class GitHubWebHookResolver extends MessageResolver {

    public GitHubWebHookResolver() {
        super("~github webhook");
    }

    @Override
    public boolean groupEnabled() {
        return GitHubModule.INSTANCE.getAuthenticator().enableOAuth2();
    }

    @Override
    public boolean groupTempChat() {
        return true;
    }

    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.ADMIN;
    }

    @Override
    // ~github webhook add/del <repo>
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, DelegateEnvironment environment) {
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

    private static void addHook(String repo, MessageContext context, DelegateEnvironment environment) {
        GitHubModule.INSTANCE.getAuthenticator().authenticateOperation(token -> {
            try {
                GitHubModule.INSTANCE.getWebHookListener().addHook(repo, token);
                environment.getMessageSender().sendMessage(context,
                        environment.newText("已成功创建Web Hook，请不要在仓库的Webhooks手动删除bot的hook。"));
            } catch (IOException e) {
                environment.getMessageSender().onError(e, "github.webhook.create", context, false);
            }
        }, context, environment, "repo", "admin:repo_hook");
    }

    private static void deleteHook(String repo, MessageContext context, DelegateEnvironment environment) {
        GitHubModule.INSTANCE.getAuthenticator().authenticateOperation(token -> {
            try {
                GitHubModule.INSTANCE.getWebHookListener().deleteHook(repo, token);
                environment.getMessageSender().sendMessage(context,
                        environment.newText("已成功删除Web Hook。"));
            } catch (IOException e) {
                environment.getMessageSender().onError(e, "github.webhook.delete", context, false);
            }
        }, context, environment, "repo", "admin:repo_hook");
    }
}
