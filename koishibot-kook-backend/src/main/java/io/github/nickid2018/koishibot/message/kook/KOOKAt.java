package io.github.nickid2018.koishibot.message.kook;

import io.github.kookybot.contract.GuildUser;
import io.github.kookybot.message.AtKt;
import io.github.kookybot.message.SelfMessage;
import io.github.nickid2018.koishibot.message.api.AtMessage;
import io.github.nickid2018.koishibot.network.ByteData;

public class KOOKAt extends AtMessage implements KOOKMessage {

    private GuildUser at;
    private SelfMessage sentMessage;

    public KOOKAt(KOOKEnvironment environment) {
        super(environment);
    }

    public KOOKAt(KOOKEnvironment environment, GuildUser at) {
        super(environment);
        this.at = at;
        this.user = new KOOKUser(environment, at);
    }

    @Override
    public void formatMessage(KOOKMessageData data) {
        data.getMentionUsers().add(at);
        data.getTexts().add(AtKt.At(at).toMarkdown());
    }

    @Override
    public void setSentMessage(SelfMessage message) {
        sentMessage = message;
        source = new KOOKMessageSource(env, message.getId(), message);
    }

    @Override
    public SelfMessage getSentMessage() {
        return sentMessage;
    }

    @Override
    public void read(ByteData buf) {
        super.read(buf);
        at = ((KOOKUser) user).getUser();
    }
}
