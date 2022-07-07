package io.github.nickid2018.koishibot.webhook;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.nickid2018.koishibot.core.ErrorRecord;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class WebHookManager {

    public static final Logger WEBHOOK_LOGGER = LoggerFactory.getLogger("Webhook Manager");

    private static HttpServer httpServer;

    public static void startWebHook() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(14514), 0);
        httpServer.setExecutor(Executors.newCachedThreadPool(
                new BasicThreadFactory.Builder().daemon(true).uncaughtExceptionHandler(
                        (th, t) -> ErrorRecord.enqueueError("concurrent.webhook", t)
                ).build()));
        httpServer.start();
        WEBHOOK_LOGGER.info("Webhook Manager started on port 14514.");
    }

    public static void addHandle(String root, HttpHandler handler) throws IOException {
        if (httpServer == null)
            startWebHook();
        httpServer.createContext(root, handler);
        WEBHOOK_LOGGER.info("Webhook Manager added a handle named {}.", root);
    }

    public static void stop() {
        if (httpServer != null)
            httpServer.stop(0);
        WEBHOOK_LOGGER.info("Webhook Manager stopped.");
    }
}
