package ml.alternet.parser.step5;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ml.alternet.parser.Grammar;
import ml.alternet.parser.EventsHandler.TokenValue;
import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.ast.NodeBuilder;
import ml.alternet.parser.ast.RuleMapper;
import ml.alternet.parser.ast.TokenMapper;
import ml.alternet.parser.handlers.ValueMapper.Value;
import ml.alternet.parser.step4.ExpressionBuilder;
import ml.alternet.parser.step4.NumericExpression;
import ml.alternet.parser.util.ValueStack;
import ml.alternet.util.EnumUtil;
import ml.alternet.parser.step4.NumericExpression.Term;
import ml.alternet.parser.step5.Math.MathMultiplicative;

public class MathExpressionBuilder extends NodeBuilder<NumericExpression> {

    public MathExpressionBuilder() {
        this(Math.$);
    }

    public MathExpressionBuilder(Grammar g) {
        super(g);
        setTokenMapper(MathTokens.class);
        setRuleMapper(MathRules.class);
    }

    /**
     * A factor expression is made of multiplicative (×|÷) terms.
     */
    static class MathProduct implements NumericExpression {

        List<Term<MathMultiplicative>> arguments = new ArrayList<>();

        public MathProduct(List<Term<MathMultiplicative>> arguments) {
            this.arguments = arguments;
        }

        @Override
        public Number eval(Map<String, Number> variables) {
            double product = this.arguments.stream()
                .reduce(1d,
                    (val, term) -> term.operation == MathMultiplicative.DIV ?
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

    enum MathRules implements RuleMapper<NumericExpression> {

        Product( (stack, rule, args) -> {
            // Product ::= Factor (MULTIPLICATIVE SignedFactor)*
            if (args.size() == 1) {
                // a single term is not a product
                return args.pollFirst().getTarget();
            } else {
                // assume x to be *x, because the product will start by 1*x
                Term<MathMultiplicative> factor = new Term<>(MathMultiplicative.MULT, args.removeFirst().getTarget());
                List<Term<MathMultiplicative>> arguments = new LinkedList<>();
                arguments.add(factor);
                args.stream()
                    // next arguments are all Term<Multiplicative>
                    .map(v -> (Term<MathMultiplicative>) v.getTarget())
                    .forEachOrdered(arguments::add);
                return new MathProduct(arguments);
            }
        });

        static {
            EnumUtil.extend(ExpressionBuilder.CalcRules.class);
        }

        RuleMapper<NumericExpression> rm;

        MathRules(ExpressionBuilder.CalcRules cr) {
            this.rm = cr::transform;
        }

        MathRules(RuleMapper<NumericExpression> rm) {
            this.rm = rm;
        }

        @Override
        public NumericExpression transform(ValueStack<Value<NumericExpression>> stack, Rule rule, Deque<Value<NumericExpression>> args) {
            return rm.transform(stack, rule, args);
        }

    }

    enum MathTokens implements TokenMapper<NumericExpression> {

        MULTIPLICATIVE( (stack, token, next) -> {
            // e.g. a × b
            MathMultiplicative op = token.getValue(); // × | ÷
            // × is always followed by an argument
            NumericExpression arg = next.pollFirst().getTarget(); // b argument
            Term<MathMultiplicative> term = new Term<>(op, arg);
            return term;
        });

        static {
            EnumUtil.extend(ExpressionBuilder.CalcTokens.class);
        }

        TokenMapper<NumericExpression> tm;

        MathTokens(ExpressionBuilder.CalcTokens ct) {
            this.tm = ct::transform;
        }

        MathTokens(TokenMapper<NumericExpression> tm) {
            this.tm = tm;
        }

        @Override
        public NumericExpression transform(ValueStack<Value<NumericExpression>> stack, TokenValue<?> token, Deque<Value<NumericExpression>> next) {
            return tm.transform(stack, token, next);
        }

    }

}
