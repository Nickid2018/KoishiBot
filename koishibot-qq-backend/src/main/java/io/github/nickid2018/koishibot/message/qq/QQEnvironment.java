package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.UnsupportedMessage;
import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.network.Connection;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.message.data.*;

public class QQEnvironment extends Environment {

    private final Bot bot;
    private final QQMessagePublisher publisher;
    private final boolean nudgeEnabled;

    public QQEnvironment(Bot bot, boolean nudgeEnabled, Connection connection) {
        super(connection);
        this.bot = bot;
        this.nudgeEnabled = nudgeEnabled;

        publisher = new QQMessagePublisher(this);

        botID = "qq.user" + bot.getId();
        environmentName = "QQ";
        environmentUserPrefix = "qq.user";
        forwardMessageSupported = true;
        audioSupported = true;
        audioToFriendSupported = false;
        quoteSupported = true;
    }

    public User getQQUser(String id, boolean isStranger) {
        return id.startsWith("qq.user") ? (isStranger ?
                bot.getStranger(Long.parseLong(id.substring(7))) :
                bot.getFriend(Long.parseLong(id.substring(7)))
        ) : null;
    }

    public Group getQQGroup(String id) {
        return id.startsWith("qq.group") ? bot.getGroup(Long.parseLong(id.substring(8))) : null;
    }

    @Override
    public UserInfo getUser(String id, boolean isStranger) {
        return id.startsWith("qq.user") ?
                new QQUser(this, getQQUser(id, isStranger), isStranger) :
                null;
    }

    @Override
    public GroupInfo getGroup(String id) {
        return id.startsWith("qq.group") ? new QQGroup(this, getQQGroup(id)) : null;
    }

    public void close() {
        bot.close();
    }

    public boolean isNudgeEnabled() {
        return nudgeEnabled;
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

    public static void send(UserInfo user, AbstractMessage message) {
        User qq = ((QQUser) user).getUser();
        qq.sendMessage(((QQMessage) message).getMessage());
    }

    public static void send(GroupInfo group, AbstractMessage message) {
        Group qq = ((QQGroup) group).getGroup();
        qq.sendMessage(((QQMessage) message).getMessage());
    }
}
