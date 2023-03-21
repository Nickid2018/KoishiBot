package io.github.nickid2018.koishibot.message.kook;

import io.github.kookybot.message.SelfMessage;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageSource;
import io.github.nickid2018.koishibot.util.TimeoutCache;

public class KOOKMessageSource extends MessageSource {

    public static TimeoutCache<KOOKMessageSource> messageCache = new TimeoutCache<>();

    private SelfMessage sentMessage;

    public KOOKMessageSource(KOOKEnvironment environment) {
        super(environment);
    }

    public KOOKMessageSource(Environment environment, String msgID, SelfMessage message) {
        super(environment);
        messageUniqueID = msgID;
        sentTime = -1;
        sentMessage = message;
        if (sentMessage != null)
            messageCache.put(messageUniqueID, this, 1000 * 60 * 5);
    }

    public static void recall(KOOKMessageSource source) {
        if (source.sentMessage != null)
            source.sentMessage.delete();
    }
}
