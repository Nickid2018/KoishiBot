package io.github.nickid2018.koishibot.message.kook;

import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.ChainMessage;
import io.github.nickid2018.koishibot.message.api.GroupInfo;

import java.util.ArrayList;
import java.util.List;

public class KOOKChain extends KOOKMessage implements ChainMessage {

    private KOOKMessageData data;
    private final List<KOOKImage> images = new ArrayList<>();

    public KOOKChain(KOOKEnvironment environment) {
        super(environment);
    }

    public KOOKChain(KOOKEnvironment environment, KOOKMessageData data) {
        super(environment);
        this.data = data;
    }

    @Override
    public void send(GroupInfo group) {
        super.send(group);
        if (!images.isEmpty())
            images.forEach(image -> image.send(group));
    }

    @Override
    public ChainMessage fillChain(AbstractMessage... messages) {
        data = new KOOKMessageData();
        for (AbstractMessage message : messages) {
            if (message instanceof KOOKImage image)
                images.add(image);
            else if (message instanceof KOOKQuote quote)
                data.setQuoteID(quote.getMessage().data.getMsgID());
            else if (message instanceof KOOKMessage kook)
                kook.formatMessage(data);
        }
        return this;
    }

    @Override
    public AbstractMessage[] getMessages() {
        List<AbstractMessage> messages = new ArrayList<>();
        if (data.getQuoteID() != null)
            messages.add(new KOOKQuote(environment, this));
        data.getMentionUsers().stream().map(user -> new KOOKAt(environment, user)).forEach(messages::add);
        data.getTexts().stream().map(text -> new KOOKText(environment, text)).forEach(messages::add);
        return messages.toArray(AbstractMessage[]::new);
    }

    @Override
    public void formatMessage(KOOKMessageData data) {
        data.getMentionUsers().addAll(this.data.getMentionUsers());
        data.getTexts().addAll(this.data.getTexts());
        data.setQuoteID(this.data.getQuoteID());
    }

    public KOOKMessageData getData() {
        return data;
    }
}
