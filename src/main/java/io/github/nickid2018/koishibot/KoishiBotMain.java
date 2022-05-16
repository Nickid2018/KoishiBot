package io.github.nickid2018.koishibot;

import io.github.nickid2018.koishibot.core.*;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class KoishiBotMain extends JavaPlugin {

    // Singleton
    public static final KoishiBotMain INSTANCE = new KoishiBotMain();

    public File workingDir = new File(".");
    public Bot botKoishi;
    public ExecutorService executor;

    private KoishiBotMain() {
        super(new JvmPluginDescriptionBuilder("io.github.nickid2018.koishibot", "1.0-SNAPSHOT").build());
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
        registerEvents();
        CommandManager.INSTANCE.registerCommand(new BotKoishiCommand(this), true);
        botKoishi.login();
        executor = Executors.newCachedThreadPool(new BasicThreadFactory.Builder().uncaughtExceptionHandler(
                (th, t) -> ErrorRecord.enqueueError("concurrent", t)
        ).build());
    }

    @Override
    public void onDisable() {
        if (botKoishi == null)
            return;
        botKoishi.close();
        executor.shutdown();
        executor = null;
    }

    private void registerEvents() {
        EventChannel<BotEvent> channel = botKoishi.getEventChannel();
        channel.exceptionHandler(create("group.recall"))
                .subscribe(MessageRecallEvent.GroupRecall.class, messageRecallEvent -> {
            MessageManager.onGroupRecall(messageRecallEvent);
            return ListeningStatus.LISTENING;
        });
        channel.exceptionHandler(create("friend.recall"))
                .subscribe(MessageRecallEvent.FriendRecall.class, messageRecallEvent -> {
            MessageManager.onFriendRecall(messageRecallEvent);
            return ListeningStatus.LISTENING;
        });
        channel.exceptionHandler(create("group.message"))
                .subscribe(GroupMessageEvent.class, messageEvent -> {
            MessageInfo info = new MessageInfo();
            info.data = messageEvent.getMessage();
            info.sender = messageEvent.getSender();
            info.group = messageEvent.getGroup();
            info.event = messageEvent;
            MessageManager.resolveGroupMessage(messageEvent.getMessage(), info);
            return ListeningStatus.LISTENING;
        });
        channel.exceptionHandler(create("friend.message"))
                .subscribe(FriendMessageEvent.class, messageEvent -> {
            MessageInfo info = new MessageInfo();
            info.data = messageEvent.getMessage();
            info.friend = messageEvent.getFriend();
            info.event = messageEvent;
            MessageManager.resolveFriendMessage(messageEvent.getMessage(), info);
            return ListeningStatus.LISTENING;
        });
        channel.exceptionHandler(create("stranger.message"))
                .subscribe(StrangerMessageEvent.class, messageEvent -> {
            MessageInfo info = new MessageInfo();
            info.data = messageEvent.getMessage();
            info.stranger = messageEvent.getSender();
            info.event = messageEvent;
            MessageManager.resolveStrangerMessage(messageEvent.getMessage(), info);
            return ListeningStatus.LISTENING;
        });
        channel.exceptionHandler(create("group.message.temp"))
                .subscribe(GroupTempMessageEvent.class, messageEvent -> {
            MessageInfo info = new MessageInfo();
            info.data = messageEvent.getMessage();
            info.sender = messageEvent.getSender();
            info.event = messageEvent;
            MessageManager.resolveGroupTempMessage(messageEvent.getMessage(), info);
            return ListeningStatus.LISTENING;
        });
        channel.exceptionHandler(create("group.join"))
                .subscribe(MemberJoinEvent.class, memberJoinEvent -> {
            MessageChain chain = MessageUtils.newChain(
                    new At(memberJoinEvent.getMember().getId()),
                    new PlainText(" 欢迎来到本群，要使用Koishi bot可以at或私聊输入~help查看帮助")
            );
            memberJoinEvent.getGroup().sendMessage(chain);
            return ListeningStatus.LISTENING;
        });
    }

    private Function1<Throwable, Unit> create(String name) {
        return exception -> {
            ErrorRecord.enqueueError(name, exception);
            return Unit.INSTANCE;
        };
    }
}