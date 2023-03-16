package io.github.nickid2018.koishibot.message;

import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.permission.PermissionLevel;
import io.github.nickid2018.koishibot.util.Pair;

import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class MessageResolver {

    private final Function<String, Pair<String, Object>> resolverTrigger;
    private final boolean inline;

    public MessageResolver(Function<String, Pair<String, Object>> resolverTrigger, boolean inline) {
        this.resolverTrigger = resolverTrigger;
        this.inline = inline;
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
        inline = true;
    }

    public MessageResolver(String prefix) {
        String lowCased = prefix.toLowerCase(Locale.ROOT);
        resolverTrigger = s ->
                s.toLowerCase(Locale.ROOT).startsWith(lowCased) ? new Pair<>(s.substring(prefix.length()).trim(), null) : null;
        inline = false;
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

    public final boolean isInline() {
        return inline;
    }

    public boolean friendEnabled() {
        return true;
    }

    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.TRUSTED;
    }

    public boolean resolve(String segment, MessageContext context, DelegateEnvironment environment) {
        segment = segment.trim();
        Pair<String, Object> ret = resolverTrigger.apply(segment);
        return ret != null && resolveInternal(ret.first(), context, ret.second(), environment);
    }

    public abstract boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, DelegateEnvironment environment);
}
