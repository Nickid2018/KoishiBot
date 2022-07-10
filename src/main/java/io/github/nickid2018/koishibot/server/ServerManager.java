package io.github.nickid2018.koishibot.server;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.nickid2018.koishibot.core.ErrorRecord;
import io.github.nickid2018.koishibot.util.InternalStack;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

@InternalStack
public class ServerManager {

    public static final Logger SERVER_LOGGER = LoggerFactory.getLogger("Server Manager");

    private static HttpServer httpServer;

    public static void startServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(14514), 0);
        httpServer.setExecutor(Executors.newCachedThreadPool(
                new BasicThreadFactory.Builder().daemon(true).uncaughtExceptionHandler(
                        (th, t) -> ErrorRecord.enqueueError("concurrent.webhook", t)
                ).build()));
        httpServer.start();
        SERVER_LOGGER.info("Server Manager started on port 14514.");
    }

    public static void addHandle(String root, HttpHandler handler) throws IOException {
        if (httpServer == null)
            startServer();
        httpServer.createContext(root, handler);
        SERVER_LOGGER.info("Server Manager added a handle named {}.", root);
    }

    public static void stop() {
        if (httpServer != null)
            httpServer.stop(0);
        SERVER_LOGGER.info("Server Manager stopped.");
    }
}
