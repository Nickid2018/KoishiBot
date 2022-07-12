package io.github.nickid2018.koishibot.resolver;

import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;

import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class MessageResolver {

    private final Function<String, Object> resolverTrigger;

    public MessageResolver(Function<String, Object> resolverTrigger) {
        this.resolverTrigger = resolverTrigger;
    }

    public MessageResolver(Pattern... patterns) {
        resolverTrigger = s -> {
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(s);
                if (matcher.find())
                    return pattern;
            }
            return null;
        };
    }

    public MessageResolver(String prefix) {
        String lowCased = prefix.toLowerCase(Locale.ROOT);
        resolverTrigger = s -> s.toLowerCase(Locale.ROOT).startsWith(lowCased) ? prefix : null;
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

    public boolean resolve(String segment, MessageContext context, Environment environment) {
        segment = segment.trim();
        Object ret = resolverTrigger.apply(segment);
        return ret != null && resolveInternal(segment, context, ret, environment);
    }

    public abstract boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment);
}
