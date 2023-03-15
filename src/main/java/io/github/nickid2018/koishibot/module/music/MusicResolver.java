package io.github.nickid2018.koishibot.module.music;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.message.AudioSender;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.permission.PermissionLevel;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import kotlin.Pair;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.Future;

@ResolverName("music")
@Syntax(syntax = "~music [网易云音乐ID]", help = "播放网易云音乐")
public class MusicResolver extends MessageResolver {

    public MusicResolver() {
        super("~music");
    }

    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.ADMIN;
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        try {
            int id = Integer.parseInt(key);
            AsyncUtil.execute(() -> {
                try {
                    Optional<JsonObject> data = NeteaseMusicProtocol.getMusicInfo(id);
                    if (data.isPresent()) {
                        JsonObject obj = data.get();
                        String url = NeteaseMusicProtocol.getMusicDataURL(id);
                        Pair<String, URL> pair = MusicInfoResolver.getMusicInfo(obj);
                        if (url != null) {
                            Future<File[]> fileToSend = environment.parseAudioFile("mp3", new URL(url));
                            environment.getMessageSender().sendMessage(context, environment.newChain(
                                    environment.newText(pair.getFirst()),
                                    environment.newText("\n(源URL: " + url + ")"),
                                    environment.newImage(pair.getSecond().openStream())
                            ));
                            AudioSender.sendAudio(fileToSend, context, environment);
                        } else
                            environment.getMessageSender().sendMessage(context, environment.newChain(
                                    environment.newText(pair.getFirst()),
                                    environment.newText("\n由于版权或其他问题，此歌曲不提供播放。"),
                                    environment.newImage(pair.getSecond().openStream())
                            ));
                    } else
                        environment.getMessageSender().sendMessage(context, environment.newText("未找到歌曲"));
                } catch (Exception e) {
                    environment.getMessageSender().onError(e, "music.get", context, false);
                }
            });
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}