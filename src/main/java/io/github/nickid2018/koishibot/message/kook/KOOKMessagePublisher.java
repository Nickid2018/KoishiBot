package io.github.nickid2018.koishibot.message.kook;

import io.github.kookybot.events.EventHandler;
import io.github.kookybot.events.Listener;
import io.github.kookybot.events.MessageEvent;
import io.github.kookybot.events.channel.ChannelMessageEvent;
import io.github.nickid2018.koishibot.message.api.ChainMessage;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.MessageEventPublisher;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import kotlin.Pair;
import kotlin.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class KOOKMessagePublisher implements MessageEventPublisher {

    private final KOOKEnvironment environment;

    public KOOKMessagePublisher(KOOKEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void subscribeGroupMessage(BiConsumer<Triple<GroupInfo, UserInfo, ChainMessage>, Long> consumer) {
        environment.getKookClient().getEventManager().addClassListener(new ChannelMessageListener(environment, consumer));
    }

    public static class ChannelMessageListener implements Listener {

        private static final Logger LOGGER = LoggerFactory.getLogger("KOOK Test");

        private final KOOKEnvironment environment;
        private final BiConsumer<Triple<GroupInfo, UserInfo, ChainMessage>, Long> consumer;

        public ChannelMessageListener(KOOKEnvironment environment, BiConsumer<Triple<GroupInfo, UserInfo, ChainMessage>, Long> consumer) {
            this.environment = environment;
            this.consumer = consumer;
        }

        @EventHandler
        public void onChannelMessage(ChannelMessageEvent event) {
            KOOKTextChannel groupInfo = new KOOKTextChannel(environment, event.getChannel());
            KOOKUser userInfo = new KOOKUser(environment, event.getSender());

            if (event.getEventType() == MessageEvent.EventType.PLAIN_TEXT ||
                    event.getEventType() == MessageEvent.EventType.MARKDOWN) {
                KOOKChain chain = new KOOKChain(environment, KOOKMessageData.fromChannelMessage(event));
                consumer.accept(new Triple<>(groupInfo, userInfo, chain), Long.valueOf(event.getTimestamp()));
            }
        }
    }

    @Override
    public void subscribeFriendMessage(BiConsumer<Pair<UserInfo, ChainMessage>, Long> consumer) {
        // Unsupported
    }

    @Override
    public void subscribeGroupTempMessage(BiConsumer<Pair<UserInfo, ChainMessage>, Long> consumer) {
        // Unsupported
    }

    @Override
    public void subscribeStrangerMessage(BiConsumer<Pair<UserInfo, ChainMessage>, Long> consumer) {
        // Unsupported
    }

    @Override
    public void subscribeNewMemberAdd(BiConsumer<GroupInfo, UserInfo> consumer) {
        // Unsupported
    }

    @Override
    public void subscribeGroupRecall(Consumer<Triple<GroupInfo, UserInfo, Long>> consumer) {
        // Unsupported
    }

    @Override
    public void subscribeFriendRecall(BiConsumer<UserInfo, Long> consumer) {
        // Unsupported
    }
}
