package io.github.nickid2018.koishibot.module.calc;

import io.github.nickid2018.smcl.Statement;
import io.github.nickid2018.smcl.VariableValueList;
import io.github.nickid2018.smcl.functions.FunctionStatement;
import io.github.nickid2018.smcl.number.NumberObject;
import io.github.nickid2018.smcl.util.BinaryFunction;

public class StandardRandomFunctionStatement extends FunctionStatement {

    private final Statement lower;
    private final Statement higher;
    private final BinaryFunction function;
    private final String name;

    public StandardRandomFunctionStatement(Statement lower, Statement higher, BinaryFunction function, String name, boolean isNegative) {
        super(higher.getSMCL(), higher.getVariables(), isNegative);
        this.lower = lower;
        this.higher = higher;
        this.function = function;
        this.name = name;
    }

    @Override
    public Statement negate() {
        if (lower == null)
            return new StandardRandomFunctionStatement(null, higher.deepCopy(), function, name, !isNegative);
        return new StandardRandomFunctionStatement(lower.deepCopy(), higher.deepCopy(), function, name, !isNegative);
    }

    @Override
    public Statement deepCopy() {
        if (lower == null)
            return new StandardRandomFunctionStatement(null, higher.deepCopy(), function, name, isNegative);
        return new StandardRandomFunctionStatement(lower.deepCopy(), higher.deepCopy(), function, name, isNegative);
    }

    @Override
    public String toString() {
        if (lower == null)
            return name + "(" + higher + ")";
        return name + "(" + lower + ", " + higher + ")";
    }

    @Override
    protected NumberObject calculateInternal(VariableValueList list) {
        NumberObject lowerValue = higher.getSMCL().numberProvider.getZero();
        if (lower != null)
            lowerValue = lower.calculate(list);
        NumberObject higherValue = higher.calculate(list);
        return function.accept(lowerValue, higherValue);
    }

    @Override
    protected Statement derivativeInternal() {
        return null;
    }
}
