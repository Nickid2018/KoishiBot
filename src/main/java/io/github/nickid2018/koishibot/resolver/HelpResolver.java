package io.github.nickid2018.koishibot.resolver;

import io.github.nickid2018.koishibot.Constants;
import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.util.LazyLoadValue;
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
        HELP_DATA.put("github", new LazyLoadValue<>(loadMessage("github")));
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
    public boolean resolveInternal(String key, MessageContext context, Pattern pattern, Environment environment) {
        key = key.trim();
        if (key.isEmpty() || !HELP_DATA.containsKey(key))
            environment.getMessageSender().sendMessage(context, environment.newText(UNIVERSAL_HELP.get()));
        else {
            if (environment.forwardMessageSupported()) {
                BufferedReader reader = new BufferedReader(new StringReader(HELP_DATA.get(key).get()));
                ForwardMessage forwards = environment.newForwards();
                ContactInfo contact = environment.getUser(environment.getBotId(), false);
                StringBuilder message = new StringBuilder();
                List<MessageEntry> messageEntries = new ArrayList<>();
                reader.lines().forEach(line -> {
                    if (line.equals("#")) {
                        messageEntries.add(environment.newMessageEntry(
                                environment.getBotId(),
                                "Koishi bot",
                                environment.newText(message.toString().trim()),
                                Constants.TIME_OF_514
                        ));
                        message.delete(0, message.length());
                    } else
                        message.append(line).append("\n");
                });
                forwards.fillForwards(contact, messageEntries.toArray(new MessageEntry[0]));
                environment.getMessageSender().sendMessage(context, forwards);
            } else
                environment.getMessageSender().sendMessage(context, environment.newText("环境不支持转发信息"));
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
