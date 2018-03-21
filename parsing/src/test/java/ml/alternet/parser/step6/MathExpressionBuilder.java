package ml.alternet.parser.step6;

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
import ml.alternet.parser.step6.ExpressionBuilder;
import ml.alternet.parser.step6.NumericExpression;
import ml.alternet.parser.step6.Math.MathMultiplicative;
import ml.alternet.parser.step6.NumericExpression.Product;
import ml.alternet.parser.step6.NumericExpression.Term;
import ml.alternet.parser.step6.Operation.Multiplication;
import ml.alternet.parser.util.ValueStack;
import ml.alternet.util.EnumUtil;

public class MathExpressionBuilder extends NodeBuilder<NumericExpression> {

    public MathExpressionBuilder() {
        this(Math.$);
    }

    public MathExpressionBuilder(Grammar g) {
        super(g);
        setTokenMapper(MathTokens.class);
        setRuleMapper(MathRules.class);
    }

    enum MathRules implements RuleMapper<NumericExpression> {

        Product( (stack, rule, args) -> {
            // Product ::= Factor (MULTIPLICATIVE SignedFactor)*
            if (args.size() == 1) {
                // a single term is not a product
                return args.pollFirst().getTarget();
            } else {
                // assume x to be ×x, because the product will start by 1×x
                Term<Multiplication> factor = new Term<>(MathMultiplicative.MULT, args.removeFirst().getTarget());
                List<Term<Multiplication >> arguments = new LinkedList<>();
                arguments.add(factor);
                args.stream()
                    // next arguments are all Term<Multiplicative>
                    .map(v -> (Term<Multiplication>) v.getTarget())
                    .forEachOrdered(arguments::add);
                return new Product(arguments);
            }
        });

        static {
            EnumUtil.extend(ExpressionBuilder.CalcRules.class);
        }

        RuleMapper<NumericExpression> rm;

        MathRules(ExpressionBuilder.CalcRules cr) {
            this.rm = cr;
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
            this.tm = ct;
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
