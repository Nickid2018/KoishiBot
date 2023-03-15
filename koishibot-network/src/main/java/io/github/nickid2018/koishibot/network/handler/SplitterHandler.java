package io.github.nickid2018.koishibot.network.handler;

import io.github.nickid2018.koishibot.network.ByteData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;

import java.util.List;

public class SplitterHandler extends ByteToMessageDecoder {

    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
        byteBuf.markReaderIndex();
        byte[] bs = new byte[3];

        for (int i = 0; i < bs.length; ++i) {
            if (!byteBuf.isReadable()) {
                byteBuf.resetReaderIndex();
                return;
            }

            bs[i] = byteBuf.readByte();
            if (bs[i] >= 0) {
                ByteData packetByteBuf = new ByteData(Unpooled.wrappedBuffer(bs));

                try {
                    int j = packetByteBuf.readVarInt();
                    if (byteBuf.readableBytes() < j) {
                        byteBuf.resetReaderIndex();
                        return;
                    }

                    list.add(byteBuf.readBytes(j));
                } finally {
                    packetByteBuf.release();
                }

                return;
            }
        }

        throw new CorruptedFrameException("length wider than 21-bit");
    }
}
