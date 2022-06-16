package io.github.nickid2018.koishibot.resolver;

import io.github.nickid2018.koishibot.Constants;
import io.github.nickid2018.koishibot.KoishiBotMain;
import io.github.nickid2018.koishibot.core.MessageInfo;
import io.github.nickid2018.koishibot.core.Settings;
import io.github.nickid2018.koishibot.util.LazyLoadValue;
import net.mamoe.mirai.message.data.*;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class HelpResolver extends MessageResolver {

    public static final Map<String, LazyLoadValue<String>> HELP_DATA = new HashMap<>();
    public static final LazyLoadValue<String> UNIVERSAL_HELP = new LazyLoadValue<>(loadMessage("helpUniversal"));

    static {
        HELP_DATA.put("translation", new LazyLoadValue<>(loadMessage("translation")));
        HELP_DATA.put("bugtracker", new LazyLoadValue<>(loadMessage("bugtracker")));
        HELP_DATA.put("info", new LazyLoadValue<>(loadMessage("info")));
        HELP_DATA.put("curseforge", new LazyLoadValue<>(loadMessage("curseforge")));
        HELP_DATA.put("bilibili", new LazyLoadValue<>(loadMessage("bilibili")));
        HELP_DATA.put("wiki", new LazyLoadValue<>(loadMessage("wiki")));
        HELP_DATA.put("latex", new LazyLoadValue<>(loadMessage("latex")));
        HELP_DATA.put("qrcode", new LazyLoadValue<>(loadMessage("qrcode")));
    }

    public HelpResolver() {
        super("~help");
    }

    @Override
    public boolean groupTempChat() {
        return true;
    }

    @Override
    public boolean needAt() {
        return true;
    }

    @Override
    public boolean resolveInternal(String key, MessageInfo info, Pattern pattern) {
        key = key.trim();
        if (key.isEmpty() || !HELP_DATA.containsKey(key)) {
            MessageChain chain = MessageUtils.newChain(new PlainText(UNIVERSAL_HELP.get()));
            info.sendMessage(chain);
        } else {
            BufferedReader reader = new BufferedReader(new StringReader(HELP_DATA.get(key).get()));
            ForwardMessageBuilder builder = new ForwardMessageBuilder(
                    Objects.requireNonNull(KoishiBotMain.INSTANCE.botKoishi.getFriend(2833231379L)));
            StringBuilder message = new StringBuilder();
            reader.lines().forEach(line -> {
                if (line.equals("#")) {
                    PlainText text = new PlainText(message.toString().trim());
                    message.delete(0, message.length());
                    builder.add(Settings.BOT_QQ, "Koishi bot", text, Constants.TIME_OF_514);
                } else
                    message.append(line).append("\n");
            });
            ForwardMessage forwardMessage = builder.build();
            info.sendMessage(forwardMessage);
        }
        return true;
    }

    private static Supplier<String> loadMessage(String path) {
        return () -> {
            try {
                return IOUtils.toString(Objects.requireNonNull(
                        HelpResolver.class.getResourceAsStream("/text/" + path + ".txt")), StandardCharsets.UTF_8);
            } catch (Exception e) {
                return "<读取帮助信息失败>";
            }
        };
    }
}
