package io.github.nickid2018.koishibot.message.kook;

import io.github.kookybot.message.MarkdownMessage;
import io.github.kookybot.message.SelfMessage;
import io.github.nickid2018.koishibot.message.api.*;

public abstract class KOOKMessage implements AbstractMessage {

    protected final KOOKEnvironment environment;
    protected SelfMessage sentMessage;

    public KOOKMessage(KOOKEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public void send(UserInfo contact) {
        // Unsupported
    }

    @Override
    public void send(GroupInfo group) {
        KOOKMessageData data = new KOOKMessageData();
        formatMessage(data);
        MarkdownMessage message = new MarkdownMessage(environment.getKookClient(), String.join("", data.getTexts()));
        message.setQuote(data.getQuoteID());
        sentMessage = ((KOOKTextChannel) group).getChannel().sendMessage(message);
    }

    @Override
    public void recall() {
        if (sentMessage != null)
            sentMessage.delete();
    }

    @Override
    public long getSentTime() {
        return sentMessage.getTimestamp();
    }

    @Override
    public MessageFrom getSource() {
        if (sentMessage != null)
            return new KOOKMessageFrom(sentMessage.getId());
        return null;
    }

    public abstract void formatMessage(KOOKMessageData data);
}
