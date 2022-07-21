package io.github.nickid2018.koishibot.filter;

import io.github.nickid2018.koishibot.message.api.*;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class AntiFilter implements PostFilter {

    public static final String[] ANTI_AUTO_FILTER = new String[]{
            "[ffk]", ">anti-auto_filter<", "~防止风向操控~", "=_禁止符卡攻击_="
    };

    private final Random random = new Random();
    private final AtomicLong messageCounter = new AtomicLong(0);

    @NotNull
    @Override
    public AbstractMessage filterMessagePost(AbstractMessage input, MessageContext context, Environment environment) {
        if (input instanceof ForwardMessage || input instanceof AudioMessage || input instanceof ImageMessage)
            return input;
        if (messageCounter.getAndIncrement() % 10 == 0)
            return environment.newChain(
                    input,
                    environment.newText("\n" + ANTI_AUTO_FILTER[random.nextInt(ANTI_AUTO_FILTER.length)])
            );
        return input;
    }
}
