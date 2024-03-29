package io.github.nickid2018.koishibot.filter;

import io.github.nickid2018.koishibot.message.DelegateEnvironment;
import io.github.nickid2018.koishibot.message.api.ChainMessage;
import io.github.nickid2018.koishibot.message.api.MessageContext;

import javax.annotation.Nullable;

public interface PreFilter {

    @Nullable
    ChainMessage filterMessagePre(ChainMessage input, MessageContext context, DelegateEnvironment environment);
}
