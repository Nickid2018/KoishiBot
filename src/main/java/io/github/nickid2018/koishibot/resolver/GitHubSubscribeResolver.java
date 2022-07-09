package io.github.nickid2018.koishibot.resolver;

import io.github.nickid2018.koishibot.github.GitHubListener;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.AsyncUtil;

import java.util.Locale;
import java.util.regex.Pattern;

public class GitHubSubscribeResolver extends MessageResolver {

    public GitHubSubscribeResolver() {
        super("~github subscribe");
    }

    @Override
    // ~github subscribe add/del <repo>
    public boolean resolveInternal(String key, MessageContext context, Pattern pattern, Environment environment) {
        key = key.trim();
        String[] split = key.split(" ", 2);
        if (split.length != 2)
            return false;
        switch (split[0].toLowerCase(Locale.ROOT)) {
            case "add":
                addRepo(split[1], context, environment);
                return true;
            case "del":
                removeRepo(split[1], context, environment);
                return true;
            default:
                return false;
        }
    }

    private void addRepo(String repo, MessageContext context, Environment environment) {
        AsyncUtil.execute(() -> {
            try {
                GitHubListener.LISTENER.addRepo(context.group().getGroupId(), repo);
                environment.getMessageSender().sendMessage(context, environment.newText("已填加仓库"));
            } catch (Exception e) {
                environment.getMessageSender().onError(e, "github.add", context, false);
            }
        });
    }

    private void removeRepo(String repo, MessageContext context, Environment environment) {
        AsyncUtil.execute(() -> {
            try {
                GitHubListener.LISTENER.removeRepo(context.group().getGroupId(), repo);
                environment.getMessageSender().sendMessage(context, environment.newText("已移除仓库"));
            } catch (Exception e) {
                environment.getMessageSender().onError(e, "github.remove", context, false);
            }
        });
    }
}
