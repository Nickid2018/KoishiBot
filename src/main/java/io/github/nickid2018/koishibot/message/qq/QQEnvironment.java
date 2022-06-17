package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.MessageSender;
import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.message.api.ForwardMessage;
import io.github.nickid2018.koishibot.message.api.UnsupportedMessage;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.message.data.*;

public class QQEnvironment implements Environment {

    private final Bot bot;
    private final QQMessagePublisher publisher;
    private final MessageSender sender;

    public QQEnvironment(Bot bot) {
        this.bot = bot;
        publisher = new QQMessagePublisher(this);
        sender = new MessageSender(this, true);
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
    public UserInfo getUser(String id, boolean isStranger) {
        return new QQUser(isStranger ? bot.getStranger(Long.parseLong(id.substring(7))) :
                bot.getFriend(Long.parseLong(id.substring(7))), isStranger, false);
    }

    @Override
    public GroupInfo getGroup(String id) {
        return new QQGroup(bot.getGroup(Long.parseLong(id.substring(8))));
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
        else
            return new UnsupportedMessage(this);
    }
}
