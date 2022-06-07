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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Predicate;

public class MessageManager {

    public static final Logger LOGGER = LoggerFactory.getLogger("KoishiBot-Message");

    public static final List<MessageResolver> RESOLVERS = new ArrayList<>();
    public static final Map<String, ServiceResolver> SERVICES = new HashMap<>();
    public static final String[] ERROR_MESSAGES = new String[] {
            "发生了错误", "bot发生了异常", "bot陷入无意识之中"
    };

    private static final Random RANDOM = new Random();

    public static final Queue<Pair<MessageEvent, MessageReceipt<?>>> SENT_QUOTE_QUEUE = new ConcurrentLinkedDeque<>();

    public static void tryNudge(MessageInfo info) {
        if (RANDOM.nextDouble() < 0.2)
            info.nudge();
    }

    public static boolean resolveGroupMessage(MessageChain chain, MessageInfo info) {
        UserAwaitData.onMessage(info);
        if (chain.get(1) instanceof RichMessage) {
            RichMessage message = (RichMessage) chain.get(1);
            JsonObject content = JsonParser.parseString(message.getContent()).getAsJsonObject();
            String desc = content.get("desc").getAsString();
            if (SERVICES.containsKey(desc))
                SERVICES.get(desc).resolveService(content, info);
            return true;
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
        return bool.getValue() || at;
    }

    public static boolean resolveFriendMessage(MessageChain chain, MessageInfo info) {
        UserAwaitData.onMessage(info);
        List<String> strings = new ArrayList<>();
        for (SingleMessage content : chain) {
            if (content instanceof PlainText)
                strings.add(((PlainText) content).component1());
        }
        MutableBoolean bool = new MutableBoolean(false);
        RESOLVERS.stream().filter(messageResolver -> !messageResolver.groupOnly())
                .forEach(messageResolver -> strings.forEach(string -> {
                    if (!bool.getValue() && messageResolver.resolve(string, info))
                        bool.setValue(true);
                }));
        return bool.getValue();
    }

    public static boolean resolveTempMessage(MessageChain chain, MessageInfo info, Predicate<MessageResolver> predicate) {
        UserAwaitData.onMessage(info);
        List<String> strings = new ArrayList<>();
        for (SingleMessage content : chain) {
            if (content instanceof PlainText)
                strings.add(((PlainText) content).component1());
        }
        MutableBoolean bool = new MutableBoolean(false);
        RESOLVERS.stream().filter(predicate)
                .forEach(messageResolver -> strings.forEach(string -> {
                    if (!bool.getValue() && messageResolver.resolve(string, info))
                        bool.setValue(true);
                }));
        return bool.getValue();
    }

    public static boolean resolveGroupTempMessage(MessageChain chain, MessageInfo info) {
        return resolveTempMessage(chain, info, MessageResolver::groupTempChat);
    }

    public static boolean resolveStrangerMessage(MessageChain chain, MessageInfo info) {
        return resolveTempMessage(chain, info, MessageResolver::strangerChat);
    }

    public static void onError(Throwable t, String module, MessageInfo info, boolean quote) {
        ErrorRecord.enqueueError(module, t);
        MessageChain chain;
        String choose = ERROR_MESSAGES[RANDOM.nextInt(ERROR_MESSAGES.length)];
        if (t instanceof ErrorCodeException) {
            int code = ((ErrorCodeException) t).code;
            try {
                Image image = Contact.uploadImage(KoishiBotMain.INSTANCE.botKoishi.getAsFriend(),
                                new URL("https://http.cat/" + code).openStream());
                chain = MessageUtils.newChain(
                        new QuoteReply(info.data),
                        new PlainText(choose + ": 状态码" + code),
                        image
                );
            } catch (IOException e) {
                chain = MessageUtils.newChain(
                        new QuoteReply(info.data),
                        new PlainText(choose + ": 状态码" + code)
                );
            }
        } else {
            String message = t.getMessage();
            chain = MessageUtils.newChain(
                    new QuoteReply(info.data),
                    new PlainText(choose + ": " + (message.length() > 100 ? t.getClass().getName() : message))
            );
        }
        if (quote)
            info.sendMessageRecallable(chain);
        else
            info.sendMessage(chain);
        LOGGER.error(module, t);
        t.printStackTrace();
    }

    static {
        RESOLVERS.add(new InfoResolver());
        RESOLVERS.add(new WikiResolver());
        RESOLVERS.add(new TranslateResolver());
        RESOLVERS.add(new BilibiliDataResolver());
        RESOLVERS.add(new BugTrackerResolver());
        RESOLVERS.add(new CurseForgeResolver());
        RESOLVERS.add(new HelpResolver());
        RESOLVERS.add(new LaTeXResolver());
        RESOLVERS.add(new QRCodeResolver());

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
