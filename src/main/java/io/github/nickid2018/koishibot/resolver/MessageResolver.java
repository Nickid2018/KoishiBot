package io.github.nickid2018.koishibot.resolver;

import io.github.nickid2018.koishibot.core.MessageInfo;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class MessageResolver {

    private final Pattern[] regex;
    private final String prefix;

    public MessageResolver(Pattern... pattern) {
        regex = pattern;
        prefix = null;
    }

    public MessageResolver(String prefix) {
        regex = null;
        this.prefix = prefix.toLowerCase(Locale.ROOT);
    }

    public boolean needAt() {
        return false;
    }

    public boolean groupTempChat() {
        return false;
    }

    public boolean strangerChat() {
        return false;
    }

    public boolean resolve(String segment, MessageInfo info) {
        segment = segment.trim();
        if (prefix != null && segment.toLowerCase(Locale.ROOT).startsWith(prefix))
            return resolveInternal(segment.substring(prefix.length()), info, null);
        else if (regex != null) {
            for (Pattern pattern : regex) {
                Matcher matcher = pattern.matcher(segment);
                if (matcher.find())
                    return resolveInternal(segment.substring(matcher.start(), matcher.end()), info, pattern);
            }
        }
        return false;
    }

    public abstract boolean resolveInternal(String key, MessageInfo info, Pattern pattern);
}
