package io.github.nickid2018.koishibot.resolver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.ImageMessage;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.message.qq.QQEnvironment;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.RegexUtil;
import io.github.nickid2018.koishibot.util.WebUtil;
import org.apache.http.client.methods.HttpGet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class BilibiliDataResolver extends MessageResolver implements JSONServiceResolver {

    public static final Pattern B_AV_VIDEO_PATTERN = Pattern.compile("[aA][vV][1-9]\\d{0,9}");
    public static final Pattern B_BV_VIDEO_PATTERN = Pattern.compile(
            "[bB][vV][fZodR9XQDSUm21yCkr6zBqiveYah8bt4xsWpHnJE7jL5VG3guMTKNPAwcF]{10}");
    public static final Pattern B_CV_ARTICLE_PATTERN = Pattern.compile("[cC][vV][1-9]\\d{0,9}");
    public static final Pattern B_AU_AUDIO_PATTERN = Pattern.compile("[aA][uU][1-9]\\d{0,9}");
    public static final Pattern B_EP_EPISODE_PATTERN = Pattern.compile("[eE][pP][1-9]\\d{0,7}");
    public static final Pattern B_SS_EPISODE_PATTERN = Pattern.compile("[sS]{2}[1-9]\\d{0,7}");
    public static final Pattern B_SHORT_LINK_PATTERN = Pattern.compile("https://b23\\.tv/[0-9a-zA-Z]+");

    public static final String BILIBILI_VIDEO_API = "https://api.bilibili.com/x/web-interface/";
    public static final String BILIBILI_ARTICLE_API = "https://api.bilibili.com/x/article/";
    public static final String BILIBILI_AUDIO_API = "https://www.bilibili.com/audio/music-service-c/web/song/";
    public static final String BILIBILI_EPISODE_API = "https://api.bilibili.com/pgc/view/web/season";

    public static final String[] EPISODE_TYPE = {null, "??????", "??????", "?????????", "??????", "?????????", null, "??????"};

    public BilibiliDataResolver() {
        super(B_AV_VIDEO_PATTERN, B_BV_VIDEO_PATTERN, B_CV_ARTICLE_PATTERN, B_AU_AUDIO_PATTERN,
                B_EP_EPISODE_PATTERN, B_SS_EPISODE_PATTERN, B_SHORT_LINK_PATTERN);
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        AsyncUtil.execute(() -> {
            try {
                if (resolvedArguments == B_SHORT_LINK_PATTERN)
                    fromShortLink(key, context, environment);
                else if (resolvedArguments == B_CV_ARTICLE_PATTERN)
                    doArticleDisplay(key, context, environment);
                else if (resolvedArguments == B_SS_EPISODE_PATTERN)
                    doEpisodeDisplay(key, context, true, environment);
                else if (resolvedArguments == B_EP_EPISODE_PATTERN)
                    doEpisodeDisplay(key, context, false, environment);
                else if (resolvedArguments == B_AU_AUDIO_PATTERN)
                    doAudioDisplay(key, context, environment);
                else
                    doVideoDisplay(key, context, environment);
            } catch (Exception e) {
                environment.getMessageSender().onError(e, "bilibili", context, true);
            }
        });
        return true;
    }

    private void choose(String key, MessageContext contact, Environment environment) throws IOException {
        if (RegexUtil.match(B_CV_ARTICLE_PATTERN, key))
            doArticleDisplay(key, contact, environment);
        else if (RegexUtil.match(B_SS_EPISODE_PATTERN, key))
            doEpisodeDisplay(key, contact, true, environment);
        else if (RegexUtil.match(B_EP_EPISODE_PATTERN, key))
            doEpisodeDisplay(key, contact, false, environment);
        else if (RegexUtil.match(B_AU_AUDIO_PATTERN, key))
            doAudioDisplay(key, contact, environment);
        else
            doVideoDisplay(key, contact, environment);
    }

    private void doEpisodeDisplay(String key, MessageContext contact, boolean isSSID, Environment environment) throws IOException {
        JsonObject articleData = WebUtil.fetchDataInJson(new HttpGet(
                BILIBILI_EPISODE_API + (isSSID ? "?season_id=" : "?ep_id=") + key.substring(2))).getAsJsonObject();
        int code = JsonUtil.getIntOrZero(articleData, "code");
        if (code != 0)
            throw new IOException("API????????????" + code + "(" + JsonUtil.getStringOrNull(articleData, "message") + ")");

        JsonObject data = articleData.getAsJsonObject("result");
        StringBuilder builder = new StringBuilder();

        builder.append("????????????: ").append(JsonUtil.getStringOrNull(data, "title")).append("\n");

        JsonObject publish = data.getAsJsonObject("publish");
        boolean isStart = JsonUtil.getIntOrZero(publish, "is_started") == 1;
        boolean isFinish = JsonUtil.getIntOrZero(publish, "is_finish") == 1;
        builder.append("??????: ");
        if (!isStart)
            builder.append("????????????");
        else {
            builder.append("??? ").append(JsonUtil.getStringOrNull(publish, "pub_time")).append(" ?????????");
            if (isFinish)
                builder.append("???????????????").append(JsonUtil.getIntOrZero(data, "total")).append("???");
            else
                builder.append("?????????");
        }
        builder.append("\n");

        builder.append("????????????: ").append(EPISODE_TYPE[JsonUtil.getIntOrZero(data, "type")]).append("\n");

        JsonObject stats = data.getAsJsonObject("stat");
        builder.append("????????????: ").append(JsonUtil.getIntOrZero(stats, "views")).append(" | ");
        builder.append("????????????: ").append(JsonUtil.getIntOrZero(stats, "danmakus")).append(" | ");
        builder.append("????????????: ").append(JsonUtil.getIntOrZero(stats, "reply")).append("\n");
        builder.append("????????????: ").append(JsonUtil.getIntOrZero(stats, "likes")).append(" | ");
        builder.append("????????????: ").append(JsonUtil.getIntOrZero(stats, "coins")).append(" | ");
        builder.append("????????????: ").append(JsonUtil.getIntOrZero(stats, "favorites")).append(" | ");
        builder.append("????????????: ").append(JsonUtil.getIntOrZero(stats, "share")).append("\n");

        builder.append("?????????URL: https://www.bilibili.com/bangumi/play/ss")
                .append(JsonUtil.getIntOrZero(data, "season_id")).append("\n");

        BufferedReader reader = new BufferedReader(new StringReader(JsonUtil.getStringOrElse(data, "evaluate", "")));
        String line;
        while ((line = reader.readLine()) != null && builder.length() <= 400) {
            line = line.trim();
            if (!line.isEmpty())
                builder.append(line).append("\n");
        }
        if (line != null)
            builder.append("(??????????????????)");

        ImageMessage image = environment.newImage();
        try (InputStream stream = new URL(JsonUtil.getStringOrNull(data, "cover")).openStream()) {
            image.fillImage(stream);
        }

        environment.getMessageSender().sendMessageRecallable(contact, environment.newChain(
                environment.newText(builder.toString().trim()),
                image
        ));
    }

    private void doAudioDisplay(String key, MessageContext contact, Environment environment) throws IOException {
        JsonObject articleData = WebUtil.fetchDataInJson(new HttpGet(
                BILIBILI_AUDIO_API + "info?sid=" + key.substring(2))).getAsJsonObject();
        int code = JsonUtil.getIntOrZero(articleData, "code");
        if (code != 0)
            throw new IOException("API????????????" + code + "(" + JsonUtil.getStringOrNull(articleData, "msg") + ")");

        JsonObject data = articleData.getAsJsonObject("data");
        StringBuilder builder = new StringBuilder();

        builder.append("????????????: ").append(JsonUtil.getStringOrNull(data, "title")).append("\n");
        builder.append("??????: ").append(JsonUtil.getStringOrNull(data, "author")).append("\n");
        builder.append("UP???: ").append(JsonUtil.getStringOrNull(data, "uname")).append("\n");

        builder.append("????????????: ").append(formatTime(JsonUtil.getIntOrZero(data, "duration"))).append("\n");

        long publishTime = JsonUtil.getIntOrZero(data, "passtime") * 1000L;
        Date date = new Date(publishTime);
        builder.append("????????????: ").append(String.format("%tY/%tm/%td %tT", date, date, date, date)).append("\n");

        JsonUtil.getString(data, "bvid").ifPresent(bvid -> builder.append("????????????: ").append(bvid).append("\n"));

        ImageMessage image = environment.newImage();
        try (InputStream stream = new URL(JsonUtil.getStringOrNull(data, "cover")).openStream()) {
            image.fillImage(stream);
        }

        environment.getMessageSender().sendMessageRecallable(contact, environment.newChain(
                environment.newText(builder.toString().trim()),
                image
        ));
    }

    private void doArticleDisplay(String key, MessageContext contact, Environment environment) throws IOException {
        JsonObject articleData = WebUtil.fetchDataInJson(new HttpGet(
                BILIBILI_ARTICLE_API + "viewinfo?id=" + key.substring(2))).getAsJsonObject();
        int code = JsonUtil.getIntOrZero(articleData, "code");
        if (code != 0)
            throw new IOException("API????????????" + code + "(" + JsonUtil.getStringOrNull(articleData, "message") + ")");

        JsonObject data = articleData.getAsJsonObject("data");
        StringBuilder builder = new StringBuilder();

        builder.append("????????????: ").append(JsonUtil.getStringOrNull(data, "title")).append("\n");
        builder.append("??????: ").append(JsonUtil.getStringOrNull(data, "author_name")).append("\n");

        JsonObject stats = data.getAsJsonObject("stats");
        builder.append("?????????: ").append(JsonUtil.getIntOrZero(stats, "view")).append(" | ");
        builder.append("?????????: ").append(JsonUtil.getIntOrZero(stats, "reply")).append("\n");
        builder.append("?????????: ").append(JsonUtil.getIntOrZero(stats, "like")).append(" | ");
        builder.append("?????????: ").append(JsonUtil.getIntOrZero(stats, "coin")).append(" | ");
        builder.append("?????????: ").append(JsonUtil.getIntOrZero(stats, "favorite")).append("\n");
        builder.append("???????????????: ").append(JsonUtil.getIntOrZero(stats, "dynamic")).append(" | ");
        builder.append("?????????: ").append(JsonUtil.getIntOrZero(stats, "share")).append("\n");

        builder.append("?????????URL: https://www.bilibili.com/read/").append(key);

        ImageMessage image = environment.newImage();
        try (InputStream stream = new URL(JsonUtil.getStringInPathOrNull(data, "origin_image_urls.0")).openStream()) {
            image.fillImage(stream);
        }

        environment.getMessageSender().sendMessageRecallable(contact, environment.newChain(
                environment.newText(builder.toString().trim()),
                image
        ));
    }

    private void doVideoDisplay(String key, MessageContext contact, Environment environment) throws IOException {
        JsonObject videoData;
        boolean useAVID = false;
        if (RegexUtil.match(B_AV_VIDEO_PATTERN, key)) {
            useAVID = true;
            videoData = WebUtil.fetchDataInJson(new HttpGet(
                    BILIBILI_VIDEO_API + "view?aid=" + key.substring(2))).getAsJsonObject();
        } else
            videoData = WebUtil.fetchDataInJson(new HttpGet(
                    BILIBILI_VIDEO_API + "view?bvid=" + key)).getAsJsonObject();

        int code = JsonUtil.getIntOrZero(videoData, "code");
        if (code != 0)
            throw new IOException("API????????????" + code + "(" + JsonUtil.getStringOrNull(videoData, "message") + ")");

        JsonObject data = videoData.getAsJsonObject("data");
        StringBuilder builder = new StringBuilder();
        builder.append("??????ID: ").append(JsonUtil.getStringOrNull(data, "bvid"));
        if (useAVID)
            builder.append(" (??????").append(key).append("????????????)");
        builder.append("\n");

        builder.append("????????????: ").append(JsonUtil.getStringOrNull(data, "title")).append("\n");
        builder.append("????????????: ").append(JsonUtil.getIntOrZero(data, "copyright") == 1 ? "??????" : "??????").append("\n");

        Optional<JsonArray> staffArray = JsonUtil.getData(data, "staff", JsonArray.class);
        if (staffArray.isPresent()) {
            builder.append("????????????: ");
            List<String> staffList = new ArrayList<>();
            for (JsonElement element : staffArray.get()) {
                JsonObject staff = element.getAsJsonObject();
                String name = JsonUtil.getStringOrNull(staff, "name");
                String title = JsonUtil.getStringOrNull(staff, "title");
                staffList.add(name + "(" + title + ")");
            }
            builder.append(String.join(", ", staffList));
        } else
            builder.append("UP???: ").append(JsonUtil.getStringInPathOrNull(data, "owner.name"));
        builder.append("\n");

        JsonObject stats = data.getAsJsonObject("stat");
        builder.append("?????????: ").append(JsonUtil.getIntOrZero(stats, "view")).append(" | ");
        builder.append("?????????: ").append(JsonUtil.getIntOrZero(stats, "danmaku")).append(" | ");
        builder.append("?????????: ").append(JsonUtil.getIntOrZero(stats, "reply")).append("\n");
        builder.append("?????????: ").append(JsonUtil.getIntOrZero(stats, "like")).append(" | ");
        builder.append("?????????: ").append(JsonUtil.getIntOrZero(stats, "coin")).append(" | ");
        builder.append("?????????: ").append(JsonUtil.getIntOrZero(stats, "favorite")).append(" | ");
        builder.append("?????????: ").append(JsonUtil.getIntOrZero(stats, "share")).append("\n");

        long publishTime = JsonUtil.getIntOrZero(data, "pubdate") * 1000L;
        Date date = new Date(publishTime);
        builder.append("????????????: ").append(String.format("%tY/%tm/%td %tT", date, date, date, date)).append("\n");

        builder.append("???????????????: ").append(formatTime(JsonUtil.getIntOrZero(data, "duration")));
        int videos = JsonUtil.getIntOrZero(data, "videos");
        if (videos > 1)
            builder.append("(???").append(videos).append("?????????)");
        builder.append("\n");

        builder.append("?????????URL: https://www.bilibili.com/video/").append(
                JsonUtil.getStringOrNull(data, "bvid")).append("\n");

        BufferedReader reader = new BufferedReader(new StringReader(JsonUtil.getStringOrNull(data, "desc")));
        String line;
        while ((line = reader.readLine()) != null && builder.length() <= 400) {
            line = line.trim();
            if (!line.isEmpty())
                builder.append(line).append("\n");
        }
        if (line != null)
            builder.append("(??????????????????)");

        ImageMessage image = environment.newImage();
        try (InputStream stream = new URL(JsonUtil.getStringOrNull(data, "pic")).openStream()) {
            image.fillImage(stream);
        }

        environment.getMessageSender().sendMessageRecallable(contact, environment.newChain(
                environment.newText(builder.toString().trim()),
                image
        ));
    }

    private void fromShortLink(String key, MessageContext contact, Environment environment) throws IOException {
        String location = WebUtil.getRedirected(new HttpGet(key));
        if (location == null)
            return;
        String id = location.split("\\?")[0];
        id = id.substring(id.lastIndexOf('/') + 1);
        choose(id, contact, environment);
    }

    private static String formatTime(int time) {
        if (time < 60)
            return time + "s";
        if (time < 3600)
            return time / 60 + "m" + time % 60 + "s";
        return time / 3600 + "h" + (time % 3600) / 60 + "m" + time % 60 + "s";
    }

    @Override
    public void resolveService(JsonObject content, MessageContext context, Environment environment) {
        AsyncUtil.execute(() -> {
            if (environment instanceof QQEnvironment) {
                try {
                    String url = JsonUtil.getStringInPathOrNull(content, "meta.news.jumpUrl");
                    if (url == null)
                        url = JsonUtil.getStringInPathOrNull(content, "meta.detail_1.qqdocurl");
                    if (url == null)
                        return;
                    url = url.split("\\?")[0];
                    fromShortLink(url, context, environment);
                } catch (IOException e) {
                    environment.getMessageSender().onError(e, "bilibili.service", context, false);
                }
            }
        });
    }
}
