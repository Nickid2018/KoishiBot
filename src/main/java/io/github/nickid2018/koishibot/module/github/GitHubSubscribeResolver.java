package io.github.nickid2018.koishibot.module.github;

import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.util.AsyncUtil;

import java.util.Locale;

@ResolverName("github-subscribe")
@Syntax(syntax = "~github subscribe add [仓库]", help = "订阅仓库的信息", rem = "需要事先添加WebHook")
@Syntax(syntax = "~github subscribe del [仓库]", help = "取消仓库的信息")
public class GitHubSubscribeResolver extends MessageResolver {

    public GitHubSubscribeResolver() {
        super("~github subscribe");
    }

    @Override
    // ~github subscribe add/del <repo>
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
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
                GitHubModule.INSTANCE.addRepo(context.group().getGroupId(), repo);
                environment.getMessageSender().sendMessage(context, environment.newText("已添加仓库"));
            } catch (Exception e) {
                environment.getMessageSender().onError(e, "github.add", context, false);
            }
        });
    }

    private void removeRepo(String repo, MessageContext context, Environment environment) {
        AsyncUtil.execute(() -> {
            try {
                GitHubModule.INSTANCE.removeRepo(context.group().getGroupId(), repo);
                environment.getMessageSender().sendMessage(context, environment.newText("已移除仓库"));
            } catch (Exception e) {
                environment.getMessageSender().onError(e, "github.remove", context, false);
            }
        });
    }
}
