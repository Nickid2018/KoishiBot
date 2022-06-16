package io.github.nickid2018.koishibot.resolver;

import io.github.nickid2018.koishibot.core.ErrorRecord;
import io.github.nickid2018.koishibot.KoishiBotMain;
import io.github.nickid2018.koishibot.core.MessageInfo;
import io.github.nickid2018.koishibot.translation.TranslationProvider;
import io.github.nickid2018.koishibot.translation.YoudaoTranslation;
import net.mamoe.mirai.message.data.*;

import java.util.regex.Pattern;

public class TranslateResolver extends MessageResolver {

    public static final Pattern TRANSLATE_PATTERN = Pattern.compile(
            "trans:([a-zA-Z\\-]*->[a-zA-Z\\-]*)?:.+", Pattern.MULTILINE | Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    public static TranslationProvider provider = new YoudaoTranslation();

    public TranslateResolver() {
        super(TRANSLATE_PATTERN);
    }

    @Override
    public boolean resolveInternal(String key, MessageInfo info, Pattern pattern) {
        KoishiBotMain.INSTANCE.executor.execute(() -> {
            if (info.event.getSender().getId() == 694000037 || info.event.getSender().getId() == 2435219469L) {
                info.sendMessage(MessageUtils.newChain(
                        new QuoteReply(info.data),
                        new PlainText("我要把你挂在地灵殿门口做装饰")
                ));
                return;
            }
            String[] splits = key.split(":", 3);
            String from = null, to = null;
            if (!splits[1].isEmpty()) {
                String[] keys = splits[1].split("->");
                from = keys[0];
                if (keys.length == 2)
                    to = keys[1];
            }
            String trans = splits[2];
            if (trans.contains("|"))
                trans = trans.substring(0, trans.indexOf("|"));
            String data;
            try {
                String translation = provider.translate(trans, from, to);
                data = splits[1].isEmpty() ? translation : "(" + splits[1] + ")" + translation;
            } catch (Exception e) {
                data = "无法处理翻译：" + e.getMessage();
                ErrorRecord.enqueueError("translation", e);
            }
            MessageChain chain = MessageUtils.newChain(
                    new QuoteReply(info.data),
                    new PlainText(data)
            );
            info.sendMessageRecallable(chain);
        });
        return true;
    }
}
