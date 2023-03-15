package io.github.nickid2018.koishibot.message.action;

import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.SerializableData;
import io.github.nickid2018.koishibot.util.Either;

public class SendMessageAction implements SerializableData {

    private final Environment env;

    public AbstractMessage message;
    public Either<UserInfo, GroupInfo> target;

    public SendMessageAction(Environment env) {
        this.env = env;
    }

    @Override
    public void read(ByteData buf) {
        message = buf.readSerializableData(env.getConnection().getRegistry(), AbstractMessage.class);
        if (buf.readBoolean())
            target = Either.left(buf.readSerializableData(env.getConnection().getRegistry(), UserInfo.class));
        else
            target = Either.right(buf.readSerializableData(env.getConnection().getRegistry(), GroupInfo.class));
    }

    @Override
    public void write(ByteData buf) {
        buf.writeSerializableData(message);
        if (target.isLeft()) {
            buf.writeBoolean(true);
            buf.writeSerializableData(target.left());
        } else {
            buf.writeBoolean(false);
            buf.writeSerializableData(target.right());
        }
    }
}
