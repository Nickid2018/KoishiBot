package io.github.nickid2018.koishibot.message;

import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.resolver.*;
import io.github.nickid2018.koishibot.util.MutableBoolean;
import kotlin.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class MessageManager {

    private final Environment environment;

    public static final List<MessageResolver> RESOLVERS = new ArrayList<>();

    static {
        RESOLVERS.add(new HelpResolver());
        RESOLVERS.add(new InfoResolver());
        RESOLVERS.add(new LaTeXResolver());
        RESOLVERS.add(new QRCodeResolver());
        RESOLVERS.add(new WikiResolver());
        RESOLVERS.add(new BilibiliDataResolver());
        RESOLVERS.add(new BugTrackerResolver());
        RESOLVERS.add(new CurseForgeResolver());
        RESOLVERS.add(new TranslateResolver());
        RESOLVERS.add(new GitHubWebHookResolver());
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

    private void onGroupMessage(Triple<GroupInfo, UserInfo, ChainMessage> messageTriple) {
        dealMessage(messageTriple.component1(), messageTriple.component2(), messageTriple.component3(),
                MessageResolver::groupEnabled, true);
    }

    private void onFriendMessage(UserInfo user, ChainMessage message) {
        dealMessage(null, user, message, s -> true, false);
    }

    private void onTempMessage(UserInfo user, ChainMessage message) {
        dealMessage(null, user, message, MessageResolver::groupTempChat, false);
    }

    private void onStrangerMessage(UserInfo user, ChainMessage message) {
        dealMessage(null, user, message, MessageResolver::strangerChat, false);
    }

    private void dealMessage(GroupInfo group, UserInfo user, ChainMessage message,
                             Predicate<MessageResolver> predicate, boolean inGroup) {
        if (MemberFilter.shouldNotResponse(user))
            return;
        UserAwaitData.onMessage(group, user, message);
        List<String> strings = new ArrayList<>();
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
        }
        MutableBoolean bool = new MutableBoolean(false);
        boolean finalAtt = att;
        MessageContext contact = new MessageContext(group, user, message);
        RESOLVERS.stream().filter(predicate.and(s -> !inGroup || !s.needAt() || finalAtt))
                .forEach(messageResolver -> strings.forEach(string -> {
                    if (!bool.getValue() && messageResolver.resolve(string, contact, environment))
                        bool.setValue(true);
                }));
        if (!bool.getValue() && att && inGroup && !replyMe) {
            environment.getMessageSender().sendMessage(contact, environment.newChain(
                    environment.newQuote(message),
                    environment.newText("不要乱@人，会被514打电话的。")
            ));
        }
        if (bool.getValue() || (inGroup && att)) {
            MemberFilter.refreshRequestTime(user);
            user.nudge(inGroup ? group : user);
        }
    }

    private void onMemberAdd(GroupInfo group, UserInfo user) {
        if (MemberFilter.shouldNotResponse(user))
            return;
        user.nudge(group);
        environment.getMessageSender().sendMessage(new MessageContext(group, null, null), environment.newChain().fill(
                environment.newAt(user),
                environment.newText(" 欢迎来到本群，要使用Koishi bot可以at或私聊输入~help查看帮助")
        ));
    }

    private void onGroupRecall(Triple<GroupInfo, UserInfo, Long> info) {
        environment.getMessageSender().onRecall(info.component1(), info.component2(), info.component3());
    }
}
