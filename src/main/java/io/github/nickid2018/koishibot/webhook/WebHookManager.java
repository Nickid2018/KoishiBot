package io.github.nickid2018.koishibot.webhook;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.nickid2018.koishibot.core.ErrorRecord;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class WebHookManager {

    private static HttpServer httpServer;

    public static void startWebHook() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(14514), 0);
        httpServer.setExecutor(Executors.newCachedThreadPool(
                new BasicThreadFactory.Builder().daemon(true).uncaughtExceptionHandler(
                        (th, t) -> ErrorRecord.enqueueError("concurrent.webhook", t)
                ).build()));
        httpServer.start();
        System.out.println("Webhook Manager started on port 14514");
    }

    public static void addHandle(String root, HttpHandler handler) throws IOException {
        if (httpServer == null)
            startWebHook();
        httpServer.createContext(root, handler);
    }

    public static void stop() {
        if (httpServer != null)
            httpServer.stop(0);
    }
}
