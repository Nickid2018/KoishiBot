package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.core.BotLoginData;
import io.github.nickid2018.koishibot.core.Settings;
import io.github.nickid2018.koishibot.message.MessageManager;
import io.github.nickid2018.koishibot.message.MessageSender;
import io.github.nickid2018.koishibot.message.api.*;
import org.apache.http.client.config.RequestConfig;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class TelegramEnvironment implements Environment {

    private final DefaultBotSession session;
    private final TelegramBot bot;
    private final TelegramUser me;

    public TelegramEnvironment(BotLoginData data) throws TelegramApiException {
        DefaultBotOptions botOptions = new DefaultBotOptions();
        RequestConfig.Builder builder = RequestConfig.custom();
        builder.setConnectTimeout(10000);
        builder.setConnectionRequestTimeout(10000);
        builder.setSocketTimeout(10000);
        builder.setCircularRedirectsAllowed(false);
        builder.setContentCompressionEnabled(true);
        botOptions.setRequestConfig(builder.build());
        if (Settings.PROXY_TYPE == null) {
            botOptions.setProxyHost(Settings.PROXY_HOST);
            botOptions.setProxyPort(Settings.PROXY_PORT);
            botOptions.setProxyType(Settings.PROXY_TYPE.equals("http") ?
                    DefaultBotOptions.ProxyType.HTTP :DefaultBotOptions.ProxyType.SOCKS5);
        }
        bot = new TelegramBot(data.uid(), data.token(), botOptions, this);
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        session = (DefaultBotSession) botsApi.registerBot(bot);
        me = new TelegramUser(this, bot.getMe());
    }

    @Override
    public AtMessage at() {
        return null;
    }

    @Override
    public ChainMessage chain() {
        return null;
    }

    @Override
    public TextMessage text() {
        return null;
    }

    @Override
    public AudioMessage audio() {
        return null;
    }

    @Override
    public ImageMessage image() {
        return null;
    }

    @Override
    public ForwardMessage forwards() {
        return null;
    }

    @Override
    public MessageEntry messageEntry() {
        return null;
    }

    @Override
    public QuoteMessage quote() {
        return null;
    }

    @Override
    public ServiceMessage service() {
        return null;
    }

    @Override
    public UserInfo getUser(String id, boolean isStranger) {
        // Because API doesn't support getting user info, so we can only get the user info from the message.
        return me;
    }

    @Override
    public GroupInfo getGroup(String id) {
        if (!id.startsWith("tg.group"))
            return null;
        GetChat chat = new GetChat();
        chat.setChatId(id.substring(8));
        try {
            return new TelegramGroup(this, bot.execute(chat));
        } catch (TelegramApiException e) {
            return null;
        }
    }

    @Override
    public String getBotId() {
        return me.getUserId();
    }

    @Override
    public MessageEventPublisher getEvents() {
        return bot;
    }

    @Override
    public MessageSender getMessageSender() {
        return null;
    }

    @Override
    public MessageManager getManager() {
        return null;
    }

    @Override
    public boolean forwardMessageSupported() {
        return true;
    }

    @Override
    public boolean audioSupported() {
        return false;
    }

    @Override
    public boolean quoteSupported() {
        return true;
    }

    @Override
    public String getEnvironmentName() {
        return "Telegram";
    }

    @Override
    public String getEnvironmentUserPrefix() {
        return "tg.user";
    }

    public TelegramBot getBot() {
        return bot;
    }

    @Override
    public void close() {
    }
}
