package io.github.nickid2018.koishibot.resolver;

import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.module.ModuleManager;
import io.github.nickid2018.koishibot.permission.PermissionLevel;

@ResolverName("help")
@Syntax(syntax = "~help", help = "显示此帮助信息")
@Syntax(syntax = "~help [模块名称]", help = "显示模块下所有命令的帮助信息")
public class HelpResolver extends MessageResolver {

    public HelpResolver() {
        super("~help");
    }

    @Override
    public boolean groupTempChat() {
        return true;
    }

    @Override
    public boolean needAt() {
        return true;
    }

    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.UNTRUSTED;
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        key = key.trim();
        try {
            if (key.isEmpty()) {
                StringBuilder builder= new StringBuilder();
                builder.append("Koishi bot模块帮助，下面为已加载的模块:\n");
                ModuleManager.getModules().forEach(module ->
                        builder.append(module.getName()).append(" : ").append(module.getDescription()).append("\n"));
                builder.append("使用~help [模块名]获取具体模块帮助信息");
                environment.getMessageSender().sendMessage(context, environment.newText(builder.toString()));
            } else {
                // TODO
            }
        } catch (Exception e) {
            environment.getMessageSender().onError(e, "help", context, false);
        }
        return true;
    }
}
