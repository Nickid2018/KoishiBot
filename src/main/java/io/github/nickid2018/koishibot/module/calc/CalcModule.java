package io.github.nickid2018.koishibot.module.calc;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.nickid2018.koishibot.module.Module;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.smcl.SMCLContext;
import io.github.nickid2018.smcl.SMCLSettings;
import io.github.nickid2018.smcl.VariableValueList;
import io.github.nickid2018.smcl.functions.BinaryFunctionBuilder;
import io.github.nickid2018.smcl.parser.BinaryFunctionParser;
import io.github.nickid2018.smcl.statements.NumberStatement;

import java.util.List;

public class CalcModule extends Module {

    private static SMCLContext context;
    public static VariableValueList DEFAULT_VARIABLES;

    // Non-standard functions --------------------------------
    public static final BinaryFunctionBuilder STANDARD_RANDOM = BinaryFunctionBuilder.createBuilder("rand").withCalcFunction(
            (a, b) -> context.numberProvider.fromStdNumber(Math.random() * b.subtract(a).toStdNumber() + a.toStdNumber()));
    public static final BinaryFunctionBuilder STANDARD_RANDOM_INT = BinaryFunctionBuilder.createBuilder("randint").withCalcFunction(
            (a, b) -> context.numberProvider.fromStdNumber(Math.floor(Math.random() * b.subtract(a).toStdNumber() + a.toStdNumber())));
    // End ---------------------------------------------------

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
        context.register.registerFunction("rand", new BinaryFunctionParser(STANDARD_RANDOM));
        context.register.registerFunction("randint", new BinaryFunctionParser(STANDARD_RANDOM_INT));
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
        return "提供有关于数学计算的功能";
    }

    public static SMCLContext getContext() {
        return context;
    }
}
