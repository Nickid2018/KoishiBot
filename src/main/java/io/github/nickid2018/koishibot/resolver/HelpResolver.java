package io.github.nickid2018.koishibot.resolver;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.core.TempFileSystem;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.SyntaxCollection;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.module.Module;
import io.github.nickid2018.koishibot.module.ModuleManager;
import io.github.nickid2018.koishibot.module.ModuleStatus;
import io.github.nickid2018.koishibot.permission.PermissionLevel;
import io.github.nickid2018.koishibot.util.web.WebPageRenderer;
import io.github.nickid2018.koishibot.util.web.WebUtil;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@ResolverName("help")
@Syntax(syntax = "~help", help = "显示此帮助信息")
@Syntax(syntax = "~help [模块名称]", help = "显示模块下所有命令的帮助信息")
public class HelpResolver extends MessageResolver {

    public static final String DOC_HEAD = """
            <!DOCTYPE html>
            <html>
            <head>
            	<link rel="stylesheet" href="https://github.githubassets.com/assets/light-5178aee0ee76.css" />
                <link rel="stylesheet" href="https://github.githubassets.com/assets/primer-494ab2110a2a.css" />
                <link rel="stylesheet" href="https://github.githubassets.com/assets/global-5a9114f3bf45.css" />
                <link rel="stylesheet" href="https://github.githubassets.com/assets/github-82b524748602.css" />
              	<link rel="stylesheet" href="https://github.githubassets.com/assets/code-3d7b701fc6eb.css" />
            </head>
            <body>
            <div class="markdown-body" style="padding:10px;width:min-content">
            """;
    public static final String DOC_END = """
            </div>
            </body>
            </html>
            """;

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
                StringBuilder builder = new StringBuilder();
                builder.append("Koishi bot模块帮助，下面为已加载的模块:\n");
                ModuleManager.getModules().forEach(module ->
                        builder.append(module.getName()).append(" : ").append(module.getDescription()).append("\n"));
                builder.append("使用~help [模块名]获取具体模块帮助信息");
                environment.getMessageSender().sendMessage(context, environment.newText(builder.toString()));
            } else {
                key = key.toLowerCase(Locale.ROOT);

                Module module = ModuleManager.getModule(key);
                if (module == null) {
                    environment.getMessageSender().sendMessage(context, environment.newText("未知的模块"));
                    return true;
                }
                boolean enabled = context.group() != null && !ModuleManager.isOpened(context.group().getGroupId(), key);
                boolean error = module.getStatus() == ModuleStatus.ERROR;

                File helpImage = TempFileSystem.getTmpFileBuffered(
                        "help", key + (enabled ? "-on" : "-off") + (error ? "-error" : "-normal"));

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

                    JsonObject object = new JsonObject();
                    object.addProperty("text", writer.toString());

                    String finalKey = key;
                    WebPageRenderer.getExecutor().execute(() -> {
                        try {
                            HttpPost post = new HttpPost("https://api.github.com/markdown");
                            post.setHeader("Accept", "application/vnd.github+json");
                            post.setEntity(new StringEntity(object.toString(), StandardCharsets.UTF_8));

                            String convert = WebUtil.fetchDataInText(post);
                            File tmpHTML = TempFileSystem.createTmpFileAndCreate("help", "html");
                            FileWriter fileWriter = new FileWriter(tmpHTML);
                            fileWriter.write(DOC_HEAD);
                            fileWriter.write(convert);
                            fileWriter.write(DOC_END);
                            fileWriter.close();

                            WebPageRenderer.getDriver().manage().window().setSize(new Dimension(10000, 10000));
                            WebPageRenderer.getDriver().get(tmpHTML.toURI().toURL().toString());
                            byte[] imageData = WebPageRenderer.getDriver().findElement(By.className("markdown-body"))
                                    .getScreenshotAs(OutputType.BYTES);
                            TempFileSystem.unlockFileAndDelete(tmpHTML);

                            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
                            File png = TempFileSystem.createTmpFileBuffered("help",
                                    finalKey + (enabled ? "-on" : "-off") + (error ? "-error" : "-normal"),
                                    "help", "png", false);
                            ImageIO.write(image, "png", png);

                            environment.getMessageSender().sendMessage(context, environment.newImage(new FileInputStream(png)));
                        } catch (IOException e) {
                            environment.getMessageSender().onError(e, "help.render", context, false);
                        }
                    });
                } else
                    environment.getMessageSender().sendMessage(context, environment.newImage(new FileInputStream(helpImage)));
            }
        } catch (Exception e) {
            environment.getMessageSender().onError(e, "help", context, false);
        }
        return true;
    }
}
