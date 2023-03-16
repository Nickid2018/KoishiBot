package io.github.nickid2018.koishibot.resolver;

import io.github.nickid2018.koishibot.core.TempFileSystem;
import io.github.nickid2018.koishibot.message.*;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.module.KoishiBotModule;
import io.github.nickid2018.koishibot.module.ModuleManager;
import io.github.nickid2018.koishibot.module.ModuleStatus;
import io.github.nickid2018.koishibot.permission.PermissionLevel;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.web.MarkdownRenderer;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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
    public boolean resolveInternal(String str, MessageContext context, Object resolvedArguments, DelegateEnvironment environment) {
        AsyncUtil.execute(() -> {
            try {
                String key = str.trim();
                if (key.isEmpty()) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("Koishi bot模块帮助，下面为已加载的模块:\n");
                    ModuleManager.getModules().forEach(module ->
                            builder.append(module.getName()).append(" : ").append(module.getDescription()).append("\n"));
                    builder.append("使用~help [模块名]获取具体模块帮助信息");
                    environment.getMessageSender().sendMessage(context, environment.newText(builder.toString()));
                } else {
                    key = key.toLowerCase(Locale.ROOT);

                    KoishiBotModule module = ModuleManager.getModule(key);
                    if (module == null) {
                        environment.getMessageSender().sendMessage(context, environment.newText("未知的模块"));
                        return;
                    }
                    boolean enabled = context.group() != null && !ModuleManager.isOpened(context.group().getGroupId(), key);
                    boolean error = module.getStatus() == ModuleStatus.ERROR;

                    String bufferedName = key + (enabled ? "-on" : "-off") + (error ? "-error" : "-normal");
                    File helpImage = TempFileSystem.getTmpFileBuffered("help", bufferedName);

                    if (helpImage == null) {
                        String name = module.getName();
                        String summary = module.getSummary();
                        List<MessageResolver> resolvers = module.getResolvers();

                        StringWriter writer = new StringWriter();
                        PrintWriter markdown = new PrintWriter(writer);

                        markdown.print("## 模块: " + name);
                        if (error)
                            markdown.print("(因错误停止)");

                        markdown.println();
                        markdown.println();
                        if (enabled)
                            markdown.println("状态: 已禁用");
                        else
                            markdown.println("状态: 已启用");
                        markdown.println();

                        markdown.println(summary.lines().collect(Collectors.joining("\n\n")));
                        markdown.println();

                        markdown.println("### 具体命令语法");
                        markdown.println();
                        markdown.println("所有命令无特殊说明均使用**半角**符号。");
                        markdown.println();
                        markdown.println("以下语法中使用小括号标记代表非必需参数，使用中括号代表必须参数。");
                        markdown.println();

                        markdown.println("|解析器名称|权限|需要at|群聊|私聊|临时会话|陌生人对话|句中解析|命令语法|描述|备注|");
                        markdown.println("|:--|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:--|:--|:--|");
                        resolvers.forEach(resolver -> {
                            String resolverInfo = "|`" + resolver.getClass().getAnnotation(ResolverName.class).value()
                                    + "`|" + resolver.getPermissionLevel() + "|" + (resolver.needAt() ? "是" : "否")
                                    + "|" + (resolver.groupEnabled() ? "是" : "否") + "|" + (resolver.friendEnabled() ? "是" : "否")
                                    + "|" + (resolver.groupTempChat() ? "是" : "否") + "|" + (resolver.strangerChat() ? "是" : "否")
                                    + "|" + (resolver.isInline() ? "是" : "否");
                            List<Syntax> syntax = new ArrayList<>();
                            if (resolver.getClass().isAnnotationPresent(SyntaxCollection.class))
                                syntax.addAll(List.of(resolver.getClass().getAnnotation(SyntaxCollection.class).value()));
                            else
                                syntax.add(resolver.getClass().getAnnotation(Syntax.class));
                            for (Syntax singleSyntax : syntax) {
                                markdown.println(resolverInfo + "|`" + singleSyntax.syntax().replace("|", "\\|")
                                        + "`|" + singleSyntax.help().replace("|", "\\|")
                                        + "|" + singleSyntax.rem().replace("|", "\\|") + "|");
                            }
                        });

                        File png = TempFileSystem.createTmpFileBuffered(
                                "help", bufferedName, "help", "png", false);
                        MarkdownRenderer.render(writer.toString(), png, () -> {
                            environment.getMessageSender().sendMessage(context, environment.newImage(png.toURI().toURL()));
                            return null;
                        }, e -> environment.getMessageSender().onError(e, "help.render", context, false));
                    } else
                        environment.getMessageSender().sendMessage(context, environment.newImage(helpImage.toURI().toURL()));
                }
            } catch (Exception e) {
                environment.getMessageSender().onError(e, "help", context, false);
            }
        });
        return true;
    }
}
