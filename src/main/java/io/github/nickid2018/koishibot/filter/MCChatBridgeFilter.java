package io.github.nickid2018.koishibot.filter;

import io.github.nickid2018.koishibot.mc.chat.MCChatBridge;
import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MCChatBridgeFilter implements PreFilter {

    @Nullable
    @Override
    public ChainMessage filterMessagePre(ChainMessage input, MessageContext context, Environment environment) {
        AsyncUtil.execute(() -> MCChatBridge.INSTANCE.onReceiveGroupText(
                context.group(), context.user(), messageToString(input, context)));
        return input;
    }

    private String messageToString(AbstractMessage message, MessageContext context) {
        if (message instanceof ChainMessage chain)
            return Stream.of(chain.getMessages()).map(m -> messageToString(m, context)).collect(Collectors.joining(" "));
        else if (message instanceof TextMessage text)
            return text.getText();
        else if (message instanceof ImageMessage)
            return "[图片]";
        else if (message instanceof AudioMessage)
            return "[语音]";
        else if (message instanceof ForwardMessage)
            return "[转发信息]";
        else if (message instanceof AtMessage at)
            return "@" + at.getUser(context.group()).getNameInGroup(context.group());
        else if (message instanceof QuoteMessage quote)
            return quote.getReplyTo() != null ?
                    "@" + quote.getReplyTo().getNameInGroup(context.group()) :
                    "[回复信息]";
        else if (message instanceof UnsupportedMessage)
            return "";
        else
            return "[不支持的消息类型]";
    }
}
