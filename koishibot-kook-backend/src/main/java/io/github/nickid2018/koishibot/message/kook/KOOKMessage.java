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
        kookMessage.setSentMessage(((KOOKTextChannel) group).getChannel().sendMessage(message));
        if (kookMessage instanceof KOOKChain kookChain) {
            if (!kookChain.getImages().isEmpty())
                kookChain.getImages().forEach(image -> send(image, group));
        }
    }

    static void recall(KOOKMessage message) {
        if (message.getSentMessage() != null)
            message.getSentMessage().delete();
    }

    static long getSentTime(KOOKMessage message) {
        return message.getSentMessage().getTimestamp();
    }

    static MessageSource getSource(KOOKMessage message) {
        if (message.getSentMessage() != null)
            return new KOOKMessageSource(message.getEnvironment(), message.getSentMessage().getId(), message.getSentMessage());
        return null;
    }

    Environment getEnvironment();
    void formatMessage(KOOKMessageData data);
    void setSentMessage(SelfMessage message);
    SelfMessage getSentMessage();
}
