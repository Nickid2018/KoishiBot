package io.github.nickid2018.koishibot.message.kook;

import io.github.nickid2018.koishibot.message.api.MessageFrom;

public class KOOKMessageFrom implements MessageFrom {

    private final String msgID;

    public KOOKMessageFrom(String msgID) {
        this.msgID = msgID;
    }

    @Override
    public boolean equals(MessageFrom source) {
        return source instanceof KOOKMessageFrom other && other.msgID.equals(msgID);
    }
}
