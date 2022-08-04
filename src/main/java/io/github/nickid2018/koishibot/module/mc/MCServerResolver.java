package io.github.nickid2018.koishibot.module.mc;

import com.google.common.net.HostAndPort;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.ImageMessage;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.JsonUtil;
import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ResolverName("mc-server")
@Syntax(syntax = "~server (je) [地址]", help = "查询JE服务器信息")
@Syntax(syntax = "~server be [地址]", help = "查询BE服务器信息")
public class MCServerResolver extends MessageResolver {

    public MCServerResolver() {
        super("~server");
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        AsyncUtil.execute(() -> {
            String[] split = key.split(" ");
            String addr;
            boolean je = true;
            if (split.length == 1)
                addr = split[0];
            else if (split.length == 2) {
                addr = split[1];
                if (split[0].equalsIgnoreCase("be"))
                    je = false;
                else if (!split[0].equalsIgnoreCase("je"))
                    return;
            } else
                return;

            HostAndPort hostAndPort = HostAndPort.fromString(addr).withDefaultPort(25565);
            InetSocketAddress address = new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort());

            if (je)
                try {
                    JsonObject json = new MCJEServerPing(address).fetchData();
                    StringBuilder sb = new StringBuilder();

                    sb.append("服务器: ").append(addr).append("\n");
                    sb.append("延迟: ").append(JsonUtil.getIntOrZero(json, "ping")).append("ms\n");
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
            else
                try {
                    Map<String, String> data = new MCBEServerPing(address).fetchData();
                    StringBuilder sb = new StringBuilder();

                    sb.append("服务器: ").append(addr).append("\n");
                    sb.append("延迟: ").append(data.get("ping")).append("ms\n");
                    sb.append("类型: ").append(data.get("edition")).append("\n");
                    sb.append("版本: ").append(data.get("version")).append("(协议版本 ").append(data.get("protocol")).append(")\n");
                    sb.append("玩家数量: ").append(data.get("players")).append("/").append(data.get("maxPlayers")).append("\n");
                    sb.append("游戏模式: ").append(data.get("gamemode")).append("\n");
                    sb.append(data.get("motd1")).append("\n").append(data.get("motd2"));

                    environment.getMessageSender().sendMessage(context, environment.newText(sb.toString().trim()));
                } catch (Exception e) {
                    environment.getMessageSender().onError(e, "mc.ping", context, false);
                }
        });
        return true;
    }
}
