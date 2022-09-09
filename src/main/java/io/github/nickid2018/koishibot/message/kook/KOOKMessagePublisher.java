package io.github.nickid2018.koishibot.message.kook;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.github.kookybot.events.EventHandler;
import io.github.kookybot.events.Listener;
import io.github.kookybot.events.channel.ChannelMessageEvent;
import io.github.kookybot.message.AtKt;
import io.github.kookybot.message.Message;
import io.github.kookybot.message.MessageComponent;
import io.github.nickid2018.koishibot.message.api.ChainMessage;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.MessageEventPublisher;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.value.Either;
import kotlin.Pair;
import kotlin.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        private static final Logger LOGGER = LoggerFactory.getLogger("KOOK Test");

        private final KOOKEnvironment environment;
        private final BiConsumer<Triple<GroupInfo, UserInfo, ChainMessage>, Long> consumer;

        public ChannelMessageListener(KOOKEnvironment environment, BiConsumer<Triple<GroupInfo, UserInfo, ChainMessage>, Long> consumer) {
            this.environment = environment;
            this.consumer = consumer;
        }

        @EventHandler
        @SuppressWarnings("unchecked")
        public void onChannelMessage(ChannelMessageEvent event) {
            LOGGER.info(event.getContent());

            KOOKTextChannel groupInfo = new KOOKTextChannel(environment, event.getChannel());
            KOOKUser userInfo = new KOOKUser(environment, event.getSender(), true);
            List<Either<Message, MessageComponent>> messages = new ArrayList<>();

            // Ats
            JsonUtil.getData(event.getExtra(), "mention", JsonArray.class).ifPresent(mentions -> {
                for (JsonElement element : mentions)
                    messages.add(Either.right(AtKt.At(Objects.requireNonNull(
                            event.getChannel().getGuild().getGuildUser(element.getAsString())))));
            });

            String text = event.getContent();

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
