package io.github.nickid2018.koishibot.filter;

import io.github.nickid2018.koishibot.message.DelegateEnvironment;
import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.MessageContext;

import javax.annotation.Nonnull;

public interface PostFilter {

    @Nonnull
    AbstractMessage filterMessagePost(AbstractMessage input, MessageContext context, DelegateEnvironment environment);
}
