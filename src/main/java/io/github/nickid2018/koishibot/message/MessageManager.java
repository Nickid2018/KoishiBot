package io.github.nickid2018.koishibot.message;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.filter.MCChatBridgeFilter;
import io.github.nickid2018.koishibot.filter.PreFilter;
import io.github.nickid2018.koishibot.filter.RequestFrequencyFilter;
import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.module.ModuleManager;
import io.github.nickid2018.koishibot.permission.PermissionManager;
import io.github.nickid2018.koishibot.permission.PermissionResolver;
import io.github.nickid2018.koishibot.resolver.*;
import io.github.nickid2018.koishibot.util.value.Either;
import io.github.nickid2018.koishibot.util.value.MutableBoolean;
import kotlin.Pair;
import kotlin.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class MessageManager {

    private final Environment environment;

    public static final Logger LOGGER = LoggerFactory.getLogger("Message Manager");

    public static final List<MessageResolver> RESOLVERS = new ArrayList<>();
    public static final Map<String, JSONServiceResolver> JSON_SERVICE_MAP = new HashMap<>();

    public static final List<PreFilter> GROUP_PREFILTER = new ArrayList<>();
    public static final List<PreFilter> FRIEND_PREFILTER = new ArrayList<>();
    public static final List<PreFilter> TEMP_PREFILTER = new ArrayList<>();
    public static final List<PreFilter> STRANGER_PREFILTER = new ArrayList<>();

    static {
        RESOLVERS.add(new PermissionResolver());

        JSON_SERVICE_MAP.put("哔哩哔哩", new BilibiliDataResolver());

        RequestFrequencyFilter frequencyFilter = new RequestFrequencyFilter();

        GROUP_PREFILTER.add(frequencyFilter);
        FRIEND_PREFILTER.add(frequencyFilter);
        TEMP_PREFILTER.add(frequencyFilter);
        STRANGER_PREFILTER.add(frequencyFilter);
        GROUP_PREFILTER.add(new MCChatBridgeFilter());
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
                GROUP_PREFILTER, MessageResolver::groupEnabled, true, sentTime);
    }

    private void onFriendMessage(Pair<UserInfo, ChainMessage> message, long sentTime) {
        dealMessage(null, message.component1(), message.component2(),
                FRIEND_PREFILTER, MessageResolver::friendEnabled, false, sentTime);
    }

    private void onTempMessage(Pair<UserInfo, ChainMessage> message, long sentTime) {
        dealMessage(null, message.component1(), message.component2(),
                TEMP_PREFILTER, MessageResolver::groupTempChat, false, sentTime);
    }

    private void onStrangerMessage(Pair<UserInfo, ChainMessage> message, long sentTime) {
        dealMessage(null, message.component1(), message.component2(),
                STRANGER_PREFILTER, MessageResolver::strangerChat, false, sentTime);
    }

    private void dealMessage(GroupInfo group, UserInfo user, ChainMessage message, List<PreFilter> preFilters,
                             Predicate<MessageResolver> predicate, boolean inGroup, long sentTime) {
        MessageContext context = new MessageContext(group, user, message, sentTime);

        for (PreFilter filter : preFilters) {
            message = filter.filterMessagePre(message, context, environment);
            if (message == null)
                return;
        }

        UserAwaitData.onMessage(group, user, message);

        List<String> strings = new ArrayList<>();
        ServiceMessage service = null;
        boolean att = false;
        QuoteMessage replyMe = null;

        for (AbstractMessage content : message.getMessages()) {
            if (content instanceof TextMessage text)
                strings.add(text.getText());
            if (content instanceof AtMessage at)
                if (at.getId().equals(environment.getBotId()))
                    att = true;
            if (content instanceof QuoteMessage quote)
                if (quote.getReplyToID().equals(environment.getBotId()))
                    replyMe = quote;
            if (content instanceof ServiceMessage serviceMessage)
                service = serviceMessage;
        }

        if (replyMe != null)
            MessageReplyData.onMessage(group, user, replyMe, message);

        if (service == null) {
            if (strings.isEmpty() || strings.get(0).startsWith("!"))
                return;

            MutableBoolean bool = new MutableBoolean(false);
            boolean finalAtt = att;

            ModuleManager.getAvailableResolvers(context.getSendDest()).stream()
                    .filter(predicate)
                    .filter(resolver -> !inGroup || !resolver.needAt() || finalAtt)
                    .filter(resolver -> PermissionManager.getLevel(user.getUserId()).levelGreaterOrEquals(resolver.getPermissionLevel()))
                    .forEach(resolver -> strings.forEach(string -> {
                        if (!bool.getValue() && resolver.resolve(string, context, environment)) {
                            bool.setValue(true);
                            LOGGER.info("Command detected: {}.", resolver.getClass().getName());
                        }
                    }));
            if (!bool.getValue() && att && inGroup && replyMe == null &&
                    ModuleManager.isOpened(group.getGroupId(), "interact")) {
                environment.getMessageSender().sendMessage(context, environment.newChain(
                        environment.newQuote(message),
                        environment.newText("不要乱@人，会被514打电话的。")
                ));
            }
        } else {
            String name = service.getName();
            Either<JsonObject, String> data = service.getData();
            if (data.isLeft() && JSON_SERVICE_MAP.containsKey(name))
                JSON_SERVICE_MAP.get(name).resolveService(data.left(), context, environment);
        }
    }

    private void onMemberAdd(GroupInfo group, UserInfo user) {
        if (RequestFrequencyFilter.shouldNotResponse(user, new MutableBoolean(false)))
            return;
        user.nudge(group);
        if (ModuleManager.isOpened(group.getGroupId(), "interact"))
            environment.getMessageSender().sendMessage(
                    new MessageContext(group, user, environment.chain().fillChain(), -1), environment.chain().fillChain(
                            environment.newAt(group, user),
                            environment.newText(" 欢迎来到本群，要使用Koishi bot可以at或私聊输入~help查看帮助")
                    ));
    }

    private void onGroupRecall(Triple<GroupInfo, UserInfo, Long> info) {
        environment.getMessageSender().onRecall(info.component1(), info.component2(), info.component3());
    }
}
