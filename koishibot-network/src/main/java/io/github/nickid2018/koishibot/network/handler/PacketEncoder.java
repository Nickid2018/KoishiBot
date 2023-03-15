package io.github.nickid2018.koishibot.network.handler;

import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.DataRegistry;
import io.github.nickid2018.koishibot.network.SerializableData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.IOException;

public class PacketEncoder extends MessageToByteEncoder<SerializableData> {

    private final DataRegistry registry;

    public PacketEncoder(DataRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, SerializableData msg, ByteBuf out) throws Exception {
        int packetID = registry.getPacketId(msg.getClass());
        if (packetID < 0)
            throw new IOException(
                    "Error in encoding packet (Name: %s)".formatted(msg.getClass().getName()));
        ByteData buf = new ByteData(out);
        buf.writeVarInt(packetID);
        msg.write(buf);
    }
}
