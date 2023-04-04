package io.github.nickid2018.koishibot.module.calc;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.nickid2018.koishibot.module.KoishiBotModule;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.smcl.SMCLContext;
import io.github.nickid2018.smcl.SMCLSettings;
import io.github.nickid2018.smcl.VariableValueList;
import io.github.nickid2018.smcl.statements.NumberStatement;

import java.util.List;

public class CalcModule extends KoishiBotModule {

    private static SMCLContext context;
    public static VariableValueList DEFAULT_VARIABLES;

    public CalcModule() {
        super("calc", List.of(
                new CalcResolver()
        ), true);
    }

    @Override
    public void onStartInternal() throws Exception {
        context = new SMCLContext(new SMCLSettings());
        context.init();
        context.globalvars.registerVariable("k");
        context.register.registerConstant("phi",
                new NumberStatement(context, context.numberProvider.fromStdNumber(Math.sqrt(5) / 2 + 0.5)));
        context.register.registerFunction("rand", new StandardRandomFunctionParser(false));
        context.register.registerFunction("randint", new StandardRandomFunctionParser(true));
        context.register.registerFunction("sum", new SumFunctionParser());
        DEFAULT_VARIABLES = new VariableValueList(context);
    }

    @Override
    public void onSettingReloadInternal(JsonObject settingRoot) throws Exception {
        JsonUtil.getData(settingRoot, "math", JsonObject.class).ifPresent(math ->
                context.settings.degreeAngle = JsonUtil.getData(math, "degree", JsonPrimitive.class)
                        .map(JsonPrimitive::getAsBoolean).orElse(false));
    }

    @Override
    public void onTerminateInternal() throws Exception {
    }

    @Override
    public String getDescription() {
        return "数学计算模块";
    }

    @Override
    public String getSummary() {
        return """
                提供有关于数学计算的功能
                """;
    }

    public static SMCLContext getContext() {
        return context;
    }
}
