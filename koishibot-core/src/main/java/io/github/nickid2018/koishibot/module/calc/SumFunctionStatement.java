package io.github.nickid2018.koishibot.module.calc;

import io.github.nickid2018.smcl.Statement;
import io.github.nickid2018.smcl.VariableValueList;
import io.github.nickid2018.smcl.functions.FunctionStatement;
import io.github.nickid2018.smcl.number.LongConvertable;
import io.github.nickid2018.smcl.number.NumberObject;

public class SumFunctionStatement extends FunctionStatement {

    private final Statement startValue;
    private final Statement overValue;
    private final Statement computeStatement;

    public SumFunctionStatement(Statement startValue, Statement overValue, Statement computeStatement, boolean isNegative) {
        super(startValue.getSMCL(), startValue.getVariables(), isNegative);
        this.startValue = startValue;
        this.overValue = overValue;
        this.computeStatement = computeStatement;
    }

    @Override
    public Statement negate() {
        return new SumFunctionStatement(startValue.deepCopy(), overValue.deepCopy(), computeStatement.deepCopy(), !isNegative);
    }

    @Override
    public Statement deepCopy() {
        return new SumFunctionStatement(startValue.deepCopy(), overValue.deepCopy(), computeStatement.deepCopy(), isNegative);
    }

    @Override
    public String toString() {
        return "sum(" + startValue + ", " + overValue + ", " + computeStatement + ")";
    }

    @Override
    protected NumberObject calculateInternal(VariableValueList list) {
        VariableValueList withIterator = new VariableValueList(getSMCL());
        NumberObject object = startValue.calculate(list);
        if (!(object instanceof LongConvertable))
            throw new ArithmeticException("Cannot calculate sum function with non-integer start value");
        long iterator = ((LongConvertable) object).toLong();
        NumberObject over = overValue.calculate(list);
        if (!(over instanceof LongConvertable))
            throw new ArithmeticException("Cannot calculate sum function with non-integer over value");
        long overValue = ((LongConvertable) over).toLong();
        NumberObject compute = getSMCL().numberProvider.getZero();
        while (iterator <= overValue) {
            withIterator.addVariableValue("k", getSMCL().numberProvider.fromStdNumber(iterator));
            compute = compute.add(computeStatement.calculate(withIterator));
            iterator++;
        }
        return compute;
    }

    @Override
    protected Statement derivativeInternal() {
        throw new ArithmeticException("Cannot calculate derivative of sum function");
    }
}
