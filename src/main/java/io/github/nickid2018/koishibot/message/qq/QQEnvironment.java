package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.core.BotLoginData;
import io.github.nickid2018.koishibot.message.MessageManager;
import io.github.nickid2018.koishibot.message.MessageSender;
import io.github.nickid2018.koishibot.message.api.ForwardMessage;
import io.github.nickid2018.koishibot.message.api.ServiceMessage;
import io.github.nickid2018.koishibot.message.api.UnsupportedMessage;
import io.github.nickid2018.koishibot.message.api.*;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.BotConfiguration;
import net.mamoe.mirai.utils.LoggerAdapters;
import org.slf4j.LoggerFactory;

import java.io.File;

public class QQEnvironment implements Environment {

    private final Bot bot;
    private final QQMessagePublisher publisher;
    private final MessageSender sender;
    private final MessageManager manager;

    public QQEnvironment(BotLoginData loginData) {
        bot = BotFactory.INSTANCE.newBot(Long.parseLong(loginData.uid()), loginData.password(), new BotConfiguration() {{
            setHeartbeatStrategy(HeartbeatStrategy.STAT_HB);
            setWorkingDir(new File("qq"));
            fileBasedDeviceInfo();
            setBotLoggerSupplier(bot -> LoggerAdapters.asMiraiLogger(LoggerFactory.getLogger("Mirai Bot")));
            setNetworkLoggerSupplier(bot -> LoggerAdapters.asMiraiLogger(LoggerFactory.getLogger("Mirai Net")));
        }});
        bot.login();
        publisher = new QQMessagePublisher(this);
        sender = new MessageSender(this, true);
        manager = new MessageManager(this);
    }

    @Override
    public AtMessage newAt() {
        return new QQAt(this);
    }

    @Override
    public ChainMessage newChain() {
        return new QQChain(this);
    }

    @Override
    public TextMessage newText() {
        return new QQText(this);
    }

    @Override
    public AudioMessage newAudio() {
        return new QQAudio(this);
    }

    @Override
    public ImageMessage newImage() {
        return new QQImage(this);
    }

    @Override
    public ForwardMessage newForwards() {
        return new QQForward(this);
    }

    @Override
    public MessageEntry newMessageEntry() {
        return new QQMessageEntry(this);
    }

    @Override
    public QuoteMessage newQuote() {
        return new QQQuote(this);
    }

    @Override
    public ServiceMessage newService() {
        return new QQService(this);
    }

    @Override
    public UserInfo getUser(String id, boolean isStranger) {
        return id.startsWith("qq.user") ? (new QQUser(this, isStranger ? bot.getStranger(Long.parseLong(id.substring(7))) :
                bot.getFriend(Long.parseLong(id.substring(7))), isStranger, false)) : null;
    }

    @Override
    public GroupInfo getGroup(String id) {
        return id.startsWith("qq.group") ? new QQGroup(this, bot.getGroup(Long.parseLong(id.substring(8)))) : null;
    }

    @Override
    public String getBotId() {
        return "qq.user" + bot.getId();
    }

    @Override
    public MessageEventPublisher getEvents() {
        return publisher;
    }

    @Override
    public MessageSender getMessageSender() {
        return sender;
    }

    @Override
    public MessageManager getManager() {
        return manager;
    }

    @Override
    public boolean forwardMessageSupported() {
        return true;
    }

    @Override
    public boolean audioSupported() {
        return true;
    }

    @Override
    public boolean quoteSupported() {
        return true;
    }

    @Override
    public String getEnvironmentName() {
        return "QQ";
    }

    @Override
    public String getEnvironmentUserPrefix() {
        return "qq.user";
    }

    public void close() {
        bot.close();
    }

    public Bot getBot() {
        return bot;
    }

    public AbstractMessage cast(Message message) {
        if (message instanceof MessageChain)
            return new QQChain(this, (MessageChain) message);
        else if (message instanceof At)
            return new QQAt(this, (At) message);
        else if (message instanceof PlainText)
            return new QQText(this, (PlainText) message);
        else if (message instanceof Audio)
            return new QQAudio(this, (Audio) message);
        else if (message instanceof Image)
            return new QQImage(this, (Image) message);
        else if (message instanceof QuoteReply)
            return new QQQuote(this, (QuoteReply) message);
        else if (message instanceof net.mamoe.mirai.message.data.ForwardMessage)
            return new QQForward(this, (net.mamoe.mirai.message.data.ForwardMessage) message);
        else if (message instanceof RichMessage)
            return new QQService(this, (RichMessage) message);
        else
            return new UnsupportedMessage(this);
    }
}
