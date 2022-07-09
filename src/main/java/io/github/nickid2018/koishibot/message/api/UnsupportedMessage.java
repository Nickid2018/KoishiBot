package io.github.nickid2018.koishibot.message.api;

public class UnsupportedMessage implements AbstractMessage {

    private final Environment environment;

    public UnsupportedMessage(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public void send(UserInfo contact) {
    }

    @Override
    public void send(GroupInfo group) {
    }

    @Override
    public void recall() {
    }

    @Override
    public long getSentTime() {
        return -1;
    }

    @Override
    public MessageFrom getSource() {
        return null;
    }
}
