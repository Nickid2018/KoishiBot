package io.github.nickid2018.koishibot.message.kook;

import io.github.kookybot.message.SelfMessage;
import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.ChainMessage;
import io.github.nickid2018.koishibot.network.ByteData;

import java.util.ArrayList;
import java.util.List;

public class KOOKChain extends ChainMessage implements KOOKMessage {

    private KOOKMessageData data;
    private SelfMessage sentMessage;
    private final List<KOOKImage> images = new ArrayList<>();

    public KOOKChain(KOOKEnvironment environment) {
        super(environment);
    }

    public KOOKChain(KOOKEnvironment environment, KOOKMessageData data) {
        super(environment);
        this.data = data;
        messages = getMessages();
    }

    @Override
    public void read(ByteData buf) {
        super.read(buf);
        data = new KOOKMessageData();
        for (AbstractMessage message : messages) {
            if (message instanceof KOOKImage image)
                images.add(image);
            else if (message instanceof KOOKQuote quote)
                data.setQuoteID(quote.getMessage().data.getMsgID());
            else if (message instanceof KOOKMessage kook)
                kook.formatMessage(data);
        }
    }

    @Override
    public AbstractMessage[] getMessages() {
        List<AbstractMessage> messages = new ArrayList<>();
        if (data.getQuoteID() != null)
            messages.add(new KOOKQuote((KOOKEnvironment) env, this));
        data.getMentionUsers().stream().map(user -> new KOOKAt((KOOKEnvironment) env, user)).forEach(messages::add);
        data.getTexts().stream().map(text -> new KOOKText((KOOKEnvironment) env, text)).forEach(messages::add);
        return messages.toArray(AbstractMessage[]::new);
    }

    @Override
    public void formatMessage(KOOKMessageData data) {
        data.getMentionUsers().addAll(this.data.getMentionUsers());
        data.getTexts().addAll(this.data.getTexts());
        data.setQuoteID(this.data.getQuoteID());
    }

    public List<KOOKImage> getImages() {
        return images;
    }

    @Override
    public void setSentMessage(SelfMessage message) {
        sentMessage = message;
    }

    @Override
    public SelfMessage getSentMessage() {
        return sentMessage;
    }

    public KOOKMessageData getData() {
        return data;
    }
}
