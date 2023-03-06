package io.github.nickid2018.koishibot.module.calc;

import io.github.nickid2018.smcl.SMCLContext;
import io.github.nickid2018.smcl.Statement;
import io.github.nickid2018.smcl.parser.FunctionParser;

public class SumFunctionParser extends FunctionParser {
    @Override
    public boolean numParamsVaries() {
        return true;
    }

    @Override
    public int getNumParams() {
        return 3;
    }

    @Override
    public Statement parseStatement(SMCLContext smcl, Statement... statements) {
        return new SumFunctionStatement(statements[0], statements[1], statements[2], false);
    }
}
