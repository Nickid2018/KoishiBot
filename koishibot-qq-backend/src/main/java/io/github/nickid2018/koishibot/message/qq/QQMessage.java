package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.MessageSource;
import net.mamoe.mirai.message.data.Message;

public interface QQMessage {

    Message getMessage();

    void setMessageSource(MessageSource source);
}
