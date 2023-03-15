package io.github.nickid2018.koishibot.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.DecoderException;
import io.netty.util.ByteProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings("unused")
public record ByteData(ByteBuf buf) {

    public static int getVarIntSize(int paramInt) {
        for (byte b = 1; b < 5; b++) {
            if ((paramInt & -1 << b * 7) == 0)
                return b;
        }
        return 5;
    }

    public int readVarInt() {
        int i = 0;
        byte b = 0;
        while (true) {
            byte b1 = readByte();
            i |= (b1 & Byte.MAX_VALUE) << b++ * 7;
            if (b > 5)
                throw new RuntimeException("VarInt too big");
            if ((b1 & 0x80) != 128)
                return i;
        }
    }

    public ByteData writeVarInt(int i) {
        while (true) {
            if ((i & 0xFFFFFF80) == 0) {
                writeByte(i);
                return this;
            }
            writeByte(i & 0x7F | 0x80);
            i >>>= 7;
        }
    }

    public String readString() {
        return buf.readCharSequence(readVarInt(), StandardCharsets.UTF_8).toString();
    }

    public ByteData writeString(String s) {
        writeVarInt(s.length());
        buf.writeCharSequence(s, StandardCharsets.UTF_8);
        return this;
    }

    public ByteData writeLongArray(long[] array) {
        writeVarInt(array.length);
        for (long l : array)
            writeLong(l);
        return this;
    }

    public long[] readLongArray() {
        return readLongArray(null);
    }

    public long[] readLongArray(long[] array) {
        return readLongArray(array, readableBytes() / 8);
    }

    public long[] readLongArray(long[] array, int paramInt) {
        int i = readVarInt();
        if (array == null || array.length != i) {
            if (i > paramInt)
                throw new DecoderException("LongArray with size " + i + " is bigger than allowed " + paramInt);
            array = new long[i];
        }
        for (byte b = 0; b < array.length; b++)
            array[b] = readLong();
        return array;
    }

    public BitSet readBitSet() {
        return BitSet.valueOf(readLongArray());
    }

    public void writeBitSet(BitSet bitset) {
        writeLongArray(bitset.toLongArray());
    }

    public SerializableData readSerializableData(DataRegistry registry) {
        int i = readVarInt();
        Class<? extends SerializableData> clazz = registry.getDataClass(i);
        if (clazz == null)
            throw new DecoderException("Unknown data id " + i);
        SerializableData data = registry.createData(clazz);
        data.read(this);
        return data;
    }

    @SuppressWarnings("unchecked")
    public <T extends SerializableData> T readSerializableDataOrNull(DataRegistry registry, Class<T> clazz) {
        int i = readVarInt();
        Class<? extends SerializableData> clazzReal = registry.getDataClass(i);
        if (clazz != clazzReal)
            return null;
        T data = (T) registry.createData(clazzReal);
        data.read(this);
        return data;
    }

    @SuppressWarnings("unchecked")
    public <T extends SerializableData> T readSerializableData(DataRegistry registry, Class<T> clazz) {
        T data = (T) registry.createData(clazz);
        data.read(this);
        return data;
    }

    public void writeSerializableData(SerializableData data) {
        data.write(this);
    }

    public void writeSerializableDataMultiChoice(DataRegistry registry, SerializableData data) {
        writeVarInt(registry.getPacketId(data.getClass()));
        data.write(this);
    }

    public void writeSerializableDataOrNull(DataRegistry registry, SerializableData data) {
        writeSerializableDataMultiChoice(registry, Objects.requireNonNullElse(data, NullData.INSTANCE));
    }

    public ByteData writeUUID(UUID paramUUID) {
        writeLong(paramUUID.getMostSignificantBits());
        writeLong(paramUUID.getLeastSignificantBits());
        return this;
    }

    public UUID readUUID() {
        return new UUID(readLong(), readLong());
    }

    public byte[] readByteArray() {
        return readByteArray(readableBytes());
    }

    public ByteData writeByteArray(byte[] paramArrayOfbyte) {
        writeVarInt(paramArrayOfbyte.length);
        writeBytes(paramArrayOfbyte);
        return this;
    }

    public byte[] readByteArray(int paramInt) {
        int i = readVarInt();
        if (i > paramInt)
            throw new DecoderException("ByteArray with size " + i + " is bigger than allowed " + paramInt);
        byte[] arrayOfByte = new byte[i];
        readBytes(arrayOfByte);
        return arrayOfByte;
    }

    public int capacity() {
        return buf.capacity();
    }

    public ByteBuf capacity(int paramInt) {
        return buf.capacity(paramInt);
    }

    public int maxCapacity() {
        return buf.maxCapacity();
    }

    public ByteBufAllocator alloc() {
        return buf.alloc();
    }

    public ByteOrder order() {
        return buf.order();
    }

    public ByteBuf order(ByteOrder paramByteOrder) {
        return buf.order(paramByteOrder);
    }

    public ByteBuf unwrap() {
        return buf.unwrap();
    }

    public boolean isDirect() {
        return buf.isDirect();
    }

    public boolean isReadOnly() {
        return buf.isReadOnly();
    }

    public ByteBuf asReadOnly() {
        return buf.asReadOnly();
    }

    public int readerIndex() {
        return buf.readerIndex();
    }

    public ByteBuf readerIndex(int paramInt) {
        return buf.readerIndex(paramInt);
    }

    public int writerIndex() {
        return buf.writerIndex();
    }

    public ByteBuf writerIndex(int paramInt) {
        return buf.writerIndex(paramInt);
    }

    public ByteBuf setIndex(int paramInt1, int paramInt2) {
        return buf.setIndex(paramInt1, paramInt2);
    }

    public int readableBytes() {
        return buf.readableBytes();
    }

    public int writableBytes() {
        return buf.writableBytes();
    }

    public int maxWritableBytes() {
        return buf.maxWritableBytes();
    }

    public boolean isReadable() {
        return buf.isReadable();
    }

    public boolean isReadable(int paramInt) {
        return buf.isReadable(paramInt);
    }

    public boolean isWritable() {
        return buf.isWritable();
    }

    public boolean isWritable(int paramInt) {
        return buf.isWritable(paramInt);
    }

    public ByteBuf clear() {
        return buf.clear();
    }

    public ByteBuf markReaderIndex() {
        return buf.markReaderIndex();
    }

    public ByteBuf resetReaderIndex() {
        return buf.resetReaderIndex();
    }

    public ByteBuf markWriterIndex() {
        return buf.markWriterIndex();
    }

    public ByteBuf resetWriterIndex() {
        return buf.resetWriterIndex();
    }

    public ByteBuf discardReadBytes() {
        return buf.discardReadBytes();
    }

    public ByteBuf discardSomeReadBytes() {
        return buf.discardSomeReadBytes();
    }

    public ByteBuf ensureWritable(int paramInt) {
        return buf.ensureWritable(paramInt);
    }

    public int ensureWritable(int paramInt, boolean paramBoolean) {
        return buf.ensureWritable(paramInt, paramBoolean);
    }

    public boolean getBoolean(int paramInt) {
        return buf.getBoolean(paramInt);
    }

    public byte getByte(int paramInt) {
        return buf.getByte(paramInt);
    }

    public short getUnsignedByte(int paramInt) {
        return buf.getUnsignedByte(paramInt);
    }

    public short getShort(int paramInt) {
        return buf.getShort(paramInt);
    }

    public short getShortLE(int paramInt) {
        return buf.getShortLE(paramInt);
    }

    public int getUnsignedShort(int paramInt) {
        return buf.getUnsignedShort(paramInt);
    }

    public int getUnsignedShortLE(int paramInt) {
        return buf.getUnsignedShortLE(paramInt);
    }

    public int getMedium(int paramInt) {
        return buf.getMedium(paramInt);
    }

    public int getMediumLE(int paramInt) {
        return buf.getMediumLE(paramInt);
    }

    public int getUnsignedMedium(int paramInt) {
        return buf.getUnsignedMedium(paramInt);
    }

    public int getUnsignedMediumLE(int paramInt) {
        return buf.getUnsignedMediumLE(paramInt);
    }

    public int getInt(int paramInt) {
        return buf.getInt(paramInt);
    }

    public int getIntLE(int paramInt) {
        return buf.getIntLE(paramInt);
    }

    public long getUnsignedInt(int paramInt) {
        return buf.getUnsignedInt(paramInt);
    }

    public long getUnsignedIntLE(int paramInt) {
        return buf.getUnsignedIntLE(paramInt);
    }

    public long getLong(int paramInt) {
        return buf.getLong(paramInt);
    }

    public long getLongLE(int paramInt) {
        return buf.getLongLE(paramInt);
    }

    public char getChar(int paramInt) {
        return buf.getChar(paramInt);
    }

    public float getFloat(int paramInt) {
        return buf.getFloat(paramInt);
    }

    public double getDouble(int paramInt) {
        return buf.getDouble(paramInt);
    }

    public ByteBuf getBytes(int paramInt, ByteBuf paramByteBuf) {
        return buf.getBytes(paramInt, paramByteBuf);
    }

    public ByteBuf getBytes(int paramInt1, ByteBuf paramByteBuf, int paramInt2) {
        return buf.getBytes(paramInt1, paramByteBuf, paramInt2);
    }

    public ByteBuf getBytes(int paramInt1, ByteBuf paramByteBuf, int paramInt2, int paramInt3) {
        return buf.getBytes(paramInt1, paramByteBuf, paramInt2, paramInt3);
    }

    public ByteBuf getBytes(int paramInt, byte[] paramArrayOfbyte) {
        return buf.getBytes(paramInt, paramArrayOfbyte);
    }

    public ByteBuf getBytes(int paramInt1, byte[] paramArrayOfbyte, int paramInt2, int paramInt3) {
        return buf.getBytes(paramInt1, paramArrayOfbyte, paramInt2, paramInt3);
    }

    public ByteBuf getBytes(int paramInt, ByteBuffer paramByteBuffer) {
        return buf.getBytes(paramInt, paramByteBuffer);
    }

    public ByteBuf getBytes(int paramInt1, OutputStream paramOutputStream, int paramInt2) throws IOException {
        return buf.getBytes(paramInt1, paramOutputStream, paramInt2);
    }

    public int getBytes(int paramInt1, GatheringByteChannel paramGatheringByteChannel, int paramInt2)
            throws IOException {
        return buf.getBytes(paramInt1, paramGatheringByteChannel, paramInt2);
    }

    public int getBytes(int paramInt1, FileChannel paramFileChannel, long paramLong, int paramInt2) throws IOException {
        return buf.getBytes(paramInt1, paramFileChannel, paramLong, paramInt2);
    }

    public CharSequence getCharSequence(int paramInt1, int paramInt2, Charset paramCharset) {
        return buf.getCharSequence(paramInt1, paramInt2, paramCharset);
    }

    public ByteBuf setBoolean(int paramInt, boolean paramBoolean) {
        return buf.setBoolean(paramInt, paramBoolean);
    }

    public ByteBuf setByte(int paramInt1, int paramInt2) {
        return buf.setByte(paramInt1, paramInt2);
    }

    public ByteBuf setShort(int paramInt1, int paramInt2) {
        return buf.setShort(paramInt1, paramInt2);
    }

    public ByteBuf setShortLE(int paramInt1, int paramInt2) {
        return buf.setShortLE(paramInt1, paramInt2);
    }

    public ByteBuf setMedium(int paramInt1, int paramInt2) {
        return buf.setMedium(paramInt1, paramInt2);
    }

    public ByteBuf setMediumLE(int paramInt1, int paramInt2) {
        return buf.setMediumLE(paramInt1, paramInt2);
    }

    public ByteBuf setInt(int paramInt1, int paramInt2) {
        return buf.setInt(paramInt1, paramInt2);
    }

    public ByteBuf setIntLE(int paramInt1, int paramInt2) {
        return buf.setIntLE(paramInt1, paramInt2);
    }

    public ByteBuf setLong(int paramInt, long paramLong) {
        return buf.setLong(paramInt, paramLong);
    }

    public ByteBuf setLongLE(int paramInt, long paramLong) {
        return buf.setLongLE(paramInt, paramLong);
    }

    public ByteBuf setChar(int paramInt1, int paramInt2) {
        return buf.setChar(paramInt1, paramInt2);
    }

    public ByteBuf setFloat(int paramInt, float paramFloat) {
        return buf.setFloat(paramInt, paramFloat);
    }

    public ByteBuf setDouble(int paramInt, double paramDouble) {
        return buf.setDouble(paramInt, paramDouble);
    }

    public ByteBuf setBytes(int paramInt, ByteBuf paramByteBuf) {
        return buf.setBytes(paramInt, paramByteBuf);
    }

    public ByteBuf setBytes(int paramInt1, ByteBuf paramByteBuf, int paramInt2) {
        return buf.setBytes(paramInt1, paramByteBuf, paramInt2);
    }

    public ByteBuf setBytes(int paramInt1, ByteBuf paramByteBuf, int paramInt2, int paramInt3) {
        return buf.setBytes(paramInt1, paramByteBuf, paramInt2, paramInt3);
    }

    public ByteBuf setBytes(int paramInt, byte[] paramArrayOfbyte) {
        return buf.setBytes(paramInt, paramArrayOfbyte);
    }

    public ByteBuf setBytes(int paramInt1, byte[] paramArrayOfbyte, int paramInt2, int paramInt3) {
        return buf.setBytes(paramInt1, paramArrayOfbyte, paramInt2, paramInt3);
    }

    public ByteBuf setBytes(int paramInt, ByteBuffer paramByteBuffer) {
        return buf.setBytes(paramInt, paramByteBuffer);
    }

    public int setBytes(int paramInt1, InputStream paramInputStream, int paramInt2) throws IOException {
        return buf.setBytes(paramInt1, paramInputStream, paramInt2);
    }

    public int setBytes(int paramInt1, ScatteringByteChannel paramScatteringByteChannel, int paramInt2)
            throws IOException {
        return buf.setBytes(paramInt1, paramScatteringByteChannel, paramInt2);
    }

    public int setBytes(int paramInt1, FileChannel paramFileChannel, long paramLong, int paramInt2) throws IOException {
        return buf.setBytes(paramInt1, paramFileChannel, paramLong, paramInt2);
    }

    public ByteBuf setZero(int paramInt1, int paramInt2) {
        return buf.setZero(paramInt1, paramInt2);
    }

    public int setCharSequence(int paramInt, CharSequence paramCharSequence, Charset paramCharset) {
        return buf.setCharSequence(paramInt, paramCharSequence, paramCharset);
    }

    public boolean readBoolean() {
        return buf.readBoolean();
    }

    public byte readByte() {
        return buf.readByte();
    }

    public short readUnsignedByte() {
        return buf.readUnsignedByte();
    }

    public short readShort() {
        return buf.readShort();
    }

    public short readShortLE() {
        return buf.readShortLE();
    }

    public int readUnsignedShort() {
        return buf.readUnsignedShort();
    }

    public int readUnsignedShortLE() {
        return buf.readUnsignedShortLE();
    }

    public int readMedium() {
        return buf.readMedium();
    }

    public int readMediumLE() {
        return buf.readMediumLE();
    }

    public int readUnsignedMedium() {
        return buf.readUnsignedMedium();
    }

    public int readUnsignedMediumLE() {
        return buf.readUnsignedMediumLE();
    }

    public int readInt() {
        return buf.readInt();
    }

    public int readIntLE() {
        return buf.readIntLE();
    }

    public long readUnsignedInt() {
        return buf.readUnsignedInt();
    }

    public long readUnsignedIntLE() {
        return buf.readUnsignedIntLE();
    }

    public long readLong() {
        return buf.readLong();
    }

    public long readLongLE() {
        return buf.readLongLE();
    }

    public char readChar() {
        return buf.readChar();
    }

    public float readFloat() {
        return buf.readFloat();
    }

    public double readDouble() {
        return buf.readDouble();
    }

    public ByteBuf readBytes(int paramInt) {
        return buf.readBytes(paramInt);
    }

    public ByteBuf readSlice(int paramInt) {
        return buf.readSlice(paramInt);
    }

    public ByteBuf readRetainedSlice(int paramInt) {
        return buf.readRetainedSlice(paramInt);
    }

    public ByteBuf readBytes(ByteBuf paramByteBuf) {
        return buf.readBytes(paramByteBuf);
    }

    public ByteBuf readBytes(ByteBuf paramByteBuf, int paramInt) {
        return buf.readBytes(paramByteBuf, paramInt);
    }

    public ByteBuf readBytes(ByteBuf paramByteBuf, int paramInt1, int paramInt2) {
        return buf.readBytes(paramByteBuf, paramInt1, paramInt2);
    }

    public ByteBuf readBytes(byte[] paramArrayOfbyte) {
        return buf.readBytes(paramArrayOfbyte);
    }

    public ByteBuf readBytes(byte[] paramArrayOfbyte, int paramInt1, int paramInt2) {
        return buf.readBytes(paramArrayOfbyte, paramInt1, paramInt2);
    }

    public ByteBuf readBytes(ByteBuffer paramByteBuffer) {
        return buf.readBytes(paramByteBuffer);
    }

    public ByteBuf readBytes(OutputStream paramOutputStream, int paramInt) throws IOException {
        return buf.readBytes(paramOutputStream, paramInt);
    }

    public int readBytes(GatheringByteChannel paramGatheringByteChannel, int paramInt) throws IOException {
        return buf.readBytes(paramGatheringByteChannel, paramInt);
    }

    public CharSequence readCharSequence(int paramInt, Charset paramCharset) {
        return buf.readCharSequence(paramInt, paramCharset);
    }

    public int readBytes(FileChannel paramFileChannel, long paramLong, int paramInt) throws IOException {
        return buf.readBytes(paramFileChannel, paramLong, paramInt);
    }

    public ByteBuf skipBytes(int paramInt) {
        return buf.skipBytes(paramInt);
    }

    public ByteBuf writeBoolean(boolean paramBoolean) {
        return buf.writeBoolean(paramBoolean);
    }

    public ByteBuf writeByte(int paramInt) {
        return buf.writeByte(paramInt);
    }

    public ByteBuf writeShort(int paramInt) {
        return buf.writeShort(paramInt);
    }

    public ByteBuf writeShortLE(int paramInt) {
        return buf.writeShortLE(paramInt);
    }

    public ByteBuf writeMedium(int paramInt) {
        return buf.writeMedium(paramInt);
    }

    public ByteBuf writeMediumLE(int paramInt) {
        return buf.writeMediumLE(paramInt);
    }

    public ByteBuf writeInt(int paramInt) {
        return buf.writeInt(paramInt);
    }

    public ByteBuf writeIntLE(int paramInt) {
        return buf.writeIntLE(paramInt);
    }

    public ByteBuf writeLong(long paramLong) {
        return buf.writeLong(paramLong);
    }

    public ByteBuf writeLongLE(long paramLong) {
        return buf.writeLongLE(paramLong);
    }

    public ByteBuf writeChar(int paramInt) {
        return buf.writeChar(paramInt);
    }

    public ByteBuf writeFloat(float paramFloat) {
        return buf.writeFloat(paramFloat);
    }

    public ByteBuf writeDouble(double paramDouble) {
        return buf.writeDouble(paramDouble);
    }

    public ByteBuf writeBytes(ByteBuf paramByteBuf) {
        return buf.writeBytes(paramByteBuf);
    }

    public ByteBuf writeBytes(ByteBuf paramByteBuf, int paramInt) {
        return buf.writeBytes(paramByteBuf, paramInt);
    }

    public ByteBuf writeBytes(ByteBuf paramByteBuf, int paramInt1, int paramInt2) {
        return buf.writeBytes(paramByteBuf, paramInt1, paramInt2);
    }

    public ByteBuf writeBytes(byte[] paramArrayOfbyte) {
        return buf.writeBytes(paramArrayOfbyte);
    }

    public ByteBuf writeBytes(byte[] paramArrayOfbyte, int paramInt1, int paramInt2) {
        return buf.writeBytes(paramArrayOfbyte, paramInt1, paramInt2);
    }

    public ByteBuf writeBytes(ByteBuffer paramByteBuffer) {
        return buf.writeBytes(paramByteBuffer);
    }

    public int writeBytes(InputStream paramInputStream, int paramInt) throws IOException {
        return buf.writeBytes(paramInputStream, paramInt);
    }

    public int writeBytes(ScatteringByteChannel paramScatteringByteChannel, int paramInt) throws IOException {
        return buf.writeBytes(paramScatteringByteChannel, paramInt);
    }

    public int writeBytes(FileChannel paramFileChannel, long paramLong, int paramInt) throws IOException {
        return buf.writeBytes(paramFileChannel, paramLong, paramInt);
    }

    public ByteBuf writeZero(int paramInt) {
        return buf.writeZero(paramInt);
    }

    public int writeCharSequence(CharSequence paramCharSequence, Charset paramCharset) {
        return buf.writeCharSequence(paramCharSequence, paramCharset);
    }

    public int indexOf(int paramInt1, int paramInt2, byte paramByte) {
        return buf.indexOf(paramInt1, paramInt2, paramByte);
    }

    public int bytesBefore(byte paramByte) {
        return buf.bytesBefore(paramByte);
    }

    public int bytesBefore(int paramInt, byte paramByte) {
        return buf.bytesBefore(paramInt, paramByte);
    }

    public int bytesBefore(int paramInt1, int paramInt2, byte paramByte) {
        return buf.bytesBefore(paramInt1, paramInt2, paramByte);
    }

    public int forEachByte(ByteProcessor paramByteProcessor) {
        return buf.forEachByte(paramByteProcessor);
    }

    public int forEachByte(int paramInt1, int paramInt2, ByteProcessor paramByteProcessor) {
        return buf.forEachByte(paramInt1, paramInt2, paramByteProcessor);
    }

    public int forEachByteDesc(ByteProcessor paramByteProcessor) {
        return buf.forEachByteDesc(paramByteProcessor);
    }

    public int forEachByteDesc(int paramInt1, int paramInt2, ByteProcessor paramByteProcessor) {
        return buf.forEachByteDesc(paramInt1, paramInt2, paramByteProcessor);
    }

    public ByteBuf copy() {
        return buf.copy();
    }

    public ByteBuf copy(int paramInt1, int paramInt2) {
        return buf.copy(paramInt1, paramInt2);
    }

    public ByteBuf slice() {
        return buf.slice();
    }

    public ByteBuf retainedSlice() {
        return buf.retainedSlice();
    }

    public ByteBuf slice(int paramInt1, int paramInt2) {
        return buf.slice(paramInt1, paramInt2);
    }

    public ByteBuf retainedSlice(int paramInt1, int paramInt2) {
        return buf.retainedSlice(paramInt1, paramInt2);
    }

    public ByteBuf duplicate() {
        return buf.duplicate();
    }

    public ByteBuf retainedDuplicate() {
        return buf.retainedDuplicate();
    }

    public int nioBufferCount() {
        return buf.nioBufferCount();
    }

    public ByteBuffer nioBuffer() {
        return buf.nioBuffer();
    }

    public ByteBuffer nioBuffer(int paramInt1, int paramInt2) {
        return buf.nioBuffer(paramInt1, paramInt2);
    }

    public ByteBuffer internalNioBuffer(int paramInt1, int paramInt2) {
        return buf.internalNioBuffer(paramInt1, paramInt2);
    }

    public ByteBuffer[] nioBuffers() {
        return buf.nioBuffers();
    }

    public ByteBuffer[] nioBuffers(int paramInt1, int paramInt2) {
        return buf.nioBuffers(paramInt1, paramInt2);
    }

    public boolean hasArray() {
        return buf.hasArray();
    }

    public byte[] array() {
        return buf.array();
    }

    public int arrayOffset() {
        return buf.arrayOffset();
    }

    public boolean hasMemoryAddress() {
        return buf.hasMemoryAddress();
    }

    public long memoryAddress() {
        return buf.memoryAddress();
    }

    public String toString(Charset paramCharset) {
        return buf.toString(paramCharset);
    }

    public String toString(int paramInt1, int paramInt2, Charset paramCharset) {
        return buf.toString(paramInt1, paramInt2, paramCharset);
    }

    public int hashCode() {
        return buf.hashCode();
    }

    public boolean equals(Object paramObject) {
        return buf.equals(paramObject);
    }

    public int compareTo(ByteBuf paramByteBuf) {
        return buf.compareTo(paramByteBuf);
    }

    public String toString() {
        return buf.toString();
    }

    public ByteBuf retain(int paramInt) {
        return buf.retain(paramInt);
    }

    public ByteBuf retain() {
        return buf.retain();
    }

    public ByteBuf touch() {
        return buf.touch();
    }

    public ByteBuf touch(Object paramObject) {
        return buf.touch(paramObject);
    }

    public int refCnt() {
        return buf.refCnt();
    }

    public boolean release() {
        return buf.release();
    }

    public boolean release(int paramInt) {
        return buf.release(paramInt);
    }
}
