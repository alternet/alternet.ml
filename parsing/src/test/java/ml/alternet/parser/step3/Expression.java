package ml.alternet.parser.step3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ml.alternet.parser.step3.CalcGrammar.Additive;
import ml.alternet.parser.step3.CalcGrammar.Multiplicative;

/**
 * The user data model is a tree of expressions.
 *
 * @author Philippe Poulard
 */
public interface Expression {

    /**
     * An expression is evaluable to a value.
     *
     * @param variables The set of variables available when evaluating the expression.
     *
     * @return The computed value.
     */
    double eval(Map<String, Double> variables);

    /**
     * A function expression embeds an evaluable function.
     *
     * @author Philippe Poulard
     */
    class Function implements Expression {

        Expression argument;
        EvaluableFunction function;

        public Function(EvaluableFunction function, Expression argument) {
            this.function = function;
            this.argument = argument;
        }

        @Override
        public double eval(Map<String, Double> variables) {
            double arg = this.argument.eval(variables);
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
    class Constant implements Expression {

        Number n;

        public Constant(Number n) {
            this.n = n;
        }

        @Override
        public double eval(Map<String, Double> variables) {
            return n.doubleValue();
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
    class Variable implements Expression {

        String name;

        public Variable(String name) {
            this.name = name;
        }

        @Override
        public double eval(Map<String, Double> variables) {
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
    class Exponent implements Expression {

        Expression base;
        Expression exponent;

        public Exponent(Expression base, Expression exponent) {
            this.base = base;
            this.exponent = exponent;
        }

        @Override
        public double eval(Map<String, Double> variables) {
            double base = this.base.eval(variables);
            double exponent = this.exponent.eval(variables);
            return Math.pow(base, exponent);
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
    class Term<T> implements Expression {
        T operation;
        Expression term;

        public Term(T operation, Expression term) {
            this.operation = operation;
            this.term = term;
        }

        @Override
        public double eval(Map<String, Double> variables) {
            return (this.operation == Additive.MINUS) ?
                - term.eval(variables) :
                + term.eval(variables);
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
    class Sum implements Expression {

        List<Term<Additive>> arguments = new ArrayList<>();

        public Sum(List<Term<Additive>> arguments) {
            this.arguments = arguments;
        }

        @Override
        public double eval(Map<String, Double> variables) {
            double sum = this.arguments.stream()
                .mapToDouble(t -> (t.operation == Additive.MINUS) ?
                            - t.term.eval(variables)
                        :   + t.term.eval(variables))
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
    class Product implements Expression {

        List<Term<Multiplicative>> arguments = new ArrayList<>();

        public Product(List<Term<Multiplicative>> arguments) {
            this.arguments = arguments;
        }

        @Override
        public double eval(Map<String, Double> variables) {
            double product = this.arguments.stream()
                .reduce(1d,
                    (val, term) -> term.operation == Multiplicative.DIV ?
                            val / term.term.eval(variables)
                        :   val * term.term.eval(variables),
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
