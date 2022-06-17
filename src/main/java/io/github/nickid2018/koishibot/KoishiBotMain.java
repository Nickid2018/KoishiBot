package io.github.nickid2018.koishibot;

import io.github.nickid2018.koishibot.core.*;
import io.github.nickid2018.koishibot.message.MessageManager;
import io.github.nickid2018.koishibot.message.qq.QQEnvironment;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.console.command.CommandManager;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.ListeningStatus;
import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageUtils;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.utils.BotConfiguration;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public final class KoishiBotMain extends JavaPlugin {

    // Singleton
    public static final KoishiBotMain INSTANCE = new KoishiBotMain();

    public File workingDir = new File(".");
    public File tmpDir;

    public Bot botKoishi;
    public ExecutorService executor;
    public long startTime = System.currentTimeMillis();

    private QQEnvironment environment;
    private MessageManager manager;

    private KoishiBotMain() {
        super(new JvmPluginDescriptionBuilder("io.github.nickid2018.koishibot", "1.0-SNAPSHOT").build());
        tmpDir = new File(getDataFolder(), "tmp");
    }

    @Override
    public void onEnable() {
        try {
            Settings.load();
        } catch (IOException e) {
            e.printStackTrace();
            botKoishi = null;
            return;
        }
        botKoishi = BotFactory.INSTANCE.newBot(Settings.BOT_QQ, Settings.BOT_PASSWORD, new BotConfiguration() {{
            setHeartbeatStrategy(HeartbeatStrategy.STAT_HB);
            if (System.getProperty("IDE") != null) {
                enableContactCache();
                setWorkingDir(workingDir = new File("D:/KoishiBot/bots/753836054"));
            } else {
                setWorkingDir(workingDir = new File("C:/Koishi Bot/botKoishi"));
            }
            fileBasedDeviceInfo("device.json");
        }});
        CommandManager.INSTANCE.registerCommand(new BotKoishiCommand(this), true);
        botKoishi.login();
        executor = Executors.newCachedThreadPool(new BasicThreadFactory.Builder().uncaughtExceptionHandler(
                (th, t) -> ErrorRecord.enqueueError("concurrent", t)
        ).build());
        environment = new QQEnvironment(botKoishi);
        manager = new MessageManager(environment);
    }

    @Override
    public void onDisable() {
        if (botKoishi == null)
            return;
        botKoishi.close();
        executor.shutdown();
        executor = null;
        TempFileSystem.onDisable();
    }
}