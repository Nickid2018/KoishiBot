package io.github.nickid2018.koishibot.message.kook;

import io.github.kookybot.message.AtHere;
import io.github.kookybot.message.AtKt;
import io.github.kookybot.message.Message;
import io.github.kookybot.message.MessageComponent;
import io.github.nickid2018.koishibot.message.api.AtMessage;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.util.value.Either;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Objects;

public class KOOKAt extends KOOKMessage implements AtMessage {

    private MessageComponent at;

    public KOOKAt(KOOKEnvironment environment) {
        super(environment);
    }

    public KOOKAt(KOOKEnvironment environment, MessageComponent at) {
        super(environment);
        this.at = at;
    }

    @Override
    public AtMessage fillAt(GroupInfo group, UserInfo contact) {
        String id = contact.getUserId();
        if (id.equals(environment.getBotId()))
            at = AtHere.INSTANCE;
        else
            at = AtKt.At(Objects.requireNonNull(environment.getSelf().getGuildUser(
                    group.getGroupId().substring(10), contact.getUserId().substring(9))));
        return this;
    }

    @Nonnull
    @Override
    public UserInfo getUser(GroupInfo group) {
        return new KOOKUser(environment, environment.getSelf().getGuildUser(group.getGroupId().substring(10), getId().substring(9)),
                true);
    }

    @NotNull
    @Override
    public String getId() {
        String markdown = at.toMarkdown();
        return "kook.user" + markdown.substring(5, markdown.length() - 5);
    }

    @Override
    public Either<Message, MessageComponent> getKOOKMessage() {
        return Either.right(at);
    }
}
