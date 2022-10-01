package io.github.nickid2018.koishibot.message.kook;

import io.github.kookybot.contract.GuildUser;
import io.github.kookybot.message.AtKt;
import io.github.nickid2018.koishibot.message.api.AtMessage;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class KOOKAt extends KOOKMessage implements AtMessage {

    private GuildUser at;

    public KOOKAt(KOOKEnvironment environment) {
        super(environment);
    }

    public KOOKAt(KOOKEnvironment environment, GuildUser at) {
        super(environment);
        this.at = at;
    }

    @Override
    public AtMessage fillAt(GroupInfo group, UserInfo contact) {
        at = ((KOOKUser) environment.getUser(contact.getUserId(), true)).getUser();
        return this;
    }

    @Nonnull
    @Override
    public UserInfo getUser(GroupInfo group) {
        return new KOOKUser(environment, at);
    }

    @NotNull
    @Override
    public String getId() {
        return "kook.user" + at.getId();
    }

    @Override
    public void formatMessage(KOOKMessageData data) {
        data.getMentionUsers().add(at);
        data.getTexts().add(AtKt.At(at).toMarkdown());
    }
}
