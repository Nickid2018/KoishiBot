package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.ChainMessage;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageSource;
import net.mamoe.mirai.message.data.MessageUtils;

import java.util.stream.Stream;

public class QQChain extends QQMessage implements ChainMessage {

    private MessageChain chain;

    protected QQChain(QQEnvironment environment) {
        super(environment);
    }

    protected QQChain(QQEnvironment environment, MessageChain chain) {
        super(environment);
        this.chain = chain;
    }

    @Override
    public void recall() {
        super.recall();
        MessageSource source;
        if ((source = chain.get(MessageSource.Key)) != null)
            MessageSource.recall(source);
    }

    @Override
    public ChainMessage fillChain(AbstractMessage... messages) {
        chain = MessageUtils.newChain(
                Stream.of(messages).map(m -> ((QQMessage) m).getQQMessage()).toArray(Message[]::new)
        );
        return this;
    }

    @Override
    public AbstractMessage[] getMessages() {
        return chain.stream().map(environment::cast).toArray(AbstractMessage[]::new);
    }

    @Override
    protected Message getQQMessage() {
        return chain;
    }
}
