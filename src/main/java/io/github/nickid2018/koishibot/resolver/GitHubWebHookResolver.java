package io.github.nickid2018.koishibot.resolver;

import io.github.nickid2018.koishibot.github.GitHubListener;
import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.message.api.TextMessage;
import io.github.nickid2018.koishibot.util.AsyncUtil;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GitHubWebHookResolver extends MessageResolver {

    public GitHubWebHookResolver() {
        super("~github webhook");
    }

    @Override
    public boolean groupEnabled() {
        return false;
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
        AbstractMessage message = environment.newText(
                "警告: 你正在准备为一个GitHub仓库添加Web Hook，请确定你对该仓库具有完全的控制权。\n" +
                "如果要继续操作，请输入你的访问令牌，此令牌必须有repo、admin:repo_hook权限。\n" +
                "如果要终止操作，请输入N。\n" +
                "本bot承诺不会保存你的访问令牌，仅在本次添加过程中使用此令牌。"
        );
        environment.getMessageSender().sendMessageAwait(context, message, (sent, reply) -> {
            List<TextMessage> texts = Stream.of(reply.getMessages())
                    .filter(m -> m instanceof TextMessage).map(m -> (TextMessage) m).collect(Collectors.toList());
            if (texts.size() == 1 && !texts.get(0).getText().equalsIgnoreCase("N")) {
                String token = texts.get(0).getText();
                AsyncUtil.execute(() -> {
                    try {
                        GitHubListener.LISTENER.getWebHookListener().addHook(repo, token);
                        environment.getMessageSender().sendMessage(context,
                                environment.newText("已成功创建Web Hook，请不要在仓库的Webhooks手动删除bot的hook。"));
                    } catch (IOException e) {
                        environment.getMessageSender().onError(e, "github.webhook.create", context, false);
                    }
                });
            } else
                environment.getMessageSender().sendMessage(context, environment.newText("已取消创建"));
        });
    }

    private static void deleteHook(String repo, MessageContext context, Environment environment) {
        AbstractMessage message = environment.newText(
                "警告: 你正在准备删除一个GitHub仓库的Web Hook，请确定你对该仓库具有完全的控制权。\n" +
                "如果要继续操作，请输入你的访问令牌，此令牌必须有repo、admin:repo_hook权限。\n" +
                "如果要终止操作，请输入N。\n" +
                "本bot承诺不会保存你的访问令牌，仅在本次删除过程中使用此令牌。"
        );
        environment.getMessageSender().sendMessageAwait(context, message, (sent, reply) -> {
            List<TextMessage> texts = Stream.of(reply.getMessages())
                    .filter(m -> m instanceof TextMessage).map(m -> (TextMessage) m).collect(Collectors.toList());
            if (texts.size() == 1 && !texts.get(0).getText().equalsIgnoreCase("N")) {
                String token = texts.get(0).getText();
                AsyncUtil.execute(() -> {
                    try {
                        GitHubListener.LISTENER.getWebHookListener().deleteHook(repo, token);
                        environment.getMessageSender().sendMessage(context,
                                environment.newText("已成功删除Web Hook。"));
                    } catch (IOException e) {
                        environment.getMessageSender().onError(e, "github.webhook.delete", context, false);
                    }
                });
            } else
                environment.getMessageSender().sendMessage(context, environment.newText("已取消删除"));
        });
    }
}
