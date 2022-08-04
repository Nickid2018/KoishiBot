package io.github.nickid2018.koishibot.module.translation;

import io.github.nickid2018.koishibot.core.ErrorRecord;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.util.AsyncUtil;

import java.util.regex.Pattern;

@ResolverName("translate")
@Syntax(syntax = "trans:((源语言)->(目标语言)):[翻译内容](|)", help = "翻译一句话")
public class TranslateResolver extends MessageResolver {

    public static final Pattern TRANSLATE_PATTERN = Pattern.compile(
            "trans:([a-zA-Z\\-]*->[a-zA-Z\\-]*)?:.+", Pattern.MULTILINE | Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    public static TranslationProvider provider = new YoudaoTranslation();

    public TranslateResolver() {
        super(TRANSLATE_PATTERN);
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        AsyncUtil.execute(() -> {
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
            environment.getMessageSender().sendMessageRecallable(context, environment.newChain(
                    environment.newQuote(context.message()),
                    environment.newText(data)
            ));
        });
        return true;
    }
}
