package io.github.nickid2018.koishibot.module.mc;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/*
 * Packet Protocol: https://wiki.vg/Raknet_Protocol
 */
public record MCBEServerPing(InetSocketAddress address) {

    public static final long MAGIC_HIGH = 0x00ffff00fefefefeL;
    public static final long MAGIC_LOW = 0xfdfdfdfd12345678L;
    public static final int TIMEOUT = 10000;

    public Map<String, String> fetchData() throws IOException {
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(TIMEOUT);

        ByteArrayOutputStream payloadStream = new ByteArrayOutputStream();
        DataOutputStream dataPayload = new DataOutputStream(payloadStream);

        dataPayload.writeByte(0x1); // Packet ID 0x1 (Unconnected Ping, Raknet Protocol)
        dataPayload.writeLong(System.currentTimeMillis()); // Current time
        dataPayload.writeLong(MAGIC_HIGH); // Magic Number high bits
        dataPayload.writeLong(MAGIC_LOW); // Magic Number low bits
        dataPayload.writeLong(0L); // Client GUID, 0

        DatagramPacket dp = new DatagramPacket(payloadStream.toByteArray(), payloadStream.size(), address);
        socket.send(dp);

        byte[] receive = new byte[2048];
        DatagramPacket rec = new DatagramPacket(receive, 2048);
        socket.receive(rec);
        socket.close();

        ByteArrayInputStream responseStream = new ByteArrayInputStream(receive, 0, rec.getLength());
        DataInputStream dataResponse = new DataInputStream(responseStream);

        byte packetID = dataResponse.readByte();
        if (packetID != 0x1c)
            throw new IOException("Invalid packet ID: expect 28, but found " + packetID);

        Map<String,  String> data = new HashMap<>();
        data.put("ping", String.valueOf(System.currentTimeMillis() - dataResponse.readLong())); // time
        dataResponse.readLong(); // Server GUID, ignored

        long magicHigh = dataResponse.readLong(); // Magic Number high bits
        if (magicHigh != MAGIC_HIGH)
            throw new IOException("Invalid Magic Number: expect 00ffff00fefefefe(high bits) , but found "
                    + Long.toHexString(packetID));

        long magicLow = dataResponse.readLong(); // Magic Number low bits
        if (magicLow != MAGIC_LOW)
            throw new IOException("Invalid Magic Number: expect fdfdfdfd12345678(low bits) , but found "
                    + Long.toHexString(packetID));

        String serverDesc = dataResponse.readUTF();
        String[] descSplit = serverDesc.split(";");
        data.put("edition", descSplit[0]);
        data.put("motd1", descSplit[1]);
        data.put("protocol", descSplit[2]);
        data.put("version", descSplit[3]);
        data.put("players", descSplit[4]);
        data.put("maxPlayers", descSplit[5]);
        data.put("motd2", descSplit[7]);
        data.put("gamemode", descSplit[8]);

        return data;
    }
}
