package io.github.nickid2018.koishibot.filter;

import io.github.nickid2018.koishibot.message.DelegateEnvironment;
import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.message.api.TextMessage;

import javax.annotation.Nonnull;

public class MeowPostFilter implements PostFilter {

    @Nonnull
    @Override
    public AbstractMessage filterMessagePost(AbstractMessage input, MessageContext context, DelegateEnvironment environment) {
        if (input instanceof TextMessage text)
            return environment.newText(text.getText() + "\n喵~");
        return input;
    }
}
