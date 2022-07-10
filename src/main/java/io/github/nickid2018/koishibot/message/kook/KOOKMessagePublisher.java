package io.github.nickid2018.koishibot.message.kook;

import io.github.nickid2018.koishibot.message.api.ChainMessage;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.MessageEventPublisher;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.util.ReflectTarget;
import io.github.zly2006.kookybot.events.EventHandler;
import io.github.zly2006.kookybot.events.Listener;
import io.github.zly2006.kookybot.events.MessageEvent;
import io.github.zly2006.kookybot.events.channel.ChannelMessageEvent;
import io.github.zly2006.kookybot.message.ImageMessage;
import io.github.zly2006.kookybot.message.MarkdownMessage;
import kotlin.Pair;
import kotlin.Triple;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class KOOKMessagePublisher implements MessageEventPublisher {

    private final KOOKEnvironment environment;

    public KOOKMessagePublisher(KOOKEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void subscribeGroupMessage(BiConsumer<Triple<GroupInfo, UserInfo, ChainMessage>, Long> consumer) {
        environment.getKookClient().getEventManager().addClassListener(new Listener() {
            @EventHandler
            @ReflectTarget
            public void onChannelMessage(ChannelMessageEvent event) {
                GroupInfo groupInfo = new KOOKTextChannel(event.getChannel());
                UserInfo userInfo = new KOOKUser(event.getSender(), true);
                ChainMessage message = environment.newChain();
                if (event.getEventType() == MessageEvent.EventType.PLAIN_TEXT ||
                        event.getEventType() == MessageEvent.EventType.MARKDOWN)
                    message.fillChain(new KOOKText(environment,
                            new MarkdownMessage(environment.getKookClient(), event.getContent())));
                else if (event.getEventType() == MessageEvent.EventType.IMAGE)
                    message.fillChain(new KOOKImage(environment,
                            new ImageMessage(environment.getKookClient(), null, event.getContent(), null)));
                consumer.accept(new Triple<>(groupInfo, userInfo, message), Long.valueOf(event.getTimestamp()));
            }
        });
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
