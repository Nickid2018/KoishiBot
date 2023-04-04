package io.github.nickid2018.koishibot.module.calc;

import io.github.nickid2018.smcl.SMCLContext;
import io.github.nickid2018.smcl.Statement;
import io.github.nickid2018.smcl.StatementParseException;
import io.github.nickid2018.smcl.parser.FunctionParser;
import io.github.nickid2018.smcl.util.BinaryFunction;

import java.security.SecureRandom;
import java.util.Random;

public class StandardRandomFunctionParser extends FunctionParser {

    private static final Random random = new SecureRandom();
    private static final BinaryFunction RANDOM_FUNCTION = (a, b) ->
            CalcModule.getContext().numberProvider.fromStdNumber(random.nextDouble(a.toStdNumber(), b.toStdNumber()));
    private static final BinaryFunction RANDOM_INT_FUNCTION = (a, b) ->
            CalcModule.getContext().numberProvider.fromStdNumber(random.nextInt((int) a.toStdNumber(), (int) b.toStdNumber()));

    private final boolean isInt;

    public StandardRandomFunctionParser(boolean isInt) {
        this.isInt = isInt;
    }

    @Override
    public boolean numParamsVaries() {
        return true;
    }

    @Override
    public int getNumParams() {
        return -1;
    }

    @Override
    public Statement parseStatement(SMCLContext smcl, Statement... statements) {
        if (statements.length == 1)
            return new StandardRandomFunctionStatement(null, statements[0],
                    isInt ? RANDOM_INT_FUNCTION : RANDOM_FUNCTION, isInt ? "randint" : "rand", false);
        else if (statements.length == 2)
            return new StandardRandomFunctionStatement(statements[0], statements[1],
                    isInt ? RANDOM_INT_FUNCTION : RANDOM_FUNCTION, isInt ? "randint" : "rand", false);
        else
            throw new ArithmeticException("Invalid number of parameters for " + (isInt ? "randint" : "rand") + " function");
    }
}
