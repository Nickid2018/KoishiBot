package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.network.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class TelegramEnvironment extends Environment {

    public static final Logger LOGGER = LoggerFactory.getLogger("Telegram Environment");

    private final DefaultBotSession session;
    private final TelegramBot bot;
    private final TelegramUser me;

    public TelegramEnvironment(Connection connection, TelegramBot bot, DefaultBotSession session) throws TelegramApiException {
        super(connection);
        this.session = session;
        this.bot = bot;
        me = new TelegramUser(this, bot.getMe());

        botID = me.userId;
        forwardMessageSupported = false;
        audioSupported = true;
        audioToFriendSupported = true;
        quoteSupported = true;
        environmentName = "Telegram";
        environmentUserPrefix = "tg.user";
        needAntiFilter = false;
        audioSilk = false;
    }

    @Override
    public UserInfo getUser(String id, boolean isStranger) {
        return TelegramUser.USER_CACHE.get(id);
    }

    public Chat getChat(String id) {
        if (!id.startsWith("tg.group"))
            return null;
        GetChat chat = new GetChat();
        chat.setChatId(id.substring(8));
        try {
            return bot.execute(chat);
        } catch (TelegramApiException e) {
            return null;
        }
    }

    @Override
    public GroupInfo getGroup(String id) {
        Chat chat = getChat(id);
        return chat == null ? null : new TelegramGroup(this, chat);
    }

    public TelegramBot getBot() {
        return bot;
    }

    @Override
    public void close() {
//        if (System.getProperty("env.tgstop").equals("true") && session != null)
//            session.stop();
    }
}
