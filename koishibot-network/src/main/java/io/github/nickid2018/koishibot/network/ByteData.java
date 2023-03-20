package io.github.nickid2018.koishibot.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ByteProcessor;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import javax.annotation.Nullable;
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
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

@SuppressWarnings("unused")
public class ByteData extends ByteBuf {

    public static final short MAX_STRING_LENGTH = 32767;
    public static final int MAX_COMPONENT_STRING_LENGTH = 262144;
    private final ByteBuf source;

    public ByteData(ByteBuf paramByteBuf) {
        this.source = paramByteBuf;
    }

    public static int getVarIntSize(int paramInt) {
        for (byte b = 1; b < 5; b++) {
            if ((paramInt & -1 << b * 7) == 0)
                return b;
        }
        return 5;
    }

    public static int getVarLongSize(long paramLong) {
        for (byte b = 1; b < 10; b++) {
            if ((paramLong & -1L << b * 7) == 0L)
                return b;
        }
        return 10;
    }

    public SerializableData readSerializableData(Connection connection) {
        int i = readVarInt();
        Class<? extends SerializableData> clazz = connection.getRegistry().getDataClass(i);
        if (clazz == null)
            throw new DecoderException("Unknown data id " + i);
        SerializableData data = connection.getRegistry().createData(connection, clazz);
        data.read(this);
        return data;
    }

    @SuppressWarnings("unchecked")
    public <T extends SerializableData> T readSerializableDataOrNull(Connection connection, Class<T> clazz) {
        int i = readVarInt();
        Class<? extends SerializableData> clazzReal = connection.getRegistry().getDataClass(i);
        if (clazz != clazzReal)
            return null;
        T data = (T) connection.getRegistry().createData(connection, clazzReal);
        data.read(this);
        return data;
    }

    @SuppressWarnings("unchecked")
    public <T extends SerializableData> T readSerializableData(Connection connection, Class<T> clazz) {
        T data = (T) connection.getRegistry().createData(connection, clazz);
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

    public <T, C extends Collection<T>> C readCollection(IntFunction<C> paramIntFunction,
                                                         Function<ByteData, T> paramFunction) {
        int i = readVarInt();
        C collection = paramIntFunction.apply(i);
        for (byte b = 0; b < i; b++)
            collection.add(paramFunction.apply(this));
        return collection;
    }

    public <T> void writeCollection(Collection<T> paramCollection, BiConsumer<ByteData, T> paramBiConsumer) {
        writeVarInt(paramCollection.size());
        for (T obj : paramCollection)
            paramBiConsumer.accept(this, obj);
    }

    public <T> List<T> readList(Function<ByteData, T> paramFunction) {
        return readCollection(ArrayList::new, paramFunction);
    }

    public IntList readIntIdList() {
        int i = readVarInt();
        IntArrayList intArrayList = new IntArrayList();
        for (byte b = 0; b < i; b++)
            intArrayList.add(readVarInt());
        return intArrayList;
    }

    public void writeIntIdList(IntList paramIntList) {
        writeVarInt(paramIntList.size());
        paramIntList.forEach(this::writeVarInt);
    }

    public <K, V, M extends Map<K, V>> M readMap(IntFunction<M> paramIntFunction,
                                                 Function<ByteData, K> paramFunction, Function<ByteData, V> paramFunction1) {
        int i = readVarInt();
        M map = paramIntFunction.apply(i);
        for (byte b = 0; b < i; b++) {
            K k = paramFunction.apply(this);
            V v = paramFunction1.apply(this);
            map.put(k, v);
        }
        return map;
    }

    public <K, V> Map<K, V> readMap(Function<ByteData, K> paramFunction,
                                    Function<ByteData, V> paramFunction1) {
        return readMap(HashMap::new, paramFunction, paramFunction1);
    }

    public <K, V> void writeMap(Map<K, V> paramMap, BiConsumer<ByteData, K> paramBiConsumer,
                                BiConsumer<ByteData, V> paramBiConsumer1) {
        writeVarInt(paramMap.size());
        paramMap.forEach((paramObject1, paramObject2) -> {
            paramBiConsumer.accept(this, paramObject1);
            paramBiConsumer1.accept(this, paramObject2);
        });
    }

    public void readWithCount(Consumer<ByteData> paramConsumer) {
        int i = readVarInt();
        for (byte b = 0; b < i; b++)
            paramConsumer.accept(this);
    }

    public <T> void writeOptional(Optional<T> paramOptional, BiConsumer<ByteData, T> paramBiConsumer) {
        if (paramOptional.isPresent()) {
            writeBoolean(true);
            paramBiConsumer.accept(this, paramOptional.get());
        } else {
            writeBoolean(false);
        }
    }

    public <T> Optional<T> readOptional(Function<ByteData, T> paramFunction) {
        return readBoolean() ? Optional.of(paramFunction.apply(this)) : Optional.empty();
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

    public ByteData writeVarIntArray(int[] paramArrayOfint) {
        writeVarInt(paramArrayOfint.length);
        for (int i : paramArrayOfint)
            writeVarInt(i);
        return this;
    }

    public int[] readVarIntArray() {
        return readVarIntArray(readableBytes());
    }

    public int[] readVarIntArray(int paramInt) {
        int i = readVarInt();
        if (i > paramInt)
            throw new DecoderException("VarIntArray with size " + i + " is bigger than allowed " + paramInt);
        int[] arrayOfInt = new int[i];
        for (byte b = 0; b < arrayOfInt.length; b++)
            arrayOfInt[b] = readVarInt();
        return arrayOfInt;
    }

    public ByteData writeLongArray(long[] paramArrayOflong) {
        writeVarInt(paramArrayOflong.length);
        for (long l : paramArrayOflong)
            writeLong(l);
        return this;
    }

    public long[] readLongArray() {
        return readLongArray(null);
    }

    public long[] readLongArray(@Nullable long[] paramArrayOflong) {
        return readLongArray(paramArrayOflong, readableBytes() / 8);
    }

    public long[] readLongArray(@Nullable long[] paramArrayOflong, int paramInt) {
        int i = readVarInt();
        if (paramArrayOflong == null || paramArrayOflong.length != i) {
            if (i > paramInt)
                throw new DecoderException("LongArray with size " + i + " is bigger than allowed " + paramInt);
            paramArrayOflong = new long[i];
        }
        for (byte b = 0; b < paramArrayOflong.length; b++)
            paramArrayOflong[b] = readLong();
        return paramArrayOflong;
    }

    public <T extends Enum<T>> T readEnum(Class<T> paramClass) {
        return paramClass.getEnumConstants()[readVarInt()];
    }

    public ByteData writeEnum(Enum<?> paramEnum) {
        return writeVarInt(paramEnum.ordinal());
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

    public long readVarLong() {
        long l = 0L;
        byte b = 0;
        while (true) {
            byte b1 = readByte();
            l |= (long) (b1 & Byte.MAX_VALUE) << b++ * 7;
            if (b > 10)
                throw new RuntimeException("VarLong too big");
            if ((b1 & 0x80) != 128)
                return l;
        }
    }

    public ByteData writeUUID(UUID paramUUID) {
        writeLong(paramUUID.getMostSignificantBits());
        writeLong(paramUUID.getLeastSignificantBits());
        return this;
    }

    public UUID readUUID() {
        return new UUID(readLong(), readLong());
    }

    public ByteData writeVarInt(int paramInt) {
        while (true) {
            if ((paramInt & 0xFFFFFF80) == 0) {
                writeByte(paramInt);
                return this;
            }
            writeByte(paramInt & 0x7F | 0x80);
            paramInt >>>= 7;
        }
    }

    public ByteData writeVarLong(long paramLong) {
        while (true) {
            if ((paramLong & 0xFFFFFFFFFFFFFF80L) == 0L) {
                writeByte((int) paramLong);
                return this;
            }
            writeByte((int) (paramLong & 0x7FL) | 0x80);
            paramLong >>>= 7L;
        }
    }

    public String readString() {
        return readString(32767);
    }

    public String readString(int paramInt) {
        int i = readVarInt();
        if (i > paramInt * 4)
            throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + i
                    + " > " + paramInt * 4 + ")");
        if (i < 0)
            throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
        String str = toString(readerIndex(), i, StandardCharsets.UTF_8);
        readerIndex(readerIndex() + i);
        if (str.length() > paramInt)
            throw new DecoderException(
                    "The received string length is longer than maximum allowed (" + i + " > " + paramInt + ")");
        return str;
    }

    public ByteData writeString(String paramString) {
        return writeString(paramString, 32767);
    }

    public ByteData writeString(String paramString, int paramInt) {
        byte[] arrayOfByte = paramString.getBytes(StandardCharsets.UTF_8);
        if (arrayOfByte.length > paramInt)
            throw new EncoderException(
                    "String too big (was " + arrayOfByte.length + " bytes encoded, max " + paramInt + ")");
        writeVarInt(arrayOfByte.length);
        writeBytes(arrayOfByte);
        return this;
    }

    public Date readDate() {
        return new Date(readLong());
    }

    public ByteData writeDate(Date paramDate) {
        writeLong(paramDate.getTime());
        return this;
    }

    public BitSet readBitSet() {
        return BitSet.valueOf(readLongArray());
    }

    public void writeBitSet(BitSet paramBitSet) {
        writeLongArray(paramBitSet.toLongArray());
    }

    public int capacity() {
        return this.source.capacity();
    }

    public ByteBuf capacity(int paramInt) {
        return this.source.capacity(paramInt);
    }

    public int maxCapacity() {
        return this.source.maxCapacity();
    }

    public ByteBufAllocator alloc() {
        return this.source.alloc();
    }

    @SuppressWarnings("deprecation")
    public ByteOrder order() {
        return this.source.order();
    }

    @SuppressWarnings("deprecation")
    public ByteBuf order(ByteOrder paramByteOrder) {
        return this.source.order(paramByteOrder);
    }

    public ByteBuf unwrap() {
        return this.source.unwrap();
    }

    public boolean isDirect() {
        return this.source.isDirect();
    }

    public boolean isReadOnly() {
        return this.source.isReadOnly();
    }

    public ByteBuf asReadOnly() {
        return this.source.asReadOnly();
    }

    public int readerIndex() {
        return this.source.readerIndex();
    }

    public ByteBuf readerIndex(int paramInt) {
        return this.source.readerIndex(paramInt);
    }

    public int writerIndex() {
        return this.source.writerIndex();
    }

    public ByteBuf writerIndex(int paramInt) {
        return this.source.writerIndex(paramInt);
    }

    public ByteBuf setIndex(int paramInt1, int paramInt2) {
        return this.source.setIndex(paramInt1, paramInt2);
    }

    public int readableBytes() {
        return this.source.readableBytes();
    }

    public int writableBytes() {
        return this.source.writableBytes();
    }

    public int maxWritableBytes() {
        return this.source.maxWritableBytes();
    }

    public boolean isReadable() {
        return this.source.isReadable();
    }

    public boolean isReadable(int paramInt) {
        return this.source.isReadable(paramInt);
    }

    public boolean isWritable() {
        return this.source.isWritable();
    }

    public boolean isWritable(int paramInt) {
        return this.source.isWritable(paramInt);
    }

    public ByteBuf clear() {
        return this.source.clear();
    }

    public ByteBuf markReaderIndex() {
        return this.source.markReaderIndex();
    }

    public ByteBuf resetReaderIndex() {
        return this.source.resetReaderIndex();
    }

    public ByteBuf markWriterIndex() {
        return this.source.markWriterIndex();
    }

    public ByteBuf resetWriterIndex() {
        return this.source.resetWriterIndex();
    }

    public ByteBuf discardReadBytes() {
        return this.source.discardReadBytes();
    }

    public ByteBuf discardSomeReadBytes() {
        return this.source.discardSomeReadBytes();
    }

    public ByteBuf ensureWritable(int paramInt) {
        return this.source.ensureWritable(paramInt);
    }

    public int ensureWritable(int paramInt, boolean paramBoolean) {
        return this.source.ensureWritable(paramInt, paramBoolean);
    }

    public boolean getBoolean(int paramInt) {
        return this.source.getBoolean(paramInt);
    }

    public byte getByte(int paramInt) {
        return this.source.getByte(paramInt);
    }

    public short getUnsignedByte(int paramInt) {
        return this.source.getUnsignedByte(paramInt);
    }

    public short getShort(int paramInt) {
        return this.source.getShort(paramInt);
    }

    public short getShortLE(int paramInt) {
        return this.source.getShortLE(paramInt);
    }

    public int getUnsignedShort(int paramInt) {
        return this.source.getUnsignedShort(paramInt);
    }

    public int getUnsignedShortLE(int paramInt) {
        return this.source.getUnsignedShortLE(paramInt);
    }

    public int getMedium(int paramInt) {
        return this.source.getMedium(paramInt);
    }

    public int getMediumLE(int paramInt) {
        return this.source.getMediumLE(paramInt);
    }

    public int getUnsignedMedium(int paramInt) {
        return this.source.getUnsignedMedium(paramInt);
    }

    public int getUnsignedMediumLE(int paramInt) {
        return this.source.getUnsignedMediumLE(paramInt);
    }

    public int getInt(int paramInt) {
        return this.source.getInt(paramInt);
    }

    public int getIntLE(int paramInt) {
        return this.source.getIntLE(paramInt);
    }

    public long getUnsignedInt(int paramInt) {
        return this.source.getUnsignedInt(paramInt);
    }

    public long getUnsignedIntLE(int paramInt) {
        return this.source.getUnsignedIntLE(paramInt);
    }

    public long getLong(int paramInt) {
        return this.source.getLong(paramInt);
    }

    public long getLongLE(int paramInt) {
        return this.source.getLongLE(paramInt);
    }

    public char getChar(int paramInt) {
        return this.source.getChar(paramInt);
    }

    public float getFloat(int paramInt) {
        return this.source.getFloat(paramInt);
    }

    public double getDouble(int paramInt) {
        return this.source.getDouble(paramInt);
    }

    public ByteBuf getBytes(int paramInt, ByteBuf paramByteBuf) {
        return this.source.getBytes(paramInt, paramByteBuf);
    }

    public ByteBuf getBytes(int paramInt1, ByteBuf paramByteBuf, int paramInt2) {
        return this.source.getBytes(paramInt1, paramByteBuf, paramInt2);
    }

    public ByteBuf getBytes(int paramInt1, ByteBuf paramByteBuf, int paramInt2, int paramInt3) {
        return this.source.getBytes(paramInt1, paramByteBuf, paramInt2, paramInt3);
    }

    public ByteBuf getBytes(int paramInt, byte[] paramArrayOfbyte) {
        return this.source.getBytes(paramInt, paramArrayOfbyte);
    }

    public ByteBuf getBytes(int paramInt1, byte[] paramArrayOfbyte, int paramInt2, int paramInt3) {
        return this.source.getBytes(paramInt1, paramArrayOfbyte, paramInt2, paramInt3);
    }

    public ByteBuf getBytes(int paramInt, ByteBuffer paramByteBuffer) {
        return this.source.getBytes(paramInt, paramByteBuffer);
    }

    public ByteBuf getBytes(int paramInt1, OutputStream paramOutputStream, int paramInt2) throws IOException {
        return this.source.getBytes(paramInt1, paramOutputStream, paramInt2);
    }

    public int getBytes(int paramInt1, GatheringByteChannel paramGatheringByteChannel, int paramInt2)
            throws IOException {
        return this.source.getBytes(paramInt1, paramGatheringByteChannel, paramInt2);
    }

    public int getBytes(int paramInt1, FileChannel paramFileChannel, long paramLong, int paramInt2) throws IOException {
        return this.source.getBytes(paramInt1, paramFileChannel, paramLong, paramInt2);
    }

    public CharSequence getCharSequence(int paramInt1, int paramInt2, Charset paramCharset) {
        return this.source.getCharSequence(paramInt1, paramInt2, paramCharset);
    }

    public ByteBuf setBoolean(int paramInt, boolean paramBoolean) {
        return this.source.setBoolean(paramInt, paramBoolean);
    }

    public ByteBuf setByte(int paramInt1, int paramInt2) {
        return this.source.setByte(paramInt1, paramInt2);
    }

    public ByteBuf setShort(int paramInt1, int paramInt2) {
        return this.source.setShort(paramInt1, paramInt2);
    }

    public ByteBuf setShortLE(int paramInt1, int paramInt2) {
        return this.source.setShortLE(paramInt1, paramInt2);
    }

    public ByteBuf setMedium(int paramInt1, int paramInt2) {
        return this.source.setMedium(paramInt1, paramInt2);
    }

    public ByteBuf setMediumLE(int paramInt1, int paramInt2) {
        return this.source.setMediumLE(paramInt1, paramInt2);
    }

    public ByteBuf setInt(int paramInt1, int paramInt2) {
        return this.source.setInt(paramInt1, paramInt2);
    }

    public ByteBuf setIntLE(int paramInt1, int paramInt2) {
        return this.source.setIntLE(paramInt1, paramInt2);
    }

    public ByteBuf setLong(int paramInt, long paramLong) {
        return this.source.setLong(paramInt, paramLong);
    }

    public ByteBuf setLongLE(int paramInt, long paramLong) {
        return this.source.setLongLE(paramInt, paramLong);
    }

    public ByteBuf setChar(int paramInt1, int paramInt2) {
        return this.source.setChar(paramInt1, paramInt2);
    }

    public ByteBuf setFloat(int paramInt, float paramFloat) {
        return this.source.setFloat(paramInt, paramFloat);
    }

    public ByteBuf setDouble(int paramInt, double paramDouble) {
        return this.source.setDouble(paramInt, paramDouble);
    }

    public ByteBuf setBytes(int paramInt, ByteBuf paramByteBuf) {
        return this.source.setBytes(paramInt, paramByteBuf);
    }

    public ByteBuf setBytes(int paramInt1, ByteBuf paramByteBuf, int paramInt2) {
        return this.source.setBytes(paramInt1, paramByteBuf, paramInt2);
    }

    public ByteBuf setBytes(int paramInt1, ByteBuf paramByteBuf, int paramInt2, int paramInt3) {
        return this.source.setBytes(paramInt1, paramByteBuf, paramInt2, paramInt3);
    }

    public ByteBuf setBytes(int paramInt, byte[] paramArrayOfbyte) {
        return this.source.setBytes(paramInt, paramArrayOfbyte);
    }

    public ByteBuf setBytes(int paramInt1, byte[] paramArrayOfbyte, int paramInt2, int paramInt3) {
        return this.source.setBytes(paramInt1, paramArrayOfbyte, paramInt2, paramInt3);
    }

    public ByteBuf setBytes(int paramInt, ByteBuffer paramByteBuffer) {
        return this.source.setBytes(paramInt, paramByteBuffer);
    }

    public int setBytes(int paramInt1, InputStream paramInputStream, int paramInt2) throws IOException {
        return this.source.setBytes(paramInt1, paramInputStream, paramInt2);
    }

    public int setBytes(int paramInt1, ScatteringByteChannel paramScatteringByteChannel, int paramInt2)
            throws IOException {
        return this.source.setBytes(paramInt1, paramScatteringByteChannel, paramInt2);
    }

    public int setBytes(int paramInt1, FileChannel paramFileChannel, long paramLong, int paramInt2) throws IOException {
        return this.source.setBytes(paramInt1, paramFileChannel, paramLong, paramInt2);
    }

    public ByteBuf setZero(int paramInt1, int paramInt2) {
        return this.source.setZero(paramInt1, paramInt2);
    }

    public int setCharSequence(int paramInt, CharSequence paramCharSequence, Charset paramCharset) {
        return this.source.setCharSequence(paramInt, paramCharSequence, paramCharset);
    }

    public boolean readBoolean() {
        return this.source.readBoolean();
    }

    public byte readByte() {
        return this.source.readByte();
    }

    public short readUnsignedByte() {
        return this.source.readUnsignedByte();
    }

    public short readShort() {
        return this.source.readShort();
    }

    public short readShortLE() {
        return this.source.readShortLE();
    }

    public int readUnsignedShort() {
        return this.source.readUnsignedShort();
    }

    public int readUnsignedShortLE() {
        return this.source.readUnsignedShortLE();
    }

    public int readMedium() {
        return this.source.readMedium();
    }

    public int readMediumLE() {
        return this.source.readMediumLE();
    }

    public int readUnsignedMedium() {
        return this.source.readUnsignedMedium();
    }

    public int readUnsignedMediumLE() {
        return this.source.readUnsignedMediumLE();
    }

    public int readInt() {
        return this.source.readInt();
    }

    public int readIntLE() {
        return this.source.readIntLE();
    }

    public long readUnsignedInt() {
        return this.source.readUnsignedInt();
    }

    public long readUnsignedIntLE() {
        return this.source.readUnsignedIntLE();
    }

    public long readLong() {
        return this.source.readLong();
    }

    public long readLongLE() {
        return this.source.readLongLE();
    }

    public char readChar() {
        return this.source.readChar();
    }

    public float readFloat() {
        return this.source.readFloat();
    }

    public double readDouble() {
        return this.source.readDouble();
    }

    public ByteBuf readBytes(int paramInt) {
        return this.source.readBytes(paramInt);
    }

    public ByteBuf readSlice(int paramInt) {
        return this.source.readSlice(paramInt);
    }

    public ByteBuf readRetainedSlice(int paramInt) {
        return this.source.readRetainedSlice(paramInt);
    }

    public ByteBuf readBytes(ByteBuf paramByteBuf) {
        return this.source.readBytes(paramByteBuf);
    }

    public ByteBuf readBytes(ByteBuf paramByteBuf, int paramInt) {
        return this.source.readBytes(paramByteBuf, paramInt);
    }

    public ByteBuf readBytes(ByteBuf paramByteBuf, int paramInt1, int paramInt2) {
        return this.source.readBytes(paramByteBuf, paramInt1, paramInt2);
    }

    public ByteBuf readBytes(byte[] paramArrayOfbyte) {
        return this.source.readBytes(paramArrayOfbyte);
    }

    public ByteBuf readBytes(byte[] paramArrayOfbyte, int paramInt1, int paramInt2) {
        return this.source.readBytes(paramArrayOfbyte, paramInt1, paramInt2);
    }

    public ByteBuf readBytes(ByteBuffer paramByteBuffer) {
        return this.source.readBytes(paramByteBuffer);
    }

    public ByteBuf readBytes(OutputStream paramOutputStream, int paramInt) throws IOException {
        return this.source.readBytes(paramOutputStream, paramInt);
    }

    public int readBytes(GatheringByteChannel paramGatheringByteChannel, int paramInt) throws IOException {
        return this.source.readBytes(paramGatheringByteChannel, paramInt);
    }

    public CharSequence readCharSequence(int paramInt, Charset paramCharset) {
        return this.source.readCharSequence(paramInt, paramCharset);
    }

    public int readBytes(FileChannel paramFileChannel, long paramLong, int paramInt) throws IOException {
        return this.source.readBytes(paramFileChannel, paramLong, paramInt);
    }

    public ByteBuf skipBytes(int paramInt) {
        return this.source.skipBytes(paramInt);
    }

    public ByteBuf writeBoolean(boolean paramBoolean) {
        return this.source.writeBoolean(paramBoolean);
    }

    public ByteBuf writeByte(int paramInt) {
        return this.source.writeByte(paramInt);
    }

    public ByteBuf writeShort(int paramInt) {
        return this.source.writeShort(paramInt);
    }

    public ByteBuf writeShortLE(int paramInt) {
        return this.source.writeShortLE(paramInt);
    }

    public ByteBuf writeMedium(int paramInt) {
        return this.source.writeMedium(paramInt);
    }

    public ByteBuf writeMediumLE(int paramInt) {
        return this.source.writeMediumLE(paramInt);
    }

    public ByteBuf writeInt(int paramInt) {
        return this.source.writeInt(paramInt);
    }

    public ByteBuf writeIntLE(int paramInt) {
        return this.source.writeIntLE(paramInt);
    }

    public ByteBuf writeLong(long paramLong) {
        return this.source.writeLong(paramLong);
    }

    public ByteBuf writeLongLE(long paramLong) {
        return this.source.writeLongLE(paramLong);
    }

    public ByteBuf writeChar(int paramInt) {
        return this.source.writeChar(paramInt);
    }

    public ByteBuf writeFloat(float paramFloat) {
        return this.source.writeFloat(paramFloat);
    }

    public ByteBuf writeDouble(double paramDouble) {
        return this.source.writeDouble(paramDouble);
    }

    public ByteBuf writeBytes(ByteBuf paramByteBuf) {
        return this.source.writeBytes(paramByteBuf);
    }

    public ByteBuf writeBytes(ByteBuf paramByteBuf, int paramInt) {
        return this.source.writeBytes(paramByteBuf, paramInt);
    }

    public ByteBuf writeBytes(ByteBuf paramByteBuf, int paramInt1, int paramInt2) {
        return this.source.writeBytes(paramByteBuf, paramInt1, paramInt2);
    }

    public ByteBuf writeBytes(byte[] paramArrayOfbyte) {
        return this.source.writeBytes(paramArrayOfbyte);
    }

    public ByteBuf writeBytes(byte[] paramArrayOfbyte, int paramInt1, int paramInt2) {
        return this.source.writeBytes(paramArrayOfbyte, paramInt1, paramInt2);
    }

    public ByteBuf writeBytes(ByteBuffer paramByteBuffer) {
        return this.source.writeBytes(paramByteBuffer);
    }

    public int writeBytes(InputStream paramInputStream, int paramInt) throws IOException {
        return this.source.writeBytes(paramInputStream, paramInt);
    }

    public int writeBytes(ScatteringByteChannel paramScatteringByteChannel, int paramInt) throws IOException {
        return this.source.writeBytes(paramScatteringByteChannel, paramInt);
    }

    public int writeBytes(FileChannel paramFileChannel, long paramLong, int paramInt) throws IOException {
        return this.source.writeBytes(paramFileChannel, paramLong, paramInt);
    }

    public ByteBuf writeZero(int paramInt) {
        return this.source.writeZero(paramInt);
    }

    public int writeCharSequence(CharSequence paramCharSequence, Charset paramCharset) {
        return this.source.writeCharSequence(paramCharSequence, paramCharset);
    }

    public int indexOf(int paramInt1, int paramInt2, byte paramByte) {
        return this.source.indexOf(paramInt1, paramInt2, paramByte);
    }

    public int bytesBefore(byte paramByte) {
        return this.source.bytesBefore(paramByte);
    }

    public int bytesBefore(int paramInt, byte paramByte) {
        return this.source.bytesBefore(paramInt, paramByte);
    }

    public int bytesBefore(int paramInt1, int paramInt2, byte paramByte) {
        return this.source.bytesBefore(paramInt1, paramInt2, paramByte);
    }

    public int forEachByte(ByteProcessor paramByteProcessor) {
        return this.source.forEachByte(paramByteProcessor);
    }

    public int forEachByte(int paramInt1, int paramInt2, ByteProcessor paramByteProcessor) {
        return this.source.forEachByte(paramInt1, paramInt2, paramByteProcessor);
    }

    public int forEachByteDesc(ByteProcessor paramByteProcessor) {
        return this.source.forEachByteDesc(paramByteProcessor);
    }

    public int forEachByteDesc(int paramInt1, int paramInt2, ByteProcessor paramByteProcessor) {
        return this.source.forEachByteDesc(paramInt1, paramInt2, paramByteProcessor);
    }

    public ByteBuf copy() {
        return this.source.copy();
    }

    public ByteBuf copy(int paramInt1, int paramInt2) {
        return this.source.copy(paramInt1, paramInt2);
    }

    public ByteBuf slice() {
        return this.source.slice();
    }

    public ByteBuf retainedSlice() {
        return this.source.retainedSlice();
    }

    public ByteBuf slice(int paramInt1, int paramInt2) {
        return this.source.slice(paramInt1, paramInt2);
    }

    public ByteBuf retainedSlice(int paramInt1, int paramInt2) {
        return this.source.retainedSlice(paramInt1, paramInt2);
    }

    public ByteBuf duplicate() {
        return this.source.duplicate();
    }

    public ByteBuf retainedDuplicate() {
        return this.source.retainedDuplicate();
    }

    public int nioBufferCount() {
        return this.source.nioBufferCount();
    }

    public ByteBuffer nioBuffer() {
        return this.source.nioBuffer();
    }

    public ByteBuffer nioBuffer(int paramInt1, int paramInt2) {
        return this.source.nioBuffer(paramInt1, paramInt2);
    }

    public ByteBuffer internalNioBuffer(int paramInt1, int paramInt2) {
        return this.source.internalNioBuffer(paramInt1, paramInt2);
    }

    public ByteBuffer[] nioBuffers() {
        return this.source.nioBuffers();
    }

    public ByteBuffer[] nioBuffers(int paramInt1, int paramInt2) {
        return this.source.nioBuffers(paramInt1, paramInt2);
    }

    public boolean hasArray() {
        return this.source.hasArray();
    }

    public byte[] array() {
        return this.source.array();
    }

    public int arrayOffset() {
        return this.source.arrayOffset();
    }

    public boolean hasMemoryAddress() {
        return this.source.hasMemoryAddress();
    }

    public long memoryAddress() {
        return this.source.memoryAddress();
    }

    public String toString(Charset paramCharset) {
        return this.source.toString(paramCharset);
    }

    public String toString(int paramInt1, int paramInt2, Charset paramCharset) {
        return this.source.toString(paramInt1, paramInt2, paramCharset);
    }

    public int hashCode() {
        return this.source.hashCode();
    }

    public boolean equals(Object paramObject) {
        return this.source.equals(paramObject);
    }

    public int compareTo(ByteBuf paramByteBuf) {
        return this.source.compareTo(paramByteBuf);
    }

    public String toString() {
        return this.source.toString();
    }

    public ByteBuf retain(int paramInt) {
        return this.source.retain(paramInt);
    }

    public ByteBuf retain() {
        return this.source.retain();
    }

    public ByteBuf touch() {
        return this.source.touch();
    }

    public ByteBuf touch(Object paramObject) {
        return this.source.touch(paramObject);
    }

    public int refCnt() {
        return this.source.refCnt();
    }

    public boolean release() {
        return this.source.release();
    }

    public boolean release(int paramInt) {
        return this.source.release(paramInt);
    }
}