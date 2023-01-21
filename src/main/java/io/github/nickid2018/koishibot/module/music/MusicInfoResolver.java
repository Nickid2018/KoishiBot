package io.github.nickid2018.koishibot.module.music;

import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.JsonUtil;
import kotlin.Pair;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.stream.Collectors;

@ResolverName("musicinfo")
@Syntax(syntax = "~musicinfo [网易云音乐ID]", help = "获取网易云音乐信息")
public class MusicInfoResolver extends MessageResolver {

    public MusicInfoResolver() {
        super("~musicinfo");
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
                        Pair<String, URL> pair = getMusicInfo(obj);
                        environment.getMessageSender().sendMessage(context, environment.newChain(
                                environment.newText(pair.getFirst()),
                                environment.newImage(pair.getSecond().openStream())
                        ));
                    } else
                        environment.getMessageSender().sendMessage(context, environment.newText("未找到歌曲"));
                } catch (Exception e) {
                    environment.getMessageSender().sendMessage(context, environment.newText("获取音乐信息失败"));
                }
            });
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static Pair<String, URL> getMusicInfo(JsonObject obj) throws MalformedURLException {
        String name = JsonUtil.getStringOrNull(obj, "name");
        JsonArray artists = obj.getAsJsonArray("ar");
        String artistsName = Streams.stream(artists)
                .map(JsonObject.class::cast)
                .map(a -> JsonUtil.getStringOrNull(a, "name"))
                .collect(Collectors.joining(", "));
        String albumName = JsonUtil.getStringInPathOrNull(obj, "al.name");
        String albumPic = JsonUtil.getStringInPathOrNull(obj, "al.picUrl");

        StringBuilder builder = new StringBuilder();
        builder.append("歌曲名: ").append(name).append("\n");
        builder.append("歌手: ").append(artistsName).append("\n");
        builder.append("专辑: ").append(albumName);
        JsonUtil.getData(obj, "alia", JsonArray.class)
                .filter(array -> !array.isEmpty())
                .ifPresent(array -> Streams.stream(array)
                        .map(JsonPrimitive.class::cast)
                        .map(JsonPrimitive::getAsString)
                        .forEach(alia -> builder.append("\n").append(alia)));
        return new Pair<>(builder.toString(), new URL(albumPic));
    }
}
