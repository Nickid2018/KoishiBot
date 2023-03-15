package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.ChainMessage;
import io.github.nickid2018.koishibot.network.ByteData;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageSource;
import net.mamoe.mirai.message.data.MessageUtils;

import java.util.stream.Stream;

public class QQChain extends ChainMessage implements QQMessage {

    private MessageChain chain;

    protected QQChain(QQEnvironment environment) {
        super(environment);
    }

    protected QQChain(QQEnvironment environment, MessageChain chain) {
        super(environment);
        this.chain = chain;
        MessageSource messageSource = chain.get(MessageSource.Key);
        source = messageSource == null ? null : new QQMessageSource(environment, messageSource);
        messages = chain.stream().map(environment::cast).toArray(AbstractMessage[]::new);
    }

    @Override
    public Message getMessage() {
        return chain;
    }

    @Override
    public void read(ByteData buf) {
        super.read(buf);
        chain = MessageUtils.newChain(
                Stream.of(messages).map(m -> ((QQMessage) m).getMessage()).toArray(Message[]::new)
        );
    }
}
