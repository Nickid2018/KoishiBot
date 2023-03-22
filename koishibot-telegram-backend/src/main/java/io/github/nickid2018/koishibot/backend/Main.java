package io.github.nickid2018.koishibot.backend;

import io.github.nickid2018.koishibot.message.telegram.TelegramBot;
import io.github.nickid2018.koishibot.message.telegram.TelegramEnvironment;
import io.github.nickid2018.koishibot.network.Connection;
import io.github.nickid2018.koishibot.util.LazyLoadedValue;
import io.github.nickid2018.koishibot.util.LogUtils;
import org.apache.http.client.config.RequestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    public static final Logger LOGGER = LoggerFactory.getLogger("KoishiBot Telegram Backend");

    public static AtomicBoolean stopped = new AtomicBoolean();
    public static TelegramEnvironment environment;

    public static void main(String[] args) {
        try {
            Settings.loadSettings();
        } catch (Exception e) {
            LOGGER.error("Failed to load settings.", e);
            return;
        }

        DefaultBotOptions botOptions = new DefaultBotOptions();
        RequestConfig.Builder builder = RequestConfig.custom();
        builder.setConnectTimeout(10000);
        builder.setConnectionRequestTimeout(10000);
        builder.setSocketTimeout(10000);
        builder.setCircularRedirectsAllowed(false);
        builder.setContentCompressionEnabled(true);
        botOptions.setRequestConfig(builder.build());
        if (Settings.proxyHost != null) {
            botOptions.setProxyHost(Settings.proxyHost);
            botOptions.setProxyPort(Settings.proxyPort);
            botOptions.setProxyType(Settings.proxyType.equals("http") ?
                    DefaultBotOptions.ProxyType.HTTP : DefaultBotOptions.ProxyType.SOCKS5);
        }

        TelegramBot bot = new TelegramBot(Settings.uid, Settings.token, botOptions, () -> environment);
        TelegramBotsApi botsApi;
        DefaultBotSession session;
        try {
            botsApi = new TelegramBotsApi(DefaultBotSession.class);
            session = (DefaultBotSession) botsApi.registerBot(bot);
        } catch (TelegramApiException e) {
            LogUtils.error(LOGGER, "Failed to register bot.", e);
            return;
        }

        int retry = 0;
        while (!stopped.get() && retry < 20) {
            CompletableFuture<Void> disconnected = new CompletableFuture<>();
            CompletableFuture<TelegramEnvironment> env = new CompletableFuture<>();
            LazyLoadedValue<TelegramEnvironment> lazyLoadedValue = new LazyLoadedValue<>(env::join);
            BackendDataListener listener = new BackendDataListener(lazyLoadedValue::get, disconnected);
            try {
                Connection connection = Connection.connectToTcpServer(
                        listener.getRegistry(), listener, InetAddress.getLocalHost(), Settings.delegatePort);
                environment = new TelegramEnvironment(connection, bot, session);
                env.complete(environment);
                retry = 0;
                disconnected.get();
            } catch (Exception e) {
                LOGGER.error("Failed to link.", e);
            }
            if (stopped.get())
                System.exit(0);
            LOGGER.info("Disconnected. Waiting 1min to reconnect.");
            retry++;
            try {
                Thread.sleep(60000);
            } catch (InterruptedException ignored) {
            }
        }

        LOGGER.error(retry == 20 ? "Retries > 20, can't link to delegate. Shutting down." : "Bot offline. Shutting down.");
        session.stop();
    }
}
