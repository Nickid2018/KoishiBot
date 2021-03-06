package io.github.nickid2018.koishibot.resolver;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.nickid2018.koishibot.core.TempFileSystem;
import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.WebUtil;
import io.github.nickid2018.koishibot.wiki.FormatTransformer;
import io.github.nickid2018.koishibot.wiki.PageInfo;
import io.github.nickid2018.koishibot.wiki.WikiInfo;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class WikiResolver extends MessageResolver {

    public static final Pattern WIKI_PATTERN = Pattern.compile("\\[\\[.+?]{2,3}+");
    public static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).build());

    public WikiResolver() {
        super(WIKI_PATTERN);
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        key = key.substring(2, key.length() - 2);
        String finalKey = key;
        AsyncUtil.execute(() -> {
            String[] splits = finalKey.split(":", 2);
            try {
                if (splits.length == 1 || !WikiInfo.SUPPORT_WIKIS.containsKey(splits[0].toLowerCase(Locale.ROOT)))
                    requestWikiPage(WikiInfo.SUPPORT_WIKIS.get(WikiInfo.BASE_WIKI), null,
                            finalKey, context, null, environment);
                else
                    requestWikiPage(WikiInfo.SUPPORT_WIKIS.get(splits[0].toLowerCase(Locale.ROOT)), splits[0].toLowerCase(Locale.ROOT),
                            splits[1], context, null, environment);
            } catch (Exception e) {
                environment.getMessageSender().onError(e, "wiki", context, true);
            }
        });
        return true;
    }

    private static void requestWikiPage(
            WikiInfo wiki, String namespace, String title, MessageContext context, String searchTitle, Environment environment)
            throws Exception {
        PageInfo page = wiki.parsePageInfo(title, 0, namespace, environment.audioSupported());
        StringBuilder data = new StringBuilder();
        if (page.isSearched) {
            if (page.title != null) {
                ChainMessage chain = environment.newChain(
                        environment.newQuote(context.message()),
                        environment.newText("[[" + (namespace == null ? "" : namespace + ":") + title + "]]????????????" +
                                "????????????????????????[[" + (page.prefix == null ? "" : page.prefix + ":") + page.title + "]](???y??????)")
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
                            requestWikiPage(page.info, page.prefix, page.title, context,
                                    (namespace == null ? "" : namespace + ":") + title, environment);
                    } catch (Exception e) {
                        environment.getMessageSender().onError(e, "wiki.re_search", context, true);
                    }
                });
            } else {
                environment.getMessageSender().sendMessageRecallable(context, environment.newChain(
                        environment.newQuote(context.message()),
                        environment.newText("????????????????????????[[" + (namespace == null ? "" : namespace + ":") + title + "]]?????????")
                ));
            }
        } else if (page.searchTitles != null) {
            data.append("??????[[").append(title).append("]]???????????????:\n");
            for (int i = 0; i < page.searchTitles.size(); i++)
                data.append(i).append(": ").append(page.searchTitles.get(i)).append("\n");
            data.append("[?????????????????????????????????????????????]");
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
                    requestWikiPage(page.info, page.prefix, page.searchTitles.get(exact), context,
                            (namespace == null ? "" : namespace + ":") + title, environment);
                } catch (NumberFormatException ignored) {
                } catch (Exception e) {
                    environment.getMessageSender().onError(e, "wiki.search", context, true);
                }
            });
        } else {
            if (page.redirected)
                data.append("(?????????[[").append(page.prefix == null ? "" : page.prefix + ":").append(page.titlePast)
                        .append("]] -> [[").append(page.prefix == null ? "" : page.prefix + ":").append(page.title).append("]])\n");
            if (searchTitle != null)
                data.append("(???????????????[[").append(searchTitle)
                        .append("]] -> [[").append(page.prefix == null ? "" : page.prefix + ":").append(page.title).append("]])\n");
            if (page.isRandom)
                data.append("(???????????????[[").append(page.prefix == null ? "" : page.prefix + ":").append(page.title).append("]])\n");

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
            if (page.imageStream != null)
                environment.getMessageSender().sendMessageRecallable(context, environment.newImage(page.imageStream));
            if (page.audioFiles != null && context.group() != null) {
                EXECUTOR.execute(() -> {
                    try {
                        File[] audios = page.audioFiles.get();
                        for (File file : audios) {
                            Thread.sleep((FormatTransformer.VOICE_TRANSFORM_MAX_LENGTH + 10) * 1000);
                            environment.getMessageSender().sendMessage(
                                    context, environment.newAudio(context.group(), new FileInputStream(file)));
                        }
                        Stream.of(audios).forEach(TempFileSystem::unlockFile);
                    } catch (Exception e) {
                        environment.getMessageSender().onError(e, "wiki.audio", context, false);
                    }
                });
            }
            if (page.infobox != null) {
                AsyncUtil.execute(() -> {
                    try {
                        File file = page.infobox.get();
                        if (file == null)
                            return;
                        environment.getMessageSender().sendMessageRecallable(
                                context, environment.newImage(new FileInputStream(file)));
                        TempFileSystem.unlockFile(file);
                    } catch (Exception e) {
                        environment.getMessageSender().onError(e, "wiki.infobox", context, false);
                    }
                });
            }
        }
    }
}
