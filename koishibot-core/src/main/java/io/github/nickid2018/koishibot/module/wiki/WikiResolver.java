package io.github.nickid2018.koishibot.module.wiki;

import io.github.nickid2018.koishibot.core.TempFileSystem;
import io.github.nickid2018.koishibot.message.*;
import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.ChainMessage;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.message.api.TextMessage;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.web.WebUtil;

import java.io.File;
import java.util.Locale;
import java.util.regex.Pattern;

@ResolverName("wiki")
@Syntax(syntax = "[[查询页面]]", help = "查询wiki页面", rem = "允许使用跨wiki和章节，强制禁止进行跨wiki需要再加一层中括号，特殊页面查询如下")
@Syntax(syntax = "{{查询模板}}", help = "查询wiki模板", rem = "允许使用跨wiki")
@Syntax(syntax = "~rd", help = "随机页面")
@Syntax(syntax = "~iw", help = "查看跨wiki数据")
@Syntax(syntax = "~search [查询内容]", help = "查询wiki页面")
@Syntax(syntax = "~page [pageID]", help = "查询指定ID的wiki页面")
public class WikiResolver extends MessageResolver {

    public static final Pattern WIKI_PATTERN = Pattern.compile("\\[{2,3}.+?]{2,3}+");
    public static final Pattern WIKI_TEMPLATE_PATTERN = Pattern.compile("\\{\\{.+?}}");

    public WikiResolver() {
        super(WIKI_PATTERN, WIKI_TEMPLATE_PATTERN);
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, DelegateEnvironment environment) {
        key = key.substring(2, key.length() - 2);
        boolean isTemplate = resolvedArguments == WIKI_TEMPLATE_PATTERN;
        String finalKey = key.substring(0, key.indexOf('|') < 0 ? key.length() : key.indexOf('|'));
        AsyncUtil.execute(() -> {
            String[] splits = finalKey.split(":", 2);
            try {
                if (splits.length == 1 || !WikiInfo.SUPPORT_WIKIS.containsKey(splits[0].toLowerCase(Locale.ROOT)))
                    requestWikiPage(WikiInfo.SUPPORT_WIKIS.get(WikiInfo.BASE_WIKI), null,
                            isTemplate ? "Template:" + finalKey : finalKey, isTemplate, context, null, environment);
                else {
                    int lastNamespace = splits[1].lastIndexOf(':');
                    String query = splits[1];
                    if (isTemplate)
                        if (lastNamespace >= 0)
                            query = splits[1].substring(0, lastNamespace + 1) + "Template:" + splits[1].substring(lastNamespace + 1);
                        else
                            query = "Template:" + splits[1];
                    requestWikiPage(WikiInfo.SUPPORT_WIKIS.get(splits[0].toLowerCase(Locale.ROOT)), splits[0].toLowerCase(Locale.ROOT),
                            query, isTemplate, context, null, environment);
                }
            } catch (Exception e) {
                environment.getMessageSender().onError(e, "wiki", context, true);
            }
        });
        return true;
    }

    private static void requestWikiPage(
            WikiInfo wiki, String namespace, String title, boolean takeFullPage,
            MessageContext context, String searchTitle, DelegateEnvironment environment)
            throws Exception {
        PageInfo page = wiki.parsePageInfo(title, 0, namespace, environment, takeFullPage);
        StringBuilder data = new StringBuilder();
        if (page.isSearched) {
            if (page.title != null) {
                ChainMessage chain = environment.newChain(
                        environment.newQuote(context.message()),
                        environment.newText("[" + (namespace == null ? "" : namespace + ":") + title + "]不存在，" +
                                "你要查看的是否为[" + (page.prefix == null ? "" : page.prefix + ":") + page.title + "](打y确认)")
                );
                environment.getMessageSender().sendMessageAwait(context, chain, (source, next) -> {
                    try {
                        try {
                            source.recall();
                        } catch (Exception ignored) {
                        }
                        boolean accept = false;
                        for (AbstractMessage message : next.getMessages()) {
                            if (!(message instanceof TextMessage))
                                continue;
                            if (((TextMessage) message).getText().equalsIgnoreCase("y")) {
                                accept = true;
                                break;
                            }
                        }
                        if (accept)
                            requestWikiPage(page.info, page.prefix, page.title, takeFullPage, context,
                                    (namespace == null ? "" : namespace + ":") + title, environment);
                    } catch (Exception e) {
                        environment.getMessageSender().onError(e, "wiki.re_search", context, true);
                    }
                });
            } else {
                environment.getMessageSender().sendMessageRecallable(context, environment.newChain(
                        environment.newQuote(context.message()),
                        environment.newText("没有搜索到有关于[" + (namespace == null ? "" : namespace + ":") + title + "]的页面")
                ));
            }
        } else if (page.searchTitles != null) {
            data.append("对于[").append(title).append("]的搜索结果:\n");
            for (int i = 0; i < page.searchTitles.size(); i++)
                data.append(i).append(": ").append(page.searchTitles.get(i)).append("\n");
            data.append("[对本条消息引用回复数字查看详情]");
            environment.getMessageSender().sendMessageReply(context, environment.newChain(
                    environment.newQuote(context.message()),
                    environment.newText(data.toString())
            ), false, (source, reply) -> {
                String number = null;
                for (AbstractMessage message : reply.getMessages()) {
                    if (message instanceof TextMessage text) {
                        number = text.getText();
                        break;
                    }
                }
                if (number == null)
                    return;
                try {
                    int exact = Integer.parseInt(number);
                    if (exact >= page.searchTitles.size() || exact < 0)
                        return;
                    requestWikiPage(page.info, page.prefix, page.searchTitles.get(exact), takeFullPage, context,
                            (namespace == null ? "" : namespace + ":") + title, environment);
                } catch (NumberFormatException ignored) {
                } catch (Exception e) {
                    environment.getMessageSender().onError(e, "wiki.search", context, true);
                }
            });
        } else {
            if (page.redirected != null)
                data.append("(").append(page.redirected == PageInfo.RedirectType.NORMALIZED ? "标准化" : "重定向")
                        .append("[").append(page.prefix == null ? "" : page.prefix + ":").append(page.titlePast)
                        .append("] -> [").append(page.prefix == null ? "" : page.prefix + ":").append(page.title).append("])\n");
            if (searchTitle != null)
                data.append("(重搜索定向[").append(searchTitle)
                        .append("] -> [").append(page.prefix == null ? "" : page.prefix + ":").append(page.title).append("])\n");
            if (page.isRandom)
                data.append("(随机页面到[").append(page.prefix == null ? "" : page.prefix + ":").append(page.title).append("])\n");

            boolean shouldHide = false;
            if (page.url != null)
                for (String prefix : WebUtil.MIRROR.values())
                    if (page.url.contains(prefix)) {
                        shouldHide = true;
                        break;
                    }
            if (!shouldHide && page.url != null)
                data.append(page.url).append("\n");
            if (page.shortDescription != null)
                data.append(page.shortDescription);

            String st = data.toString().trim();

            if (!st.isEmpty())
                environment.getMessageSender().sendMessageRecallable(context, environment.newChain(
                        environment.newQuote(context.message()), environment.newText(st)
                ));
            if (page.imageURL != null)
                environment.getMessageSender().sendMessageRecallable(context, environment.newImage(page.imageURL));
            if (page.audioFiles != null)
                AudioSender.sendAudio(page.audioFiles, context, environment);
            if (page.infobox != null) {
                AsyncUtil.execute(() -> {
                    try {
                        File file = page.infobox.get();
                        if (file == null)
                            return;
                        environment.getMessageSender().sendMessageRecallable(
                                context, environment.newImage(file.toURI().toURL()));
                        TempFileSystem.unlockFile(file);
                    } catch (Exception e) {
                        environment.getMessageSender().onError(e, "wiki.infobox", context, false);
                    }
                });
            }
        }
    }
}
