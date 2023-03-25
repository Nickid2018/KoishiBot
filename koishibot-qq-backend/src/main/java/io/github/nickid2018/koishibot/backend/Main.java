package io.github.nickid2018.koishibot.backend;

import io.github.nickid2018.koishibot.message.qq.QQEnvironment;
import io.github.nickid2018.koishibot.network.Connection;
import io.github.nickid2018.koishibot.util.LazyLoadedValue;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.utils.BotConfiguration;
import net.mamoe.mirai.utils.LoggerAdapters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    public static final Logger LOGGER = LoggerFactory.getLogger("KoishiBot QQ Backend");

    public static AtomicBoolean stopped = new AtomicBoolean();
    public static QQEnvironment environment;

    public static void main(String[] args) {
        try {
            Settings.loadSettings();
        } catch (Exception e) {
            LOGGER.error("Failed to load settings.", e);
            return;
        }

        AtomicBoolean nudgeEnabled = new AtomicBoolean();
        Bot bot = BotFactory.INSTANCE.newBot(Settings.id, Settings.password, new BotConfiguration() {{
            setHeartbeatStrategy(HeartbeatStrategy.STAT_HB);
            setWorkingDir(new File("qq"));
            fileBasedDeviceInfo();
            setBotLoggerSupplier(bot -> LoggerAdapters.asMiraiLogger(LoggerFactory.getLogger("Mirai Bot")));
            setNetworkLoggerSupplier(bot -> LoggerAdapters.asMiraiLogger(LoggerFactory.getLogger("Mirai Net")));
            if (Settings.protocol != null) {
                MiraiProtocol miraiProtocol = MiraiProtocol.valueOf(Settings.protocol.toUpperCase());
                setProtocol(miraiProtocol);
                nudgeEnabled.set(miraiProtocol == MiraiProtocol.ANDROID_PHONE || miraiProtocol == MiraiProtocol.IPAD);
            } else
                nudgeEnabled.set(true);
        }});
        bot.login();

        int retry = 0;
        while (!stopped.get() && bot.isOnline() && retry < 100) {
            CompletableFuture<Void> disconnected = new CompletableFuture<>();
            CompletableFuture<QQEnvironment> env = new CompletableFuture<>();
            LazyLoadedValue<QQEnvironment> lazyLoadedValue = new LazyLoadedValue<>(env::join);
            BackendDataListener listener = new BackendDataListener(lazyLoadedValue::get, disconnected);
            try {
                Connection connection = Connection.connectToTcpServer(
                        listener.getRegistry(), listener, InetAddress.getLocalHost(), Settings.delegatePort);
                environment = new QQEnvironment(bot, nudgeEnabled.get(), connection);
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
                Thread.sleep(10000);
            } catch (InterruptedException ignored) {
            }
        }

        LOGGER.error(retry == 100 ? "Retries > 100, can't link to delegate. Shutting down." : "Bot offline. Shutting down.");
        bot.close();
    }
}
