package ml.alternet.parser.step4;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ml.alternet.parser.ast.Expression;
import ml.alternet.parser.step4.Calc.Additive;
import ml.alternet.parser.step4.Calc.Multiplicative;

/**
 * The user data model is a tree of expressions.
 *
 * @author Philippe Poulard
 */
public interface NumericExpression extends Expression<Number, Map<String,Number>> {

    /**
     * A function expression embeds an evaluable function.
     *
     * @author Philippe Poulard
     */
    class Function implements NumericExpression {

        NumericExpression argument;
        EvaluableFunction function;

        public Function(EvaluableFunction function, NumericExpression argument) {
            this.function = function;
            this.argument = argument;
        }

        @Override
        public Number eval(Map<String, Number> variables) {
            Number arg = this.argument.eval(variables);
            return function.eval(arg);
        }

        @Override
        public String toString() {
            return function + "(" + argument + ")";
        }

    }

    /**
     * A constant expression wraps a number value.
     *
     * @author Philippe Poulard
     */
    class Constant implements NumericExpression {

        Number n;

        public Constant(Number n) {
            this.n = n;
        }

        @Override
        public Number eval(Map<String, Number> variables) {
            return n;
        }

        @Override
        public String toString() {
            return ""+ n;
        }

    }

    /**
     * A variable expression wraps a variable name.
     *
     * @author Philippe Poulard
     */
    class Variable implements NumericExpression {

        String name;

        public Variable(String name) {
            this.name = name;
        }

        @Override
        public Number eval(Map<String, Number> variables) {
            return variables.get(this.name);
        }

        @Override
        public String toString() {
            return name;
        }

    }

    /**
     * An exponent expression is made of a base and an exponent.
     *
     * @author Philippe Poulard
     */
    class Exponent implements NumericExpression {

        NumericExpression base;
        NumericExpression exponent;

        public Exponent(NumericExpression base, NumericExpression exponent) {
            this.base = base;
            this.exponent = exponent;
        }

        @Override
        public Number eval(Map<String, Number> variables) {
            Number base = this.base.eval(variables);
            Number exponent = this.exponent.eval(variables);
            return Math.pow(base.doubleValue(), exponent.doubleValue());
        }
    }

    /**
     * A term is an expression bound with an operation
     * that appears in sums or factors expressions.
     *
     * @author Philippe Poulard
     *
     * @param <T>
     */
    class Term<T> implements NumericExpression {

        public T operation;
        public NumericExpression term;

        public Term(T operation, NumericExpression term) {
            this.operation = operation;
            this.term = term;
        }

        @Override
        public Number eval(Map<String, Number> variables) {
            return (this.operation == Additive.MINUS) ?
                - term.eval(variables).doubleValue() :
                + term.eval(variables).doubleValue();
            // can have +a or -a but can't have *a or /a
        }

        @Override
        public String toString() {
            return operation.toString() + this.term.toString();
        }
    }

    /**
     * A sum expression is made of additive(+|-) terms.
     *
     * @author Philippe Poulard
     */
    class Sum implements NumericExpression {

        List<Term<Additive>> arguments = new ArrayList<>();

        public Sum(List<Term<Additive>> arguments) {
            this.arguments = arguments;
        }

        @Override
        public Number eval(Map<String, Number> variables) {
            double sum = this.arguments.stream()
                .mapToDouble(t -> (t.operation == Additive.MINUS) ?
                            - t.term.eval(variables).doubleValue()
                        :   + t.term.eval(variables).doubleValue())
                .sum();
            return sum;
        }

        @Override
        public String toString() {
            return "(" + arguments.stream()
                .map(t -> "" + t.operation + t.term)
                .collect(Collectors.joining())
                +")";
        }

    }

    /**
     * A factor expression is made of multiplicative(*|/) terms.
     *
     * @author Philippe Poulard
     */
    class Product implements NumericExpression {

        List<Term<Multiplicative>> arguments = new ArrayList<>();

        public Product(List<Term<Multiplicative>> arguments) {
            this.arguments = arguments;
        }

        @Override
        public Number eval(Map<String, Number> variables) {
            double product = this.arguments.stream()
                .reduce(1d,
                    (val, term) -> term.operation == Multiplicative.DIV ?
                            val / term.term.eval(variables).doubleValue()
                        :   val * term.term.eval(variables).doubleValue(),
                    (t1, t2) -> t1 * t2);
            return product;
        }

        @Override
        public String toString() {
            return arguments.get(0).term
                + arguments.stream()
                .skip(1)
                .map(t -> "" + t.operation + t.term)
                .collect(Collectors.joining());
        }

    }

}
