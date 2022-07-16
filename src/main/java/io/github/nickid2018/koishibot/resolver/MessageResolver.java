package io.github.nickid2018.koishibot.resolver;

import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import kotlin.Pair;

import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class MessageResolver {

    private final Function<String, Pair<String, Object>> resolverTrigger;

    public MessageResolver(Function<String, Pair<String, Object>> resolverTrigger) {
        this.resolverTrigger = resolverTrigger;
    }

    public MessageResolver(Pattern... patterns) {
        resolverTrigger = s -> {
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(s);
                if (matcher.find())
                    return new Pair<>(matcher.group(), pattern);
            }
            return null;
        };
    }

    public MessageResolver(String prefix) {
        String lowCased = prefix.toLowerCase(Locale.ROOT);
        resolverTrigger = s ->
                s.toLowerCase(Locale.ROOT).startsWith(lowCased) ? new Pair<>(s.substring(prefix.length()).trim(), null) : null;
    }

    public boolean groupEnabled() {
        return true;
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

    public boolean friendEnabled() {
        return true;
    }

    public boolean resolve(String segment, MessageContext context, Environment environment) {
        segment = segment.trim();
        Pair<String, Object> ret = resolverTrigger.apply(segment);
        return ret != null && resolveInternal(ret.getFirst(), context, ret.getSecond(), environment);
    }

    public abstract boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment);
}
