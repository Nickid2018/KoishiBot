package io.github.nickid2018.koishibot.backend;

import io.github.kookybot.JavaBaseClass;
import io.github.kookybot.client.Client;
import io.github.kookybot.contract.Self;
import io.github.nickid2018.koishibot.message.kook.KOOKEnvironment;
import io.github.nickid2018.koishibot.network.Connection;
import io.github.nickid2018.koishibot.util.LazyLoadedValue;
import kotlin.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    public static final Logger LOGGER = LoggerFactory.getLogger("KoishiBot KOOK Backend");

    public static AtomicBoolean stopped = new AtomicBoolean();
    public static KOOKEnvironment environment;

    public static void main(String[] args) {
        try {
            Settings.loadSettings();
        } catch (Exception e) {
            LOGGER.error("Failed to load settings.", e);
            return;
        }

        Client kookClient = new Client(Settings.token, configureScope -> Unit.INSTANCE);
        Self self = JavaBaseClass.utils.connectWebsocket(kookClient);

        int retry = 0;
        while (!stopped.get() && retry < 100) {
            CompletableFuture<Void> disconnected = new CompletableFuture<>();
            CompletableFuture<KOOKEnvironment> env = new CompletableFuture<>();
            LazyLoadedValue<KOOKEnvironment> lazyLoadedValue = new LazyLoadedValue<>(env::join);
            BackendDataListener listener = new BackendDataListener(lazyLoadedValue::get, disconnected);
            try {
                Connection connection = Connection.connectToTcpServer(
                        listener.getRegistry(), listener, InetAddress.getLocalHost(), Settings.delegatePort);
                environment = new KOOKEnvironment(kookClient, self, connection);
                env.complete(environment);
                retry = 0;
                disconnected.get();
            } catch (Exception e) {
                LOGGER.error("Failed to link.", e);
            }
            if (stopped.get())
                System.exit(0);
            LOGGER.info("Disconnected. Waiting 1min to reconnect.");
            retry++;
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) {
            }
        }

        LOGGER.error(retry == 100 ? "Retries > 100, can't link to delegate. Shutting down." : "Bot offline. Shutting down.");
        kookClient.close();
    }
}
