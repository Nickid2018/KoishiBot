package io.github.nickid2018.koishibot.message.kook;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.kookybot.contract.GuildUser;
import io.github.kookybot.contract.TextChannel;
import io.github.kookybot.events.channel.ChannelMessageEvent;
import io.github.nickid2018.koishibot.util.JsonUtil;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class KOOKMessageData {

    private String msgID;
    private String quoteID;
    private GuildUser quoteUser;
    private KOOKMessageData quoteData;
    private long timestamp;
    private final Set<GuildUser> mentionUsers = new HashSet<>();
    private final List<String> texts = new ArrayList<>();

    public static final Pattern SPECIALS = Pattern.compile("\\(((met)|(rol)|(chn))\\)\\w+?\\(((met)|(rol)|(chn))\\)");

    public static KOOKMessageData fromChannelMessage(ChannelMessageEvent channelMessage) {
        KOOKMessageData data = new KOOKMessageData();

        JsonUtil.getData(channelMessage.getExtra(), "mention", JsonArray.class).ifPresent(mentions -> {
            for (JsonElement element : mentions)
                data.mentionUsers.add(channelMessage.getChannel().getGuild().getGuildUser(element.getAsString()));
        });

        Arrays.stream(SPECIALS.split(channelMessage.getContent()))
                .map(s -> s.replace("\\[", "[").replace("\\]", "]"))
                .filter(Predicate.not(String::isBlank))
                .forEach(data.texts::add);
        data.timestamp = Long.parseLong(channelMessage.getTimestamp());

        JsonUtil.getData(channelMessage.getExtra(), "quote", JsonObject.class).ifPresent(quote -> {
            data.quoteData = fromQuote(channelMessage.getChannel(), quote);
            data.quoteID = data.quoteData.getMsgID();
            data.quoteUser = channelMessage.getChannel().getGuild().getGuildUser(
                    JsonUtil.getStringInPathOrNull(quote, "author.id"));
        });

        return data;
    }

    public static KOOKMessageData fromQuote(TextChannel channel, JsonObject message) {
        KOOKMessageData data = new KOOKMessageData();
        JsonUtil.getDataInPath(message, "kmarkdown.mention_part", JsonArray.class).ifPresent(mentions -> {
            for (JsonElement element : mentions)
                data.mentionUsers.add(channel.getGuild().getGuildUser(
                        JsonUtil.getStringOrNull(element.getAsJsonObject(), "id")));
        });
        Arrays.stream(SPECIALS.split(JsonUtil.getStringOrNull(message, "content")))
                .map(s -> s.replace("\\[", "[").replace("\\]", "]"))
                .filter(Predicate.not(String::isBlank))
                .forEach(data.texts::add);

        data.timestamp = Long.parseLong(JsonUtil.getStringOrNull(message, "create_at"));
        data.msgID = JsonUtil.getStringOrNull(message, "id");
        return data;
    }

    public Set<GuildUser> getMentionUsers() {
        return mentionUsers;
    }

    public List<String> getTexts() {
        return texts;
    }

    public String getMsgID() {
        return msgID;
    }

    public String getQuoteID() {
        return quoteID;
    }

    public void setQuoteID(String quoteID) {
        this.quoteID = quoteID;
    }

    public GuildUser getQuoteUser() {
        return quoteUser;
    }

    public KOOKMessageData getQuoteData() {
        return quoteData;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
