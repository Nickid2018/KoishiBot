package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.message.api.ChainMessage;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.MessageEventPublisher;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import kotlin.Pair;
import kotlin.Triple;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TelegramBot extends TelegramLongPollingBot implements MessageEventPublisher {

    private final TelegramEnvironment environment;

    private final String userName;
    private final String token;

    private BiConsumer<Triple<GroupInfo, UserInfo, ChainMessage>, Long> groupMessageConsumer;
    private BiConsumer<Pair<UserInfo, ChainMessage>, Long> friendMessageConsumer;
    private BiConsumer<GroupInfo, UserInfo> groupMemberJoinConsumer;
    private BiConsumer<UserInfo, Long> friendRecallConsumer;


    public TelegramBot(String userName, String token, DefaultBotOptions options, TelegramEnvironment environment) {
        super(options);
        this.userName = userName;
        this.token = token;
        this.environment = environment;
    }

    @Override
    public String getBotUsername() {
        return userName;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasChatJoinRequest()) {
            if (groupMemberJoinConsumer != null) {
                groupMemberJoinConsumer.accept(
                        new TelegramGroup(environment, update.getChatJoinRequest().getChat()),
                        new TelegramUser(environment, update.getChatJoinRequest().getUser())
                );
            }
        } else if (update.hasMessage()) {
            if (groupMessageConsumer != null && (update.getMessage().isGroupMessage()
                    || update.getMessage().isSuperGroupMessage()
                    || update.getMessage().isChannelMessage())) {
                TelegramMessageData data = TelegramMessageData.fromMessage(update.getMessage());
                groupMessageConsumer.accept(
                        new Triple<>(
                                new TelegramGroup(environment, update.getMessage().getChat()),
                                new TelegramUser(environment, update.getMessage().getFrom()),
                                new TelegramChain(environment, data)
                        ),
                        Long.valueOf(update.getMessage().getDate())
                );
            } else if (friendMessageConsumer != null && update.getMessage().isUserMessage()) {
                TelegramMessageData data = TelegramMessageData.fromMessage(update.getMessage());
                friendMessageConsumer.accept(
                        new Pair<>(
                                new TelegramUser(environment, update.getMessage().getFrom()),
                                new TelegramChain(environment, data)
                        ),
                        Long.valueOf(update.getMessage().getDate())
                );
            }
        }
    }

    @Override
    public void subscribeGroupMessage(BiConsumer<Triple<GroupInfo, UserInfo, ChainMessage>, Long> consumer) {
        groupMessageConsumer = consumer;
    }

    @Override
    public void subscribeFriendMessage(BiConsumer<Pair<UserInfo, ChainMessage>, Long> consumer) {
        friendMessageConsumer = consumer;
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
        groupMemberJoinConsumer = consumer;
    }

    @Override
    public void subscribeGroupRecall(Consumer<Triple<GroupInfo, UserInfo, Long>> consumer) {
    }

    @Override
    public void subscribeFriendRecall(BiConsumer<UserInfo, Long> consumer) {
        friendRecallConsumer = consumer;
    }
}
