package io.github.nickid2018.koishibot.message.kook;

import io.github.kookybot.JavaBaseClass;
import io.github.kookybot.client.Client;
import io.github.kookybot.contract.Self;
import io.github.kookybot.contract.TextChannel;
import io.github.nickid2018.koishibot.core.BotLoginData;
import io.github.nickid2018.koishibot.message.MessageManager;
import io.github.nickid2018.koishibot.message.MessageSender;
import io.github.nickid2018.koishibot.message.api.*;
import kotlin.Unit;

public class KOOKEnvironment implements Environment {

    private final Client kookClient;
    private final Self self;
    private final MessageSender sender;
    private final MessageManager manager;
    private final KOOKMessagePublisher publisher;

    public KOOKEnvironment(BotLoginData data) {
        kookClient = new Client(data.token(), configureScope -> Unit.INSTANCE);
        self = JavaBaseClass.utils.connectWebsocket(kookClient);

        publisher = new KOOKMessagePublisher(this);
        sender = new MessageSender(this, false);
        manager = new MessageManager(this);
    }

    @Override
    public AtMessage at() {
        return new KOOKAt(this);
    }

    @Override
    public ChainMessage chain() {
        return new KOOKChain(this);
    }

    @Override
    public TextMessage text() {
        return new KOOKText(this);
    }

    @Override
    public ImageMessage image() {
        return new KOOKImage(this);
    }

    @Override
    public AudioMessage audio() {
        // Unsupported
        return null;
    }

    @Override
    public ForwardMessage forwards() {
        // Unsupported
        return null;
    }

    @Override
    public MessageEntry messageEntry() {
        // Unsupported
        return null;
    }

    @Override
    public QuoteMessage quote() {
        return new KOOKQuote(this);
    }

    @Override
    public ServiceMessage service() {
        // Unsupported
        return null;
    }

    @Override
    public UserInfo getUser(String id, boolean isStranger) {
        return id.startsWith("kook.user") ? new KOOKUser(this, self.getUser(id.substring(9)), isStranger) : null;
    }

    @Override
    public GroupInfo getGroup(String id) {
        return id.startsWith("kook.group") ? new KOOKTextChannel(this,
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
    public String getEnvironmentName() {
        return "开黑啦";
    }

    @Override
    public String getEnvironmentUserPrefix() {
        return "kook.user";
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
