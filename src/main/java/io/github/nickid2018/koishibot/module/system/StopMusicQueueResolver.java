package io.github.nickid2018.koishibot.module.system;

import io.github.nickid2018.koishibot.message.AudioSender;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.permission.PermissionLevel;
import io.github.nickid2018.koishibot.util.AsyncUtil;

@ResolverName("stopmusicqueue")
@Syntax(syntax = "~stopmusic", help = "停止播放音乐")
public class StopMusicQueueResolver extends MessageResolver {

    public StopMusicQueueResolver() {
        super("~stopmusic");
    }

    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.ADMIN;
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        AudioSender.PAUSE_FLAG.set(true);
        AsyncUtil.execute(() -> {
            environment.getMessageSender().sendMessage(context, environment.newText("已暂停播放"));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            AudioSender.PAUSE_FLAG.set(false);
        });
        return true;
    }
}
