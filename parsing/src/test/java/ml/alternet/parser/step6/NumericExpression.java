package ml.alternet.parser.step6;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ml.alternet.parser.ast.Expression;
import ml.alternet.parser.step6.Operation.Addition;
import ml.alternet.parser.step6.Operation.Multiplication;
import ml.alternet.parser.step6.Operation.Operator;

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
        Operation.Function function;

        public Function(Operation.Function function, NumericExpression argument) {
            this.function = function;
            this.argument = argument;
        }

        @Override
        public Number eval(Map<String, Number> variables) {
            Number arg = this.argument.eval(variables);
            return function.apply(arg);
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
            return java.lang.Math.pow(base.doubleValue(), exponent.doubleValue());
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
    class Term<T extends Operator> implements NumericExpression {

        public T operator;
        public NumericExpression term;

        public Term(T operator, NumericExpression term) {
            this.operator = operator;
            this.term = term;
        }

        @Override
        public Number eval(Map<String, Number> variables) {
            return ((Addition) this.operator).apply(term.eval(variables));
            // can have +a or -a but can't have *a or /a
        }

        @Override
        public String toString() {
            return operator.toString() + this.term.toString();
        }
    }

    /**
     * A sum expression is made of additive(+|-) terms.
     *
     * @author Philippe Poulard
     */
    class Sum implements NumericExpression {

        List<Term<Addition>> arguments = new ArrayList<>();

        public Sum(List<Term<Addition>> arguments) {
            this.arguments = arguments;
        }

        @Override
        public Number eval(Map<String, Number> variables) {
            double sum = this.arguments.stream()
                .mapToDouble(t -> t.operator.apply(t.term.eval(variables)).doubleValue())
                .sum();
            return sum;
        }

        @Override
        public String toString() {
            return "(" + arguments.stream()
                .map(t -> "" + t.operator + t.term)
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

        List<Term<Multiplication>> arguments = new ArrayList<>();

        public Product(List<Term<Multiplication>> arguments) {
            this.arguments = arguments;
        }

        @Override
        public Number eval(Map<String, Number> variables) {
            double product = this.arguments.stream()
                .reduce(1d,
                    (val, term) -> term.operator.apply(val, term.term.eval(variables)).doubleValue(),
                    (t1, t2) -> t1 * t2);
            return product;
        }

        @Override
        public String toString() {
            return arguments.get(0).term
                + arguments.stream()
                .skip(1)
                .map(t -> "" + t.operator + t.term)
                .collect(Collectors.joining());
        }

    }

}
