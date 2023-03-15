package io.github.nickid2018.koishibot.backend;

import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.utils.BotConfiguration;
import net.mamoe.mirai.utils.LoggerAdapters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    public static final Logger LOGGER = LoggerFactory.getLogger("KoishiBot QQ Backend");

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

        if (!bot.isOnline()) {
            LOGGER.error("Failed to login.");
            return;
        }


    }
}
