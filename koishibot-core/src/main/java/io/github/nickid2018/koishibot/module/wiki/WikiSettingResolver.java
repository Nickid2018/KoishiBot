package io.github.nickid2018.koishibot.module.wiki;

import io.github.nickid2018.koishibot.core.TempFileSystem;
import io.github.nickid2018.koishibot.message.DelegateEnvironment;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.ImageRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@ResolverName("wiki-settings")
@Syntax(syntax = "~wiki prefix", help = "获取目前内置的wiki连接信息")
@Syntax(syntax = "~wiki base", help = "获取目前的基础wiki")
public class WikiSettingResolver extends MessageResolver {

    public WikiSettingResolver() {
        super("~wiki");
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, DelegateEnvironment environment) {
        String[] data = key.split(" ");
        if (data.length != 1)
            return false;
        switch (data[0].toLowerCase(Locale.ROOT)) {
            case "prefix" -> {
                AsyncUtil.execute(() -> {
                    try {
                        Map<String, String> render = new HashMap<>();
                        WikiInfo.SUPPORT_WIKIS.forEach((name, info) -> render.put(name, info.getUrl()));
                        BufferedImage image = ImageRenderer.renderMap(render, "前缀", "目标");
                        File file = TempFileSystem.createTmpFile("wiki_prefix", ".png");
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            ImageIO.write(image, "png", fos);
                        }
                        environment.getMessageSender().sendMessage(context, environment.newImage(file.toURI().toURL()));
                    } catch (IOException e) {
                        environment.getMessageSender().onError(e, "wiki.prefix", context, false);
                    }
                });
                return true;
            }
            case "base" -> {
                AsyncUtil.execute(() -> environment.getMessageSender()
                        .sendMessage(context, environment.newText("目前的基础wiki为%s".formatted(WikiInfo.BASE_WIKI))));
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
