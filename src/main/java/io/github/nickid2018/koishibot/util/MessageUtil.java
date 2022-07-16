package io.github.nickid2018.koishibot.util;

import io.github.nickid2018.koishibot.message.api.ChainMessage;
import io.github.nickid2018.koishibot.message.api.TextMessage;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MessageUtil {

    public static String getFirstText(ChainMessage chainMessage) {
        List<TextMessage> texts = Stream.of(chainMessage.getMessages())
                .filter(m -> m instanceof TextMessage).map(m -> (TextMessage) m).collect(Collectors.toList());
        return texts.size() == 0 ? null : texts.get(0).getText();
    }
}
