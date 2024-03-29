package io.github.nickid2018.koishibot.network.handler;

import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.Connection;
import io.github.nickid2018.koishibot.network.SerializableData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.IOException;
import java.util.List;

public class PacketDecoder extends ByteToMessageDecoder {

    private final Connection connection;

    public PacketDecoder(Connection connection) {
        this.connection = connection;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() == 0)
            return;
        ByteData buf = new ByteData(in);
        int id = buf.readVarInt();
        Class<? extends SerializableData> packetClass = connection.getRegistry().getDataClass(id);
        if (packetClass == null)
            throw new IOException("Bad packet (ID %d) - Unknown ID".formatted(id));
        SerializableData packet = connection.getRegistry().createData(connection, packetClass);
        packet.read(buf);
        if (buf.readableBytes() > 0)
            throw new IOException("Bad packet (ID %d, Name: %s) - Unexpected %s byte(s) at the packet tail".formatted(
                            id, packet.getClass().getName(), buf.readableBytes()));
        out.add(packet);
    }
}
