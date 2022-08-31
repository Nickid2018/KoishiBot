package io.github.nickid2018.koishibot.message.kook;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.github.kookybot.events.EventHandler;
import io.github.kookybot.events.Listener;
import io.github.kookybot.events.MessageEvent;
import io.github.kookybot.events.channel.ChannelMessageEvent;
import io.github.kookybot.message.AtKt;
import io.github.kookybot.message.Message;
import io.github.kookybot.message.MessageComponent;
import io.github.nickid2018.koishibot.message.api.ChainMessage;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.MessageEventPublisher;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.util.value.Either;
import kotlin.Pair;
import kotlin.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

        private final KOOKEnvironment environment;
        private final BiConsumer<Triple<GroupInfo, UserInfo, ChainMessage>, Long> consumer;

        public ChannelMessageListener(KOOKEnvironment environment, BiConsumer<Triple<GroupInfo, UserInfo, ChainMessage>, Long> consumer) {
            this.environment = environment;
            this.consumer = consumer;
        }

        @EventHandler
        @SuppressWarnings("unchecked")
        public void onChannelMessage(ChannelMessageEvent event) {
            KOOKTextChannel groupInfo = new KOOKTextChannel(environment, event.getChannel());
            KOOKUser userInfo = new KOOKUser(environment, event.getSender(), true);
            List<Either<Message, MessageComponent>> messages = new ArrayList<>();

            // Ats
            if (event.getEventType() == MessageEvent.EventType.MARKDOWN ||
                    event.getEventType() == MessageEvent.EventType.PLAIN_TEXT) {
                JsonArray mentions = event.getExtra().getAsJsonArray("mention");
                for (JsonElement element : mentions)
                    messages.add(Either.right(AtKt.At(Objects.requireNonNull(
                            event.getChannel().getGuild().getGuildUser(element.getAsString())))));
            }



//            ChainMessage message = environment.newChain();
//            if (event.getEventType() == MessageEvent.EventType.PLAIN_TEXT ||
//                    event.getEventType() == MessageEvent.EventType.MARKDOWN)
//                message.fillChain(new KOOKText(environment,
//                        new MarkdownMessage(environment.getKookClient(), event.getContent())));
//            else if (event.getEventType() == MessageEvent.EventType.IMAGE)
//                message.fillChain(new KOOKImage(environment,
//                        new ImageMessage(environment.getKookClient(), null, event.getContent(), null)));

            KOOKChain chain = new KOOKChain(environment, messages.toArray(Either[]::new),
                    new KOOKMessageSource(event.getMessageId(), groupInfo, userInfo));
            consumer.accept(new Triple<>(groupInfo, userInfo, chain), Long.valueOf(event.getTimestamp()));
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
