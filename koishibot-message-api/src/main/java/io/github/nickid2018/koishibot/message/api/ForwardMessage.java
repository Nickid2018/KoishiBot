package io.github.nickid2018.koishibot.message.api;

import io.github.nickid2018.koishibot.network.ByteData;

public class ForwardMessage extends AbstractMessage {

    protected ContactInfo group;
    protected MessageEntry[] entries;

    public ForwardMessage(Environment env) {
        super(env);
    }

    public ForwardMessage fillForwards(ContactInfo group, MessageEntry... entries) {
        this.group = group;
        this.entries = entries;
        return this;
    }

    @Override
    protected void readAdditional(ByteData buf) {
        if (buf.readBoolean())
            group = (ContactInfo) buf.readSerializableData(env.getConnection());
        int len = buf.readVarInt();
        entries = new MessageEntry[len];
        for (int i = 0; i < len; i++)
            entries[i] = buf.readSerializableData(env.getConnection(), MessageEntry.class);
    }

    @Override
    protected void writeAdditional(ByteData buf) {
        buf.writeBoolean(group != null);
        if (group != null)
            buf.writeSerializableDataMultiChoice(env.getConnection().getRegistry(), group);
        buf.writeVarInt(entries.length);
        for (MessageEntry entry : entries)
            buf.writeSerializableData(entry);
    }
}
