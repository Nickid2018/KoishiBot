package io.github.nickid2018.koishibot.resolver;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.KoishiBotMain;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.ImageMessage;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.message.qq.QQEnvironment;
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

    public static final String[] EPISODE_TYPE = {null, "番剧", "电影", "纪录片", "国创", "电视剧", null, "综艺"};

    public BilibiliDataResolver() {
        super(B_AV_VIDEO_PATTERN, B_BV_VIDEO_PATTERN, B_CV_ARTICLE_PATTERN, B_AU_AUDIO_PATTERN,
                B_EP_EPISODE_PATTERN, B_SS_EPISODE_PATTERN, B_SHORT_LINK_PATTERN);
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Pattern pattern, Environment environment) {
        KoishiBotMain.INSTANCE.executor.execute(() -> {
            try {
                if (pattern == B_SHORT_LINK_PATTERN)
                    fromShortLink(key, context, environment);
                else if (pattern == B_CV_ARTICLE_PATTERN)
                    doArticleDisplay(key, context, environment);
                else if (pattern == B_SS_EPISODE_PATTERN)
                    doEpisodeDisplay(key, context, true, environment);
                else if (pattern == B_EP_EPISODE_PATTERN)
                    doEpisodeDisplay(key, context, false, environment);
                else if (pattern == B_AU_AUDIO_PATTERN)
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
        int code = articleData.get("code").getAsInt();
        if (code != 0)
            throw new IOException("API接口返回" + code + "(" + articleData.get("message").getAsString() + ")");

        JsonObject data = articleData.getAsJsonObject("result");
        StringBuilder builder = new StringBuilder();

        builder.append("剧集标题: ").append(data.get("title").getAsString()).append("\n");

        JsonObject publish = data.getAsJsonObject("publish");
        boolean isStart = publish.get("is_started").getAsInt() == 1;
        boolean isFinish = publish.get("is_finish").getAsInt() == 1;
        builder.append("状态: ");
        if (!isStart)
            builder.append("尚未发布");
        else {
            builder.append("于 ").append(publish.get("pub_time").getAsString()).append(" 发布，");
            if (isFinish)
                builder.append("已完结，共").append(data.get("total").getAsInt()).append("集");
            else
                builder.append("未完结");
        }
        builder.append("\n");

        builder.append("剧集类型: ").append(EPISODE_TYPE[data.get("type").getAsInt()]).append("\n");

        JsonObject stats = data.getAsJsonObject("stat");
        builder.append("总播放量: ").append(stats.get("views").getAsInt()).append(" | ");
        builder.append("总弹幕数: ").append(stats.get("danmakus").getAsInt()).append(" | ");
        builder.append("总评论数: ").append(stats.get("reply").getAsInt()).append("\n");
        builder.append("总点赞数: ").append(stats.get("likes").getAsInt()).append(" | ");
        builder.append("总硬币数: ").append(stats.get("coins").getAsInt()).append(" | ");
        builder.append("总收藏数: ").append(stats.get("favorites").getAsInt()).append(" | ");
        builder.append("总分享数: ").append(stats.get("share").getAsInt()).append("\n");

        builder.append("主条目URL: https://www.bilibili.com/bangumi/play/ss").append(data.get("season_id")).append("\n");

        BufferedReader reader = new BufferedReader(new StringReader(data.get("evaluate").getAsString()));
        String line;
        while ((line = reader.readLine()) != null && builder.length() <= 400) {
            line = line.trim();
            if (!line.isEmpty())
                builder.append(line).append("\n");
        }
        if (line != null)
            builder.append("(简介过长截断)");

        ImageMessage image = environment.newImage();
        try (InputStream stream = new URL(data.get("cover").getAsString()).openStream()) {
            image.fillImage(stream);
        }

        environment.getMessageSender().sendMessageRecallable(contact, environment.newChain(
                environment.newText(builder.toString()),
                image
        ));
    }

    private void doAudioDisplay(String key, MessageContext contact, Environment environment) throws IOException {
        JsonObject articleData = WebUtil.fetchDataInJson(new HttpGet(
                BILIBILI_AUDIO_API + "info?sid=" + key.substring(2))).getAsJsonObject();
        int code = articleData.get("code").getAsInt();
        if (code != 0)
            throw new IOException("API接口返回" + code + "(" + articleData.get("msg").getAsString() + ")");

        JsonObject data = articleData.getAsJsonObject("data");
        StringBuilder builder = new StringBuilder();

        builder.append("音频标题: ").append(data.get("title").getAsString()).append("\n");
        builder.append("作者: ").append(data.get("author").getAsString()).append("\n");
        builder.append("UP主: ").append(data.get("uname").getAsString()).append("\n");

        builder.append("音频长度: ").append(formatTime(data.get("duration").getAsInt())).append("\n");

        long publishTime = data.get("passtime").getAsInt() * 1000L;
        Date date = new Date(publishTime);
        builder.append("发布时间: ").append(String.format("%tY/%tm/%td %tT", date, date, date, date)).append("\n");

        JsonElement bvid = data.get("bvid");
        if (bvid.isJsonPrimitive()) {
            String bvidS = bvid.getAsString();
            if (!bvidS.isEmpty())
                builder.append("关联视频: ").append(bvidS).append("\n");
        }

        ImageMessage image = environment.newImage();
        try (InputStream stream = new URL(data.get("cover").getAsString()).openStream()) {
            image.fillImage(stream);
        }

        environment.getMessageSender().sendMessageRecallable(contact, environment.newChain(
                environment.newText(builder.toString()),
                image
        ));
    }

    private void doArticleDisplay(String key, MessageContext contact, Environment environment) throws IOException {
        JsonObject articleData = WebUtil.fetchDataInJson(new HttpGet(
                BILIBILI_ARTICLE_API + "viewinfo?id=" + key.substring(2))).getAsJsonObject();
        int code = articleData.get("code").getAsInt();
        if (code != 0)
            throw new IOException("API接口返回" + code + "(" + articleData.get("message").getAsString() + ")");

        JsonObject data = articleData.getAsJsonObject("data");
        StringBuilder builder = new StringBuilder();

        builder.append("专栏标题: ").append(data.get("title").getAsString()).append("\n");
        builder.append("作者: ").append(data.get("author_name").getAsString()).append("\n");

        JsonObject stats = data.getAsJsonObject("stats");
        builder.append("阅读数: ").append(stats.get("view").getAsInt()).append(" | ");
        builder.append("评论数: ").append(stats.get("reply").getAsInt()).append("\n");
        builder.append("点赞数: ").append(stats.get("like").getAsInt()).append(" | ");
        builder.append("硬币数: ").append(stats.get("coin").getAsInt()).append(" | ");
        builder.append("收藏数: ").append(stats.get("favorite").getAsInt()).append("\n");
        builder.append("动态转发数: ").append(stats.get("dynamic").getAsInt()).append(" | ");
        builder.append("分享数: ").append(stats.get("share").getAsInt()).append("\n");

        builder.append("主条目URL: https://www.bilibili.com/read/").append(key);

        ImageMessage image = environment.newImage();
        try (InputStream stream = new URL(data.getAsJsonArray("origin_image_urls").get(0).getAsString()).openStream()) {
            image.fillImage(stream);
        }

        environment.getMessageSender().sendMessageRecallable(contact, environment.newChain(
                environment.newText(builder.toString()),
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
        int code = videoData.get("code").getAsInt();
        if (code != 0)
            throw new IOException("API接口返回" + code + "(" + videoData.get("message").getAsString() + ")");

        JsonObject data = videoData.getAsJsonObject("data");
        StringBuilder builder = new StringBuilder();
        builder.append("视频ID: ").append(data.get("bvid").getAsString());
        if (useAVID)
            builder.append(" (已从").append(key).append("自动转换)");
        builder.append("\n");

        builder.append("视频标题: ").append(data.get("title").getAsString()).append("\n");
        builder.append("视频类型: ").append(data.get("copyright").getAsInt() == 1 ? "自制" : "转载").append("\n");

        if (data.has("staff")) {
            builder.append("制作团队: ");
            List<String> staffList = new ArrayList<>();
            for (JsonElement element : data.getAsJsonArray("staff")) {
                JsonObject staff = element.getAsJsonObject();
                String name = staff.get("name").getAsString();
                String title = staff.get("title").getAsString();
                staffList.add(name + "(" + title + ")");
            }
            builder.append(String.join(", ", staffList));
        } else
            builder.append("UP主: ").append(JsonUtil.getStringInPathOrNull(data, "owner.name"));
        builder.append("\n");

        JsonObject stats = data.getAsJsonObject("stat");
        builder.append("播放量: ").append(stats.get("view").getAsInt()).append(" | ");
        builder.append("弹幕数: ").append(stats.get("danmaku").getAsInt()).append(" | ");
        builder.append("评论数: ").append(stats.get("reply").getAsInt()).append("\n");
        builder.append("点赞数: ").append(stats.get("like").getAsInt()).append(" | ");
        builder.append("硬币数: ").append(stats.get("coin").getAsInt()).append(" | ");
        builder.append("收藏数: ").append(stats.get("favorite").getAsInt()).append(" | ");
        builder.append("分享数: ").append(stats.get("share").getAsInt()).append("\n");

        long publishTime = data.get("pubdate").getAsInt() * 1000L;
        Date date = new Date(publishTime);
        builder.append("发布时间: ").append(String.format("%tY/%tm/%td %tT", date, date, date, date)).append("\n");

        builder.append("视频总长度: ").append(formatTime(data.get("duration").getAsInt()));
        int videos = data.get("videos").getAsInt();
        if (videos > 1)
            builder.append("(共").append(videos).append("个视频)");
        builder.append("\n");

        builder.append("主条目URL: https://www.bilibili.com/video/").append(data.get("bvid").getAsString()).append("\n");

        BufferedReader reader = new BufferedReader(new StringReader(data.get("desc").getAsString()));
        String line;
        while ((line = reader.readLine()) != null && builder.length() <= 400) {
            line = line.trim();
            if (!line.isEmpty())
                builder.append(line).append("\n");
        }
        if (line != null)
            builder.append("(简介过长截断)");

        ImageMessage image = environment.newImage();
        try (InputStream stream = new URL(data.get("pic").getAsString()).openStream()) {
            image.fillImage(stream);
        }

        environment.getMessageSender().sendMessageRecallable(contact, environment.newChain(
                environment.newText(builder.toString()),
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
        KoishiBotMain.INSTANCE.executor.execute(() -> {
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
