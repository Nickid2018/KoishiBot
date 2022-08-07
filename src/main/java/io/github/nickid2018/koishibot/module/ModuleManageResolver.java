package io.github.nickid2018.koishibot.module;

import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.permission.PermissionLevel;
import io.github.nickid2018.koishibot.util.AsyncUtil;

import java.util.Locale;

@ResolverName("module-manage")
@Syntax(syntax = "~module open [模块]", help = "开启模块")
@Syntax(syntax = "~module close [模块]", help = "关闭模块")
@Syntax(syntax = "~module reload [模块]", help = "重新加载模块")
@Syntax(syntax = "~module status [模块]", help = "查询模块的状态")
public class ModuleManageResolver extends MessageResolver {

    public ModuleManageResolver() {
        super("~module");
    }

    @Override
    public boolean friendEnabled() {
        return false;
    }

    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.ADMIN;
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        String[] split = key.split(" ");
        assert context.group() != null;
        if (split.length == 2) {
            AsyncUtil.execute(() -> {
                try {
                    switch (split[0].toLowerCase(Locale.ROOT)) {
                        case "open" -> {
                            if (ModuleManager.open(context.group(), split[1]))
                                environment.getMessageSender().sendMessage(context, environment.newText("已开启模块" + split[1]));
                            else
                                environment.getMessageSender().sendMessage(context, environment.newText("无法开启模块" + split[1]));
                        }
                        case "close" -> {
                            if (ModuleManager.close(context.group(), split[1]))
                                environment.getMessageSender().sendMessage(context, environment.newText("已关闭模块" + split[1]));
                            else
                                environment.getMessageSender().sendMessage(context, environment.newText("无法关闭模块" + split[1]));
                        }
                        case "reload" -> {
                            if (ModuleManager.reload(split[1]))
                                environment.getMessageSender().sendMessage(context, environment.newText("重新载入模块" + split[1] + "完成"));
                            else
                                environment.getMessageSender().sendMessage(context, environment.newText("无法重新载入模块" + split[1]));
                        }
                        case "status" -> {
                            Module module = ModuleManager.getModule(split[1]);
                            if (module == null)
                                environment.getMessageSender().sendMessage(context, environment.newText("不存在模块" + split[1]));
                            else
                                environment.getMessageSender().sendMessage(context, environment.newText(
                                        "模块" + split[1] + "目前状态为" + module.getStatus()));
                        }
                    }
                } catch (Exception e) {
                    environment.getMessageSender().onError(e, "module.manage", context, false);
                }
            });
            return true;
        }
        return false;
    }
}
