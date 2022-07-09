package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.MessageFrom;
import net.mamoe.mirai.message.data.MessageSource;

import java.util.Arrays;

public class QQMessageFrom implements MessageFrom {

    private final net.mamoe.mirai.message.data.MessageSource source;

    public QQMessageFrom(net.mamoe.mirai.message.data.MessageSource source) {
        this.source = source;
    }

    public MessageSource getSource() {
        return source;
    }

    @Override
    public boolean equals(MessageFrom from) {
        if (!(from instanceof QQMessageFrom))
            return false;
        MessageSource other = ((QQMessageFrom) from).source;
        return source.getFromId() == other.getFromId() && source.getTargetId() == other.getTargetId() &&
                Arrays.equals(source.getIds(), other.getIds()) &&
                Arrays.equals(source.getInternalIds(), other.getInternalIds());
    }

    @Override
    public String toString() {
        return source.getFromId() + " " + source.getTargetId() + " " + source.getTime() + " " + Arrays.toString(source.getIds())
                + Arrays.toString(source.getInternalIds());
    }
}
