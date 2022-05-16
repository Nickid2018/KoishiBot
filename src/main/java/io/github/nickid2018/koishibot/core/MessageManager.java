package io.github.nickid2018.koishibot.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.nickid2018.koishibot.KoishiBotMain;
import io.github.nickid2018.koishibot.resolver.*;
import io.github.nickid2018.koishibot.util.ErrorCodeException;
import io.github.nickid2018.koishibot.util.MutableBoolean;
import kotlin.Pair;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.event.events.MessageRecallEvent;
import net.mamoe.mirai.message.MessageReceipt;
import net.mamoe.mirai.message.data.*;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Predicate;

public class MessageManager {

    public static final List<MessageResolver> RESOLVERS = new ArrayList<>();
    public static final Map<String, ServiceResolver> SERVICES = new HashMap<>();

    public static final Queue<Pair<MessageEvent, MessageReceipt<?>>> SENT_QUOTE_QUEUE = new ConcurrentLinkedDeque<>();

    public static void resolveGroupMessage(MessageChain chain, MessageInfo info) {
        if (UserAwaitData.onMessage(info))
            return;
        if (chain.get(1) instanceof RichMessage) {
            RichMessage message = (RichMessage) chain.get(1);
            JsonObject content = JsonParser.parseString(message.getContent()).getAsJsonObject();
            String desc = content.get("desc").getAsString();
            if (SERVICES.containsKey(desc))
                SERVICES.get(desc).resolveService(content, info);
            return;
        }
        List<String> strings = new ArrayList<>();
        boolean at = false;
        boolean replyMe = false;
        for (SingleMessage content : chain) {
            if (content instanceof At)
                at |= ((At) content).component1() == Settings.BOT_QQ;
            else if (content instanceof PlainText)
                strings.add(((PlainText) content).component1());
            else if (content instanceof QuoteReply)
                replyMe |= ((QuoteReply) content).component1().getFromId() == Settings.BOT_QQ;
        }
        boolean finalAt = at;
        MutableBoolean bool = new MutableBoolean(false);
        RESOLVERS.stream().filter(messageResolver -> !messageResolver.needAt() || finalAt)
                .forEach(messageResolver -> strings.forEach(string -> {
                    if (!bool.getValue() && messageResolver.resolve(string, info))
                        bool.setValue(true);
                }));
        if (at && !bool.getValue() && !replyMe)
            info.sendMessage(MessageUtils.newChain(new QuoteReply(chain), new PlainText("不要乱@人，会被514打电话的。")));
    }

    public static void resolveFriendMessage(MessageChain chain, MessageInfo info) {
        if (UserAwaitData.onMessage(info))
            return;
        List<String> strings = new ArrayList<>();
        for (SingleMessage content : chain) {
            if (content instanceof PlainText)
                strings.add(((PlainText) content).component1());
        }
        RESOLVERS.stream().filter(messageResolver -> !messageResolver.groupOnly())
                .forEach(messageResolver -> strings.forEach(string -> messageResolver.resolve(string, info)));
    }

    public static void resolveTempMessage(MessageChain chain, MessageInfo info, Predicate<MessageResolver> predicate) {
        if (UserAwaitData.onMessage(info))
            return;
        List<String> strings = new ArrayList<>();
        for (SingleMessage content : chain) {
            if (content instanceof PlainText)
                strings.add(((PlainText) content).component1());
        }
        RESOLVERS.stream().filter(predicate)
                .forEach(messageResolver -> strings.forEach(string -> messageResolver.resolve(string, info)));
    }

    public static void resolveGroupTempMessage(MessageChain chain, MessageInfo info) {
        resolveTempMessage(chain, info, MessageResolver::groupTempChat);
    }

    public static void resolveStrangerMessage(MessageChain chain, MessageInfo info) {
        resolveTempMessage(chain, info, MessageResolver::strangerChat);
    }

    public static void onError(Throwable t, String module, MessageInfo info, boolean quote) {
        ErrorRecord.enqueueError(module, t);
        MessageChain chain;
        if (t instanceof ErrorCodeException) {
            int code = ((ErrorCodeException) t).code;
            try {
                Image image = Contact.uploadImage(KoishiBotMain.INSTANCE.botKoishi.getAsFriend(),
                                new URL("https://http.cat/" + code).openStream());
                chain = MessageUtils.newChain(
                        new QuoteReply(info.data),
                        new PlainText("调用API返回了状态码" + code),
                        image
                );
            } catch (IOException e) {
                chain = MessageUtils.newChain(
                        new QuoteReply(info.data),
                        new PlainText("调用API返回了状态码" + code)
                );
            }
        } else
            chain = MessageUtils.newChain(
                    new QuoteReply(info.data),
                    new PlainText("调用API产生了错误：" + t.getMessage())
            );
        if (quote)
            info.sendMessageWithQuote(chain);
        else
            info.sendMessage(chain);
    }

    static {
        RESOLVERS.add(new InfoResolver());
        RESOLVERS.add(new WikiResolver());
        RESOLVERS.add(new TranslateResolver());
        RESOLVERS.add(new BilibiliDataResolver());
        RESOLVERS.add(new BugTrackerResolver());
        RESOLVERS.add(new CurseForgeResolver());
        RESOLVERS.add(new HelpResolver());

        SERVICES.put("哔哩哔哩", new BilibiliDataResolver());
    }

    public static void onGroupRecall(MessageRecallEvent.GroupRecall data) {
        for (Pair<MessageEvent, MessageReceipt<?>> pair : SENT_QUOTE_QUEUE) {
            if (!(pair.component1() instanceof GroupMessageEvent))
                continue;
            GroupMessageEvent event = (GroupMessageEvent) pair.component1();
            if (event.getGroup().equals(data.getGroup()) &&
                event.getTime() == data.getMessageTime() &&
                event.getSender().equals(data.component6())) {
                pair.component2().recall();
            }
        }
    }

    public static void onFriendRecall(MessageRecallEvent.FriendRecall data) {
        for (Pair<MessageEvent, MessageReceipt<?>> pair : SENT_QUOTE_QUEUE) {
            if (!(pair.component1() instanceof FriendMessageEvent))
                continue;
            FriendMessageEvent event = (FriendMessageEvent) pair.component1();
            if (event.getTime() == data.getMessageTime() && event.getSender().equals(data.component6())) {
                pair.component2().recall();
            }
        }
    }
}
