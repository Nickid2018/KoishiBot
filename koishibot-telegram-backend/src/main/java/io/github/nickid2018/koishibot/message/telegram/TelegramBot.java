package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.message.event.OnFriendMessageEvent;
import io.github.nickid2018.koishibot.message.event.OnGroupMessageEvent;
import io.github.nickid2018.koishibot.message.event.OnMemberAddEvent;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.function.Supplier;

public class TelegramBot extends TelegramLongPollingBot {

    private final Supplier<TelegramEnvironment> environment;

    private final String userName;
    private final String token;


    public TelegramBot(String userName, String token, DefaultBotOptions options, Supplier<TelegramEnvironment> environment) {
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
        TelegramEnvironment environment = this.environment.get();
        if (environment == null)
            return;
        if (update.hasChatJoinRequest()) {
            OnMemberAddEvent event = new OnMemberAddEvent(environment);
            event.group = new TelegramGroup(environment, update.getChatJoinRequest().getChat());
            event.user = new TelegramUser(environment, update.getChatJoinRequest().getUser());
            environment.getConnection().sendPacket(event);
        } else if (update.hasMessage()) {
            if (update.getMessage().getFrom() != null
                    && (update.getMessage().isGroupMessage()
                    || update.getMessage().isSuperGroupMessage()
                    || update.getMessage().isChannelMessage())) {
                TelegramMessageData data = TelegramMessageData.fromMessage(update.getMessage());
                OnGroupMessageEvent event = new OnGroupMessageEvent(environment);
                event.group = new TelegramGroup(environment, update.getMessage().getChat());
                event.user = new TelegramUser(environment, update.getMessage().getFrom());
                event.message = new TelegramChain(environment, data);
                event.time = update.getMessage().getDate();
                environment.getConnection().sendPacket(event);
            } else if (update.getMessage().isUserMessage()) {
                TelegramMessageData data = TelegramMessageData.fromMessage(update.getMessage());
                OnFriendMessageEvent event = new OnFriendMessageEvent(environment);
                event.user = new TelegramUser(environment, update.getMessage().getFrom());
                event.message = new TelegramChain(environment, data);
                event.time = update.getMessage().getDate();
                environment.getConnection().sendPacket(event);
            }
        }
    }
}
