package io.github.nickid2018.koishibot.util.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class SecServer {

    private final ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    final Map<ServerHandler, Object> serverHandlers = new ConcurrentHashMap<>();

    public SecServer(int port, Consumer<byte[]> dataListener) throws IOException {
        serverSocket = new ServerSocket(port);
        Thread serverThread = new Thread(() -> {
            while (true)
                try {
                    ServerHandler handler = new ServerHandler(this, serverSocket.accept(), dataListener);
                    executor.execute(handler);
                    serverHandlers.put(handler, "");
                } catch (IOException e) {
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }, "Server");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    public Set<ServerHandler> getServerHandlers() {
        return serverHandlers.keySet();
    }

    public void close() throws IOException {
        serverSocket.close();
        getServerHandlers().forEach(s -> {
            try {
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        executor.shutdown();
    }
}
