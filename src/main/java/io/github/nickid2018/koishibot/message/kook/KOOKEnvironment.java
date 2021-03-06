package io.github.nickid2018.koishibot.message.kook;

import io.github.nickid2018.koishibot.core.BotLoginData;
import io.github.nickid2018.koishibot.message.MessageManager;
import io.github.nickid2018.koishibot.message.MessageSender;
import io.github.nickid2018.koishibot.message.api.*;
import io.github.zly2006.kookybot.JavaBaseClass;
import io.github.zly2006.kookybot.client.Client;
import io.github.zly2006.kookybot.contract.Self;
import io.github.zly2006.kookybot.contract.TextChannel;

public class KOOKEnvironment implements Environment {

    private final Client kookClient;
    private final Self self;

    private final MessageSender sender;
    private final MessageManager manager;
    private final KOOKMessagePublisher publisher;

    public KOOKEnvironment(BotLoginData data) {
        kookClient = new Client(data.token());
        self = JavaBaseClass.utils.connectWebsocket(kookClient);

        publisher = new KOOKMessagePublisher(this);
        sender = new MessageSender(this, false);
        manager = new MessageManager(this);
    }

    @Override
    public AtMessage newAt() {
        return new KOOKAt(this);
    }

    @Override
    public ChainMessage newChain() {
        return new KOOKChain(this);
    }

    @Override
    public TextMessage newText() {
        return new KOOKText(this);
    }

    @Override
    public ImageMessage newImage() {
        return new KOOKImage(this);
    }

    @Override
    public AudioMessage newAudio() {
        // Unsupported
        return null;
    }

    @Override
    public ForwardMessage newForwards() {
        // Unsupported
        return null;
    }

    @Override
    public MessageEntry newMessageEntry() {
        // Unsupported
        return null;
    }

    @Override
    public QuoteMessage newQuote() {
        // Unsupported
        return null;
    }

    @Override
    public ServiceMessage newService() {
        // Unsupported
        return null;
    }

    @Override
    public UserInfo getUser(String id, boolean isStranger) {
        return id.startsWith("kook.user") ? new KOOKUser(self.getUser(id.substring(9)), isStranger) : null;
    }

    @Override
    public GroupInfo getGroup(String id) {
        return id.startsWith("kook.group") ? new KOOKTextChannel(
                (TextChannel) self.getChannel(id.substring(10))) : null;
    }

    @Override
    public String getBotId() {
        return "kook.user" + self.getId();
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
        return false;
    }

    @Override
    public boolean audioSupported() {
        return false;
    }

    @Override
    public boolean quoteSupported() {
        return false;
    }

    @Override
    public void close() {
        kookClient.close();
    }

    public Client getKookClient() {
        return kookClient;
    }

    public Self getSelf() {
        return self;
    }
}
