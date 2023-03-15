package io.github.nickid2018.koishibot.network;

public interface SerializableData {

    void read(ByteData buf);

    void write(ByteData buf);
}
