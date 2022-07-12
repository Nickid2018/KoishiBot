package io.github.nickid2018.koishibot.resolver;

import com.google.common.net.HostAndPort;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.mc.ServerPing;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.ImageMessage;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.JsonUtil;
import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MCServerResolver extends MessageResolver {

    public MCServerResolver() {
        super("~server");
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        AsyncUtil.execute(() -> {
            try {
                HostAndPort hostAndPort = HostAndPort.fromString(key).withDefaultPort(25565);
                InetSocketAddress address = new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort());
                JsonObject json = new ServerPing(address).fetchData();

                StringBuilder sb = new StringBuilder();

                sb.append("服务器: ").append(key).append("\n");
                sb.append("延迟: ").append(JsonUtil.getIntOrZero(json, "ping")).append("\n");
                sb.append("版本: ").append(JsonUtil.getStringInPathOrNull(json, "version.name"));
                sb.append("(协议版本 ").append(JsonUtil.getIntInPathOrElse(json, "version.protocol", -1)).append(")\n");

                int onlinePlayers;
                sb.append("玩家数量: ").append(onlinePlayers = JsonUtil.getIntInPathOrZero(json, "players.online"))
                        .append("/").append(JsonUtil.getIntInPathOrElse(json, "players.max", -1)).append("\n");

                JsonUtil.getDataInPath(json, "players.sample", JsonArray.class).ifPresent(array -> {
                    sb.append("目前玩家: ");
                    if (array.size() < onlinePlayers)
                        sb.append("(仅显示一部分)");
                    sb.append("\n");

                    List<String> players = new ArrayList<>();
                    for (JsonElement element : array)
                        players.add(JsonUtil.getStringOrNull(element.getAsJsonObject(), "name"));
                    sb.append(String.join(", ", players)).append("\n");
                });

                sb.append(JsonUtil.getStringInPathOrElse(
                        json, "description.text", "").replaceAll("§\\w", ""));

                Optional<ImageMessage> favicon = JsonUtil.getString(json, "favicon")
                        .map(s -> s.split(",", 2)[1])
                        .map(Base64::decodeBase64)
                        .map(ByteArrayInputStream::new)
                        .map(byteArrayInputStream -> {
                            try {
                                return environment.newImage(byteArrayInputStream);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });

                if (favicon.isPresent())
                    environment.getMessageSender().sendMessage(context, environment.newChain(
                            environment.newText(sb.toString().trim()),
                            favicon.get()
                    ));
                else
                    environment.getMessageSender().sendMessage(context, environment.newText(sb.toString().trim()));

            } catch (Exception e) {
                environment.getMessageSender().onError(e, "mc.ping", context, false);
            }
        });
        return true;
    }
}
