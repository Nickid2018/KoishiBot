package io.github.nickid2018.koishibot.module.mc.trans;

import io.github.nickid2018.koishibot.util.tcp.SecServer;

import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

public class TransMain {

    public static void main(String[] args) throws IOException {
        Properties properties = new Properties();
        properties.load(new FileReader("trans.properties"));

        int port = Integer.parseInt(properties.getProperty("port"));
        String rconAddr = properties.getProperty("rcon-ip");
        int rconPort = Integer.parseInt(properties.getProperty("rcon-port"));
        String rconPassword = properties.getProperty("rcon-password");

        ChatDataResolver resolver = new ChatDataResolver(new InetSocketAddress(rconAddr, rconPort), rconPassword);

        SecServer server = new SecServer(port, resolver);
        resolver.setServer(server);
        System.out.println("Transfer Server Started");

        while (System.in.read() != -1)
            ;

        server.close();
    }
}
