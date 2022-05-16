package io.github.nickid2018.koishibot.resolver;

import io.github.nickid2018.koishibot.KoishiBotMain;
import io.github.nickid2018.koishibot.core.ErrorRecord;
import io.github.nickid2018.koishibot.core.MessageInfo;
import io.github.nickid2018.koishibot.core.MessageManager;
import io.github.nickid2018.koishibot.core.Settings;
import io.github.nickid2018.koishibot.wiki.PageInfo;
import io.github.nickid2018.koishibot.wiki.WikiInfo;
import net.mamoe.mirai.message.data.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class WikiResolver extends MessageResolver {

    public static final Pattern WIKI_PATTERN = Pattern.compile("\\[\\[.+?]]");

//    public static final Map<String, WikiInfo> SUPPORT_WIKIS = new HashMap<>();
//
//    static {
//        SUPPORT_WIKIS.put("mcw", new WikiInfo("https://minecraft.fandom.com/zh/api.php?"));
//        SUPPORT_WIKIS.put("thw", new WikiInfo("https://thwiki.cc/api.php?"));
//        SUPPORT_WIKIS.put("acw", new WikiInfo("https://assassinscreed.fandom.com/zh/api.php?"));
//        Map<String, String> moeHeader = new HashMap<>();
//        moeHeader.put("accept", "*/*");
//        moeHeader.put("accept-encoding", "gzip, deflate");
//        moeHeader.put("accept-language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7,en-GB;q=0.6");
//        moeHeader.put("content-type", "application/json");
//        moeHeader.put("user-agent",
//                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)" +
//                        " Chrome/96.0.4664.110 Safari/537.36 Edg/96.0.1054.62");
//        SUPPORT_WIKIS.put("moe", new WikiInfo("https://zh.moegirl.org.cn/api.php?", moeHeader));
//        /* Mirror Website!! */
//        SUPPORT_WIKIS.put("wzh", new WikiInfo("https://zh.wikipedia.ahau.cf/w/api.php?"));
//    }

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
                    requestWikiPage(Settings.BASE_WIKI, finalKey, info);
                else
                    requestWikiPage(splits[0].toLowerCase(Locale.ROOT), splits[1], info);
            } catch (Exception e) {
                MessageManager.onError(e, "wiki", info, true);
            }
        });
        return true;
    }

    private static void requestWikiPage(String namespace, String title, MessageInfo info) throws Exception {
        WikiInfo wiki = Settings.SUPPORT_WIKIS.get(namespace);
        PageInfo page = wiki.parsePageInfo(title, 0);
        StringBuilder data = new StringBuilder();
        if (page.isSearched) {
            if (page.title != null) {
                MessageChain chain = MessageUtils.newChain(
                        new QuoteReply(info.data),
                        new PlainText("[[" + title + "]]不存在，你要查看的是否为[[" + page.title + "]](打y确认)")
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
                            requestWikiPage(namespace, page.title, info);
                    } catch (Exception e) {
                        MessageManager.onError(e, "wiki.re_search", info, true);
                    }
                });
            } else {
                MessageChain chain = MessageUtils.newChain(
                        new QuoteReply(info.data),
                        new PlainText("没有搜索到有关于[[" + title + "]]的页面")
                );
                info.sendMessageWithQuote(chain);
            }
        } else {
            if (page.redirected)
                data.append("(重定向[[").append(page.titlePast).append("]] -> [[").append(page.title).append("]])\n");
            if (!Settings.HIDE_WIKIS.contains(namespace))
                data.append("(页面URL:").append(page.url).append(")\n");
            data.append(page.shortDescription);

            MessageChain chain = MessageUtils.newChain(
                    new QuoteReply(info.data),
                    new PlainText(data.toString())
            );
            info.sendMessageWithQuote(chain);
        }
    }
}
