package io.github.nickid2018.koishibot.module.music;

import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.message.DelegateEnvironment;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.web.MarkdownRenderer;

import java.io.IOException;
import java.util.stream.Collectors;

@ResolverName("music-search")
@Syntax(syntax = "~musicsearch [歌曲名]", help = "搜索网易云音乐")
public class MusicSearchResolver extends MessageResolver {

    public MusicSearchResolver() {
        super("~musicsearch");
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, DelegateEnvironment environment) {
        if (!key.isEmpty()) {
            AsyncUtil.execute(() -> {
                try {
                    JsonObject obj = NeteaseMusicProtocol.searchMusic(key);
                    StringBuilder builder = new StringBuilder();
                    builder.append("结果数量: ").append(JsonUtil.getIntOrZero(obj, "songCount")).append("\n\n");
                    builder.append("| ID | 歌曲名 | 专辑 | 作者 |\n");
                    builder.append("| --- | --- | --- | --- |\n");
                    JsonUtil.getData(obj, "songs", JsonArray.class)
                            .ifPresent(array -> array.forEach(element -> {
                                JsonObject song = element.getAsJsonObject();
                                builder.append("| ").append(JsonUtil.getIntOrZero(song, "id")).append(" | ");
                                builder.append(JsonUtil.getStringOrElse(song, "name", "Unknown"));
                                JsonUtil.getData(song, "alias", JsonArray.class)
                                        .filter(array1 -> !array1.isEmpty())
                                        .map(Streams::stream)
                                        .map(stream -> stream
                                                .map(JsonElement::getAsString)
                                                .collect(Collectors.joining(", ")))
                                        .ifPresent(str -> builder.append(" (").append(str).append(")"));
                                builder.append(" | ");
                                builder.append(JsonUtil.getStringInPathOrElse(song, "album.name", "Unknown")).append(" | ");
                                JsonUtil.getData(song, "artists", JsonArray.class)
                                        .map(Streams::stream)
                                        .map(stream -> stream
                                                .map(JsonElement::getAsJsonObject)
                                                .map(o -> JsonUtil.getStringOrElse(o, "name", "Unknown"))
                                                .collect(Collectors.joining(", ")))
                                        .ifPresent(builder::append);
                                builder.append(" |\n");
                            }));
                    MarkdownRenderer.render(builder.toString(),
                            png -> environment.getMessageSender().sendMessage(context, environment.newImage(png.toURI().toURL())),
                            e -> environment.getMessageSender().onError(e, "music.search.render", context, false));
                } catch (IOException e) {
                    environment.getMessageSender().onError(e, "music.search", context, false);
                }
            });
            return true;
        }
        return false;
    }
}
