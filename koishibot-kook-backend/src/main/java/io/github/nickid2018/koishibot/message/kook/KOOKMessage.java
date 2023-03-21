package io.github.nickid2018.koishibot.message.kook;

import io.github.kookybot.message.AtKt;
import io.github.kookybot.message.MarkdownMessage;
import io.github.kookybot.message.SelfMessage;
import io.github.nickid2018.koishibot.message.api.*;

public interface KOOKMessage {

    static void send(KOOKMessage kookMessage, GroupInfo group) {
        KOOKMessageData data = new KOOKMessageData();
        kookMessage.formatMessage(data);
        MarkdownMessage message = new MarkdownMessage(
                ((KOOKEnvironment) kookMessage.getEnvironment()).getKookClient(), String.join("", data.getTexts()));
        message.setQuote(data.getQuoteID());
        data.getMentionUsers().forEach(user -> message.append(AtKt.At(user)));
        if (!message.content().isEmpty())
            kookMessage.setSentMessage(((KOOKTextChannel) group).getChannel().sendMessage(message));
        if (kookMessage instanceof KOOKImage kookImage)
            kookImage.send(group);
        if (kookMessage instanceof KOOKChain kookChain) {
            if (!kookChain.getImages().isEmpty())
                kookChain.getImages().forEach(image -> image.send(group));
        }
    }

    Environment getEnvironment();
    void formatMessage(KOOKMessageData data);
    void setSentMessage(SelfMessage message);
    SelfMessage getSentMessage();
}
