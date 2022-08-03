package io.github.nickid2018.koishibot.filter;

import io.github.nickid2018.koishibot.module.mc.chat.MCChatBridgeModule;
import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MCChatBridgeFilter implements PreFilter {

    @Nullable
    @Override
    public ChainMessage filterMessagePre(ChainMessage input, MessageContext context, Environment environment) {
        AsyncUtil.execute(() -> MCChatBridgeModule.INSTANCE.onReceiveGroupText(
                context.group(), context.user(), messageToString(input, context)));
        return input;
    }

    private String messageToString(AbstractMessage message, MessageContext context) {
        if (message instanceof ChainMessage chain)
            return Stream.of(chain.getMessages())
                    .filter(m -> !(m instanceof UnsupportedMessage))
                    .map(m -> messageToString(m, context))
                    .map(String::trim)
                    .collect(Collectors.joining(" "));
        else if (message instanceof TextMessage text)
            return text.getText();
        else if (message instanceof ImageMessage)
            return "§7[图片]§r";
        else if (message instanceof AudioMessage)
            return "§7[语音]§r";
        else if (message instanceof ForwardMessage)
            return "§7[转发信息]§r";
        else if (message instanceof AtMessage at)
            return "§e@" + at.getUser(context.group()).getNameInGroup(context.group()) + "§r";
        else if (message instanceof QuoteMessage)
            return "§7[回复信息]§r";
        else
            return "§7[不支持的消息类型]§r";
    }
}
