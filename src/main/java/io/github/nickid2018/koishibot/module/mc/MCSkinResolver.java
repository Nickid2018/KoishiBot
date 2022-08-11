package io.github.nickid2018.koishibot.module.mc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.web.WebUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.HttpGet;

import java.nio.charset.StandardCharsets;

@ResolverName("mc-skin")
@Syntax(syntax = "~skin [正版玩家名称]", help = "获取正版玩家皮肤")
public class MCSkinResolver extends MessageResolver {

    public MCSkinResolver() {
        super("~skin");
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        AsyncUtil.execute(() -> {
            JsonObject object;
            try {
                object = WebUtil.fetchDataInJson(new HttpGet(
                        "https://api.mojang.com/users/profiles/minecraft/" + WebUtil.encode(key))).getAsJsonObject();
            } catch (Exception e) {
                environment.getMessageSender().sendMessage(context, environment.newText("不存在此玩家"));
                return;
            }
            try {
                String uuid = JsonUtil.getString(object, "id").orElseThrow();
                String user = JsonUtil.getStringOrNull(object, "name");
                JsonObject source = WebUtil.fetchDataInJson(new HttpGet(
                        "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid)).getAsJsonObject();
                String encodedValue = JsonUtil.getStringInPathOrNull(source, "properties.0.value");
                JsonObject decoded = JsonParser.parseString(new String(
                        Base64.decodeBase64(encodedValue), StandardCharsets.UTF_8)).getAsJsonObject();
                JsonObject skin = JsonUtil.getDataInPath(decoded, "textures.SKIN", JsonObject.class).orElseThrow();
                String textureURL = JsonUtil.getStringOrNull(skin, "url");
                environment.getMessageSender().sendMessage(context, environment.newText(
                        "玩家" + user + "\n皮肤URL: " + textureURL + "\n"
                                + (skin.has("metadata") ? "Alex" : "Steve") + "模型"));
            } catch (Exception e) {
                environment.getMessageSender().onError(e, "mc.skin", context, false);
            }
        });
        return true;
    }
}
