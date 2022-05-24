package io.github.nickid2018.koishibot.resolver;

import io.github.nickid2018.koishibot.KoishiBotMain;
import io.github.nickid2018.koishibot.core.MessageInfo;
import io.github.nickid2018.koishibot.core.MessageManager;
import io.github.nickid2018.koishibot.core.Settings;
import io.github.nickid2018.koishibot.wiki.PageInfo;
import io.github.nickid2018.koishibot.wiki.WikiInfo;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.ExternalResource;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class WikiResolver extends MessageResolver {

    public static final Pattern WIKI_PATTERN = Pattern.compile("\\[\\[.+?]]");

    public WikiResolver() {
        super(WIKI_PATTERN);
    }

    @Override
    public boolean needAt() {
        return false;
    }

    @Override
    public boolean groupOnly() {
        return false;
    }

    @Override
    public boolean resolveInternal(String key, MessageInfo info) {
        key = key.substring(2, key.length() - 2);
        String finalKey = key;
        KoishiBotMain.INSTANCE.executor.execute(() -> {
            String[] splits = finalKey.split(":", 2);
            try {
                if (splits.length == 1 || !Settings.SUPPORT_WIKIS.containsKey(splits[0].toLowerCase(Locale.ROOT)))
                    requestWikiPage(Settings.SUPPORT_WIKIS.get(Settings.BASE_WIKI), null, finalKey, info, null);
                else
                    requestWikiPage(Settings.SUPPORT_WIKIS.get(splits[0].toLowerCase(Locale.ROOT)), splits[0].toLowerCase(Locale.ROOT),
                            splits[1], info, null);
            } catch (Exception e) {
                MessageManager.onError(e, "wiki", info, true);
            }
        });
        return true;
    }

    private static void requestWikiPage(WikiInfo wiki, String namespace, String title, MessageInfo info, String searchTitle)
            throws Exception {
        PageInfo page = wiki.parsePageInfo(title, 0, namespace);
        StringBuilder data = new StringBuilder();
        if (page.isSearched) {
            if (page.title != null) {
                MessageChain chain = MessageUtils.newChain(
                        new QuoteReply(info.data),
                        new PlainText("[[" + (namespace == null ? "" : namespace + ":") + title + "]]不存在，" +
                                "你要查看的是否为[[" + (page.prefix == null ? "" : page.prefix + ":") + page.title + "]](打y确认)")
                );
                info.sendMessageAwait(chain, (sourceData, newInfo) -> {
                    try {
                        sourceData.receipt.recall();
                        boolean accept = false;
                        for (SingleMessage content : newInfo.data) {
                            if (!(content instanceof PlainText))
                                continue;
                            if (((PlainText) content).component1().equalsIgnoreCase("y")) {
                                accept = true;
                                break;
                            }
                        }
                        if (accept)
                            requestWikiPage(page.info, page.prefix, page.title, info,
                                    (namespace == null ? "" : namespace + ":") + title);
                    } catch (Exception e) {
                        MessageManager.onError(e, "wiki.re_search", info, true);
                    }
                });
            } else {
                MessageChain chain = MessageUtils.newChain(
                        new QuoteReply(info.data),
                        new PlainText("没有搜索到有关于[[" + (namespace == null ? "" : namespace + ":") + title + "]]的页面")
                );
                info.sendMessageWithQuote(chain);
            }
        } else {
            if (page.redirected)
                data.append("(重定向[[").append(page.prefix == null ? "" : page.prefix + ":").append(page.titlePast)
                        .append("]] -> [[").append(page.prefix == null ? "" : page.prefix + ":").append(page.title).append("]])\n");
            if (searchTitle != null)
                data.append("(重搜索定向[[").append(searchTitle)
                        .append("]] -> [[").append(page.prefix == null ? "" : page.prefix + ":").append(page.title).append("]])\n");
            if (page.isRandom)
                data.append("(随机页面到[[").append(page.prefix == null ? "" : page.prefix + ":").append(page.title).append("]])\n");

            boolean shouldHide = false;
            if (page.url != null)
                for (String prefix : Settings.MIRROR.values())
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
                info.sendMessageWithQuote(MessageUtils.newChain(
                        new QuoteReply(info.data),
                        new PlainText(st)
                ));

            if (page.imageStream != null)
                info.sendMessageWithQuote(Contact.uploadImage(
                        KoishiBotMain.INSTANCE.botKoishi.getAsFriend(), page.imageStream));
            if (page.audioFiles != null && info.group != null) {
                KoishiBotMain.INSTANCE.executor.execute(() -> {
                    try {
                        File[] audios = page.audioFiles.get();
                        for (File file : audios) {
                            Thread.sleep(60_000);
                            ExternalResource resource = ExternalResource.create(file);
                            Audio audio = info.uploadAudio(resource);
                            info.sendMessage(audio);
                            resource.close();
                        }
                    } catch (Exception e) {
                        MessageManager.onError(e, "wiki.audio", info, false);
                    }
                });
            }
        }
    }
}
