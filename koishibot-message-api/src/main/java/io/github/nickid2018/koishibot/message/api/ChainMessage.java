package io.github.nickid2018.koishibot.message.api;

import io.github.nickid2018.koishibot.network.ByteData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChainMessage extends AbstractMessage {

    protected AbstractMessage[] messages;

    public ChainMessage(Environment env) {
        super(env);
    }

    public ChainMessage fillChain(AbstractMessage... messages) {
        List<AbstractMessage> list = new ArrayList<>();
        Arrays.stream(messages).forEach(m -> {
            if (m instanceof ChainMessage chain)
                list.addAll(Arrays.asList(chain.getMessages()));
            else
                list.add(m);
        });
        this.messages = list.toArray(AbstractMessage[]::new);
        return this;
    }

    public AbstractMessage[] getMessages() {
        return messages;
    }

    @Override
    protected void readAdditional(ByteData buf) {
        int size = buf.readInt();
        messages = new AbstractMessage[size];
        for (int i = 0; i < size; i++)
            messages[i] = (AbstractMessage) buf.readSerializableData(env.getConnection().getRegistry());
    }

    @Override
    protected void writeAdditional(ByteData buf) {
        buf.writeInt(messages.length);
        for (AbstractMessage message : messages)
            buf.writeSerializableDataMultiChoice(env.getConnection().getRegistry(), message);
    }
}
