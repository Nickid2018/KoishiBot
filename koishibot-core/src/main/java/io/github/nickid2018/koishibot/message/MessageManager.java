package io.github.nickid2018.koishibot.message;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.filter.MCChatBridgeFilter;
import io.github.nickid2018.koishibot.filter.PreFilter;
import io.github.nickid2018.koishibot.filter.RequestFrequencyFilter;
import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.module.ModuleManager;
import io.github.nickid2018.koishibot.permission.PermissionManager;
import io.github.nickid2018.koishibot.permission.PermissionResolver;
import io.github.nickid2018.koishibot.resolver.BilibiliDataResolver;
import io.github.nickid2018.koishibot.util.Either;
import io.github.nickid2018.koishibot.util.value.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;

public class MessageManager {

    private final DelegateEnvironment environment;

    public static final Logger LOGGER = LoggerFactory.getLogger("Message Manager");

    public static final List<MessageResolver> RESOLVERS = new ArrayList<>();
    public static final Map<String, JSONServiceResolver> JSON_SERVICE_MAP = new HashMap<>();

    public static final List<PreFilter> GROUP_PREFILTER = new ArrayList<>();
    public static final List<PreFilter> FRIEND_PREFILTER = new ArrayList<>();
    public static final List<PreFilter> STRANGER_PREFILTER = new ArrayList<>();

    static {
        RESOLVERS.add(new PermissionResolver());

        JSON_SERVICE_MAP.put("哔哩哔哩", new BilibiliDataResolver());

        RequestFrequencyFilter frequencyFilter = new RequestFrequencyFilter();

        GROUP_PREFILTER.add(frequencyFilter);
        FRIEND_PREFILTER.add(frequencyFilter);
        STRANGER_PREFILTER.add(frequencyFilter);
        GROUP_PREFILTER.add(new MCChatBridgeFilter());
    }

    public MessageManager(DelegateEnvironment environment) {
        this.environment = environment;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void onGroupMessage(GroupInfo group, UserInfo user, ChainMessage message, long sentTime) {
        dealMessage(group, user, message, GROUP_PREFILTER, MessageResolver::groupEnabled, true, sentTime);
    }

    public void onFriendMessage(UserInfo user, ChainMessage message, long sentTime) {
        dealMessage(null, user, message, FRIEND_PREFILTER, MessageResolver::friendEnabled, false, sentTime);
    }

    public void onStrangerMessage(UserInfo user, ChainMessage message, long sentTime) {
        dealMessage(null, user, message, STRANGER_PREFILTER, MessageResolver::strangerChat, false, sentTime);
    }

    private void dealMessage(GroupInfo group, UserInfo user, ChainMessage message, List<PreFilter> preFilters,
                             Predicate<MessageResolver> predicate, boolean inGroup, long sentTime) {
        MessageContext context = new MessageContext(environment, group, user, message, sentTime);

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

    public void onMemberAdd(GroupInfo group, UserInfo user) {
        if (RequestFrequencyFilter.shouldNotResponse(user, new MutableBoolean(false)))
            return;
        if (ModuleManager.isOpened(group.getGroupId(), "interact")) {
            user.nudge(group);
            environment.getMessageSender().sendMessage(
                    new MessageContext(environment, group, user, environment.newChain(), -1), environment.newChain(
                            environment.newAt(user),
                            environment.newText(" 欢迎来到本群。使用机器人时@Koishi bot（可群发或私聊）并输入“~help”查看帮助。")
                    ));
        }
    }

    public void onGroupRecall(GroupInfo group, UserInfo user, long time) {
        environment.getMessageSender().onRecall(group, user, time);
    }
}
