package io.github.nickid2018.koishibot;

import io.github.nickid2018.koishibot.core.BotKoishiCommand;
import io.github.nickid2018.koishibot.core.Settings;
import io.github.nickid2018.koishibot.core.TempFileSystem;
import io.github.nickid2018.koishibot.github.GitHubListener;
import io.github.nickid2018.koishibot.message.Environments;
import io.github.nickid2018.koishibot.message.MemberFilter;
import io.github.nickid2018.koishibot.message.qq.QQEnvironment;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.webhook.WebHookManager;
import io.github.nickid2018.koishibot.wiki.InfoBoxShooter;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.console.command.CommandManager;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.utils.BotConfiguration;

import java.io.File;
import java.io.IOException;

public final class KoishiBotMain extends JavaPlugin {

    // Singleton
    public static final KoishiBotMain INSTANCE = new KoishiBotMain();

    public File workingDir = new File(".");
    public File tmpDir;

    public Bot botKoishi;
    public long startTime = System.currentTimeMillis();

    public QQEnvironment environment;

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
        environment = new QQEnvironment(botKoishi);
        AsyncUtil.start();
        Environments.putEnvironment("qq", environment);
        MemberFilter.init();
        GitHubListener.clinit();
    }

    @Override
    public void onDisable() {
        if (botKoishi == null)
            return;
        botKoishi.close();
        AsyncUtil.terminate();
        TempFileSystem.onDisable();
        InfoBoxShooter.close();
        WebHookManager.stop();
    }
}