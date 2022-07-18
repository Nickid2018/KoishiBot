package io.github.nickid2018.koishibot.util.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;

public class ServerHandler implements Runnable {

    private final SecServer server;
    private final Socket socket;
    private final DataInputStream dataInput;
    private final DataOutputStream dataOutput;

    private final Consumer<byte[]> dataListener;
    private volatile Key aes;

    public ServerHandler(SecServer server, Socket socket, Consumer<byte[]> dataListener) throws Exception {
        this.server = server;
        this.socket = socket;
        dataInput = new DataInputStream(socket.getInputStream());
        dataOutput = new DataOutputStream(socket.getOutputStream());
        this.dataListener = dataListener;
    }

    @Override
    public void run() {
        try {
            KeyPair pair = CryptUtil.generateRSAKey();
            PublicKey minePublic = pair.getPublic();
            PrivateKey minePrivate = pair.getPrivate();

            // Swap Public
            sendInternal(minePublic.getEncoded(), null);
            byte[] otherPublic = readInternal(null);

            EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(otherPublic);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey remotePublic = keyFactory.generatePublic(encodedKeySpec);

            // Verify Client
            byte[] nonce = new byte[4];
            new Random().nextBytes(nonce);
            sendInternal(nonce, remotePublic);
            byte[] reply = readInternal(minePrivate);
            if (!Arrays.equals(nonce, reply))
                throw new IOException("Verify error");

            // Send AES Key
            aes = CryptUtil.generateAESKey();
            sendInternal(aes.getEncoded(), remotePublic);

            // Start Listen
            while (true)
                dataListener.accept(readInternal(aes));

        } catch (Exception e) {
            e.printStackTrace();
        }
        server.serverHandlers.remove(this);
    }

    public void send(byte[] data) throws Exception {
        if (socket.isClosed())
            throw new IOException("Connection broke.");
        if (aes != null)
            sendInternal(data, aes);
    }

    private void sendInternal(byte[] data, Key key) throws Exception {
        byte[] encode = CryptUtil.encrypt(data, key);
        dataOutput.writeInt(encode.length);
        dataOutput.write(encode);
    }

    private byte[] readInternal(Key key) throws Exception {
        int length = dataInput.readInt();
        byte[] dataEncrypted = new byte[length];
        if (length != dataInput.read(dataEncrypted))
            throw new EOFException();
        return CryptUtil.decrypt(dataEncrypted, key);
    }

    public void close() throws IOException {
        socket.close();
    }
}
