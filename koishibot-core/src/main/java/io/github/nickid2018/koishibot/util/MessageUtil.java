package io.github.nickid2018.koishibot.util;

import io.github.nickid2018.koishibot.message.api.AtMessage;
import io.github.nickid2018.koishibot.message.api.ChainMessage;
import io.github.nickid2018.koishibot.message.api.QuoteMessage;
import io.github.nickid2018.koishibot.message.api.TextMessage;

import java.util.List;
import java.util.stream.Stream;

public class MessageUtil {

    public static String getFirstText(ChainMessage chainMessage) {
        List<TextMessage> texts = Stream.of(chainMessage.getMessages())
                .filter(m -> m instanceof TextMessage).map(m -> (TextMessage) m).toList();
        return texts.size() == 0 ? null : texts.get(0).getText();
    }

    public static AtMessage[] getAts(ChainMessage chainMessage) {
        List<AtMessage> texts = Stream.of(chainMessage.getMessages())
                .filter(m -> m instanceof AtMessage).map(m -> (AtMessage) m).toList();
        return texts.toArray(AtMessage[]::new);
    }

    public static QuoteMessage getQuote(ChainMessage chainMessage) {
        List<QuoteMessage> quoteMessages = Stream.of(chainMessage.getMessages())
                .filter(m -> m instanceof QuoteMessage).map(m -> (QuoteMessage) m).toList();
        return quoteMessages.size() == 0 ? null : quoteMessages.get(0);
    }
}
