package io.github.nickid2018.koishibot.message.qq;

import net.mamoe.mirai.message.data.MessageSource;

import java.util.Arrays;

public class QQMessageSource extends io.github.nickid2018.koishibot.message.api.MessageSource {

    private final MessageSource source;

    public QQMessageSource(QQEnvironment environment, MessageSource source) {
        super(environment);
        this.source = source;
        this.sentTime = source.getTime();
        this.messageUniqueID = "%d %d %d %s %s".formatted(
                source.getFromId(), source.getTargetId(),  source.getTime(),
                Arrays.toString(source.getIds()), Arrays.toString(source.getInternalIds()));
    }

    public MessageSource getSource() {
        return source;
    }

    public static void recall(QQMessageSource source) {
        MessageSource.recall(source.source);
    }
}
