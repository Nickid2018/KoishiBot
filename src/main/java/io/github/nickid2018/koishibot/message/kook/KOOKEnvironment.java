package io.github.nickid2018.koishibot.message.kook;

import io.github.nickid2018.koishibot.message.MessageManager;
import io.github.nickid2018.koishibot.message.MessageSender;
import io.github.nickid2018.koishibot.message.api.*;

public class KOOKEnvironment implements Environment {

    public KOOKEnvironment() {

    }

    @Override
    public AtMessage newAt() {
        return null;
    }

    @Override
    public ChainMessage newChain() {
        return null;
    }

    @Override
    public TextMessage newText() {
        return null;
    }

    @Override
    public AudioMessage newAudio() {
        return null;
    }

    @Override
    public ImageMessage newImage() {
        return null;
    }

    @Override
    public ForwardMessage newForwards() {
        return null;
    }

    @Override
    public MessageEntry newMessageEntry() {
        return null;
    }

    @Override
    public QuoteMessage newQuote() {
        return null;
    }

    @Override
    public ServiceMessage newService() {
        return null;
    }

    @Override
    public UserInfo getUser(String id, boolean isStranger) {
        return null;
    }

    @Override
    public GroupInfo getGroup(String id) {
        return null;
    }

    @Override
    public String getBotId() {
        return null;
    }

    @Override
    public MessageEventPublisher getEvents() {
        return null;
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
        return false;
    }

    @Override
    public void close() {

    }
}
