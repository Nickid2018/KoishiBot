package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.QuoteMessage;
import io.github.nickid2018.koishibot.network.ByteData;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.QuoteReply;

public class QQQuote extends QuoteMessage implements QQMessage {

    private QuoteReply quoteReply;

    protected QQQuote(QQEnvironment environment) {
        super(environment);
    }

    protected QQQuote(QQEnvironment environment, QuoteReply quoteReply) {
        super(environment);
        this.quoteReply = quoteReply;
        this.replyTo = environment.getUser("qq.user" + quoteReply.getSource().getFromId(), true);
        this.message = new QQChain(environment, quoteReply.getSource().getOriginalMessage());
        this.quoteFrom = new QQMessageSource(environment, quoteReply.getSource());
    }

    @Override
    public Message getMessage() {
        return quoteReply;
    }

    @Override
    public void read(ByteData buf) {
        super.read(buf);
        quoteReply = new QuoteReply(((QQMessageSource) quoteFrom).getSource());
    }
}
