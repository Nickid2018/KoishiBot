package io.github.nickid2018.koishibot.module.calc;

import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.smcl.StatementParseException;

@ResolverName("calc")
@Syntax(syntax = "~calc [表达式]", help = "计算表达式的值")
public class CalcResolver extends MessageResolver {

    public CalcResolver() {
        super("~calc");
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        AsyncUtil.execute(() -> {
            try {
                double number = CalcModule.getContext().parse(key).calculate(CalcModule.DEFAULT_VARIABLES).toStdNumber();
                environment.getMessageSender().sendMessage(context, environment.newText(key + " = " + number));
            } catch (StatementParseException e) {
                environment.getMessageSender().sendMessage(context, environment.newText("表达式错误：" + e.getMessage()));
            } catch (ArithmeticException e) {
                environment.getMessageSender().sendMessage(context, environment.newText("计算错误：" + e.getMessage()));
            }
        });
        return true;
    }
}
