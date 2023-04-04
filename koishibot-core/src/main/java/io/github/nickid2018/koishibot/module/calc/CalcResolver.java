package io.github.nickid2018.koishibot.module.calc;

import io.github.nickid2018.koishibot.message.DelegateEnvironment;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.smcl.StatementParseException;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ResolverName("calc")
@Syntax(syntax = "~calc [表达式]", help = "计算表达式的值")
public class CalcResolver extends MessageResolver {

    public CalcResolver() {
        super("~calc");
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, DelegateEnvironment environment) {
        AsyncUtil.execute(() -> {
            Future<Double> future = AsyncUtil.submit(() ->
                    CalcModule.getContext().parse(key).calculate(CalcModule.DEFAULT_VARIABLES).toStdNumber());
            try {
                double number = Objects.requireNonNull(future).get(5, TimeUnit.SECONDS);
                environment.getMessageSender().sendMessage(context, environment.newText(key + " = " + number));
            } catch (ExecutionException e) {
                if (e.getCause() instanceof StatementParseException)
                    environment.getMessageSender().sendMessage(context, environment.newText("表达式错误：" + e.getCause().getMessage()));
                else if (e.getCause() instanceof ArithmeticException)
                    environment.getMessageSender().sendMessage(context, environment.newText("计算错误：" + e.getCause().getMessage()));
                else
                    environment.getMessageSender().onError(e.getCause(), "calc", context, false);
            } catch (TimeoutException e) {
                environment.getMessageSender().sendMessage(context, environment.newText("计算的时间超出了限制（5s）。"));
            } catch (Exception e) {
                environment.getMessageSender().onError(e, "calc", context, false);
            }
        });
        return true;
    }
}
