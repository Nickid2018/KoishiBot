package io.github.nickid2018.koishibot.filter;

import io.github.nickid2018.koishibot.message.DelegateEnvironment;
import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.module.ModuleManager;
import io.github.nickid2018.koishibot.module.mc.chat.MCChatBridgeModule;
import io.github.nickid2018.koishibot.util.AsyncUtil;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MCChatBridgeFilter implements PreFilter {

    @Nullable
    @Override
    public ChainMessage filterMessagePre(ChainMessage input, MessageContext context, DelegateEnvironment environment) {
        assert context.group() != null;
        if (ModuleManager.isOpened(context.group().getGroupId(), "mcchat"))
            AsyncUtil.execute(() -> MCChatBridgeModule.INSTANCE.onReceiveGroupText(
                    context.group(), context.user(), messageToString(input, context)));
        return input;
    }

    private String messageToString(AbstractMessage message, MessageContext context) {
        String str;
        if (message instanceof ChainMessage chain)
            str = Stream.of(chain.getMessages())
                    .filter(Objects::nonNull)
                    .filter(m -> !(m instanceof UnsupportedMessage))
                    .map(m -> messageToString(m, context))
                    .map(String::trim)
                    .collect(Collectors.joining(" "));
        else if (message instanceof TextMessage text)
            str = text.getText();
        else if (message instanceof ImageMessage)
            str = "§7[图片]§r";
        else if (message instanceof AudioMessage)
            str = "§7[语音]§r";
        else if (message instanceof ForwardMessage)
            str = "§7[转发信息]§r";
        else if (message instanceof AtMessage at)
            str = "§e@" + at.getUser(context.group()).getNameInGroup(context.group()) + "§r";
        else if (message instanceof QuoteMessage)
            str = "§7[回复信息]§r";
        else
            str = "§7[不支持的消息类型]§r";
        return str == null ? "" : str;
    }
}
