package io.github.nickid2018.koishibot.message;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.resolver.*;
import io.github.nickid2018.koishibot.util.value.Either;
import io.github.nickid2018.koishibot.util.value.MutableBoolean;
import kotlin.Pair;
import kotlin.Triple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class MessageManager {

    private final Environment environment;

    public static final List<MessageResolver> RESOLVERS = new ArrayList<>();
    public static final Map<String, JSONServiceResolver> JSON_SERVICE_MAP = new HashMap<>();

    static {
        RESOLVERS.add(new HelpResolver());
        RESOLVERS.add(new InfoResolver());
        RESOLVERS.add(new LaTeXResolver());
        RESOLVERS.add(new QRCodeResolver());
        RESOLVERS.add(new WikiResolver());
        RESOLVERS.add(new BilibiliDataResolver());
        RESOLVERS.add(new BugTrackerResolver());
        RESOLVERS.add(new ModrinthResolver());
        RESOLVERS.add(new CurseForgeResolver());
        RESOLVERS.add(new TranslateResolver());
        RESOLVERS.add(new UrbanDictResolver());
        RESOLVERS.add(new GitHubRepoResolver());
        RESOLVERS.add(new GitHubWebHookResolver());
        RESOLVERS.add(new GitHubSubscribeResolver());

        JSON_SERVICE_MAP.put("哔哩哔哩", new BilibiliDataResolver());
    }

    public MessageManager(Environment environment) {
        this.environment = environment;
        MessageEventPublisher publisher = environment.getEvents();
        publisher.subscribeGroupMessage(this::onGroupMessage);
        publisher.subscribeFriendMessage(this::onFriendMessage);
        publisher.subscribeGroupTempMessage(this::onTempMessage);
        publisher.subscribeStrangerMessage(this::onStrangerMessage);
        publisher.subscribeNewMemberAdd(this::onMemberAdd);
        publisher.subscribeGroupRecall(this::onGroupRecall);
        publisher.subscribeFriendRecall(environment.getMessageSender()::onFriendRecall);
    }

    public Environment getEnvironment() {
        return environment;
    }

    private void onGroupMessage(Triple<GroupInfo, UserInfo, ChainMessage> messageTriple, long sentTime) {
        dealMessage(messageTriple.component1(), messageTriple.component2(), messageTriple.component3(),
                MessageResolver::groupEnabled, true, sentTime);
    }

    private void onFriendMessage(Pair<UserInfo, ChainMessage> message, long sentTime) {
        dealMessage(null, message.component1(), message.component2(), s -> true, false, sentTime);
    }

    private void onTempMessage(Pair<UserInfo, ChainMessage> message, long sentTime) {
        dealMessage(null, message.component1(), message.component2(),
                MessageResolver::groupTempChat, false, sentTime);
    }

    private void onStrangerMessage(Pair<UserInfo, ChainMessage> message, long sentTime) {
        dealMessage(null, message.component1(), message.component2(),
                MessageResolver::strangerChat, false, sentTime);
    }

    private void dealMessage(GroupInfo group, UserInfo user, ChainMessage message,
                             Predicate<MessageResolver> predicate, boolean inGroup, long sentTime) {
        MessageContext context = new MessageContext(group, user, message, sentTime);

        MutableBoolean ban = new MutableBoolean(false);
        if (MemberFilter.shouldNotResponse(user, ban)) {
            if (ban.getValue())
                environment.getMessageSender().sendMessage(context, environment.newChain(
                        environment.newAt(user), environment.newText(" 被自动封禁一小时，原因: 过于频繁的操作")
                ));
            return;
        }
        UserAwaitData.onMessage(group, user, message);
        MessageReplyData.onMessage(group, user, message);

        List<String> strings = new ArrayList<>();
        ServiceMessage service = null;
        boolean att = false;
        boolean replyMe = false;

        for (AbstractMessage content : message.getMessages()) {
            if (content instanceof TextMessage)
                strings.add(((TextMessage) content).getText());
            if (content instanceof AtMessage)
                if (((AtMessage) content).getId().equals(environment.getBotId()))
                    att = true;
            if (content instanceof QuoteMessage)
                if (((QuoteMessage) content).getReplyToID().equals(environment.getBotId()))
                    replyMe = true;
            if (content instanceof ServiceMessage)
                service = (ServiceMessage) content;
        }

        MutableBoolean bool = new MutableBoolean(false);
        if (service == null) {
            boolean finalAtt = att;
            RESOLVERS.stream().filter(predicate.and(s -> !inGroup || !s.needAt() || finalAtt))
                    .forEach(messageResolver -> strings.forEach(string -> {
                        if (!bool.getValue() && messageResolver.resolve(string, context, environment))
                            bool.setValue(true);
                    }));
            if (!bool.getValue() && att && inGroup && !replyMe) {
                environment.getMessageSender().sendMessage(context, environment.newChain(
                        environment.newQuote(message),
                        environment.newText("不要乱@人，会被514打电话的。")
                ));
            }
        } else {
            bool.setValue(true);
            String name = service.getName();
            Either<JsonObject, String> data = service.getData();
            if (data.isLeft() && JSON_SERVICE_MAP.containsKey(name))
                JSON_SERVICE_MAP.get(name).resolveService(data.getLeft(), context, environment);
        }

        if (bool.getValue() || (inGroup && att)) {
            MemberFilter.refreshRequestTime(user);
            if (Math.random() < 0.2)
                user.nudge(inGroup ? group : user);
        }
    }

    private void onMemberAdd(GroupInfo group, UserInfo user) {
        if (MemberFilter.shouldNotResponse(user, new MutableBoolean(false)))
            return;
        user.nudge(group);
        environment.getMessageSender().sendMessage(
                new MessageContext(group, null, null, -1), environment.newChain().fillChain(
                environment.newAt(user),
                environment.newText(" 欢迎来到本群，要使用Koishi bot可以at或私聊输入~help查看帮助")
        ));
    }

    private void onGroupRecall(Triple<GroupInfo, UserInfo, Long> info) {
        environment.getMessageSender().onRecall(info.component1(), info.component2(), info.component3());
    }
}
