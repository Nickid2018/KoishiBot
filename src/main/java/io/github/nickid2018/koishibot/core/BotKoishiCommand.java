package io.github.nickid2018.koishibot.core;

import net.mamoe.mirai.console.command.CompositeCommand;
import net.mamoe.mirai.console.command.ConsoleCommandSender;
import net.mamoe.mirai.console.command.descriptor.CommandArgumentContext;
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class BotKoishiCommand extends CompositeCommand {

    public BotKoishiCommand(@NotNull JvmPlugin owner) {
        super(owner, "koishi", new String[]{}, "Koishi Bot指令"
                , owner.getParentPermission(), CommandArgumentContext.EMPTY);
    }

    @SubCommand
    public void reload(ConsoleCommandSender sender) {
        try {
            Settings.reload();
        } catch (IOException e) {
            sender.sendMessage("无法重新加载设置");
            e.printStackTrace();
        }
    }
}
