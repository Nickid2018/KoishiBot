package io.github.nickid2018.koishibot.message.api;

public interface QuoteMessage extends AbstractMessage {

    QuoteMessage fill(AbstractMessage message);

    UserInfo getReplyTo();

    default void send(GroupInfo group) {
        format().send(group);
    }

    default void send(UserInfo contact) {
        format().send(contact);
    }

    default ChainMessage format() {
        Environment environment = getEnvironment();
        return environment.newChain().fill(this, environment.newText().fillText(" "));
    }
}
