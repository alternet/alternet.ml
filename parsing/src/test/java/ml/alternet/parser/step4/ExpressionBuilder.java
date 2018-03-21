package ml.alternet.parser.step4;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import ml.alternet.parser.Grammar;
import ml.alternet.parser.EventsHandler.TokenValue;
import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.ast.NodeBuilder;
import ml.alternet.parser.ast.RuleMapper;
import ml.alternet.parser.ast.TokenMapper;
import ml.alternet.parser.handlers.ValueMapper.Value;
import ml.alternet.parser.step4.Calc.Additive;
import ml.alternet.parser.step4.Calc.Multiplicative;
import ml.alternet.parser.step4.NumericExpression.Constant;
import ml.alternet.parser.step4.NumericExpression.Exponent;
import ml.alternet.parser.step4.NumericExpression.Product;
import ml.alternet.parser.step4.NumericExpression.Sum;
import ml.alternet.parser.step4.NumericExpression.Term;
import ml.alternet.parser.step4.NumericExpression.Variable;
import ml.alternet.parser.step4.NumericExpression.Function;
import ml.alternet.parser.util.ValueStack;

/**
 * Build evaluable expressions based on the {@link Calc}.
 *
 * @see #build(String)
 *
 * @author Philippe Poulard
 */
public class ExpressionBuilder extends NodeBuilder<NumericExpression> {

    public ExpressionBuilder() {
        this(Calc.$);
    }

    public ExpressionBuilder(Grammar g) {
        super(g);
        setTokenMapper(CalcTokens.class);
        setRuleMapper(CalcRules.class);
    }

    /**
     * Rules to Expression mapper.
     *
     * @author Philippe Poulard
     */
    public enum CalcRules implements RuleMapper<NumericExpression> {
        Sum( (stack, rule, args) -> {
            // Sum ::= SignedTerm (ADDITIVE Product)*
            if (args.size() == 1) {
                // a single term is not a sum
                return args.pollFirst().getTarget();
            } else {
                NumericExpression signedTerm = args.removeFirst().getTarget();
                if (! (signedTerm instanceof Term<?>)
                    || ! (((Term<?>) signedTerm).operation instanceof Additive)) {
                    // force "x" to be "+x"
                    signedTerm = new Term<>(Additive.PLUS, signedTerm);
                }
                List<Term<Additive>> arguments = new LinkedList<>();
                arguments.add((Term<Additive>) signedTerm);
                args.stream()
                    // next arguments are all Term<Additive>
                    .map(v -> (Term<Additive>) v.getTarget())
                    .forEachOrdered(arguments::add);
                return new Sum(arguments);
            }
        }),
        Product( (stack, rule, args) -> {
            // Product ::= Factor (MULTIPLICATIVE SignedFactor)*
            if (args.size() == 1) {
                // a single term is not a product
                return args.pollFirst().getTarget();
            } else {
                // assume x to be *x, because the product will start by 1*x
                Term<Multiplicative> factor = new Term<>(Multiplicative.MULT, args.removeFirst().getTarget());
                List<Term<Multiplicative>> arguments = new LinkedList<>();
                arguments.add(factor);
                args.stream()
                    // next arguments are all Term<Multiplicative>
                    .map(v -> (Term<Multiplicative>) v.getTarget())
                    .forEachOrdered(arguments::add);
                return new Product(arguments);
            }
        }),
        Factor( (stack, rule, args) -> {
            // Factor ::= Argument ('^' SignedFactor)?
            NumericExpression base = args.pollFirst().getTarget();
            Value<NumericExpression> raised = args.peekFirst();
            if (raised != null && raised.isSource() && raised.getSource().getRule() == Calc.RAISED) {
                args.pollFirst(); // ^
                NumericExpression exponent = args.pollFirst().getTarget();
                return new Exponent(base, exponent);
            } else {
                // a single term is not a factor
                return base;
            }
        });

        RuleMapper<NumericExpression> mapper;

        CalcRules(RuleMapper<NumericExpression> mapper) {
            this.mapper = mapper;
        }

        @Override
        public NumericExpression transform(
                ValueStack<Value<NumericExpression>> stack,
                Rule rule,
                Deque<Value<NumericExpression>> args)
        {
            return this.mapper.transform(stack, rule, args);
        }
    }

    /**
     * Tokens to Expression mapper.
     *
     * @author Philippe Poulard
     */
    public enum CalcTokens implements TokenMapper<NumericExpression> {
        FUNCTION( (stack, token, next) -> {
            // e.g.   sin  x
            //   function  argument
            EvaluableFunction function = token.getValue();   // e.g.   Calc.Function.sin
            NumericExpression argument = next.pollFirst().getTarget(); // e.g.   Expression.Variable("x")
            return new Function(function, argument);
        }),
        RAISED( (stack, token, next) ->
            // e.g. a ^ b
            null // we don't know how to process it here => keep the source value
        ),
        ADDITIVE( (stack, token, next) -> {
            // e.g. a + b
            Additive op = token.getValue(); // + | -
            if (next.isEmpty()) { // in SignedTerm and SignedFactor, the ADDITIVE term
                                  // is within an optional term therefore always alone,
                                  // it is the optional term that is followed by a Product term
                                  // or Factor term, not the ADDITIVE term
                                  // When the next terms list is empty, we are in this situation
                // SignedTerm   ::= ADDITIVE? Product
                // SignedFactor ::= ADDITIVE? Factor
                return null; // raw value Additive
            } else {
                // + is always followed by an argument
                // Sum ::= SignedTerm (ADDITIVE Product)*
                NumericExpression arg = next.pollFirst().getTarget(); // b argument
                Term<Additive> term = new Term<>(op, arg);
                return term;
            }
        }),
        MULTIPLICATIVE( (stack, token, next) -> {
            // e.g. a * b
            Multiplicative op = token.getValue(); // * | /
            // * is always followed by an argument
            NumericExpression arg = next.pollFirst().getTarget(); // b argument
            Term<Multiplicative> term = new Term<>(op, arg);
            return term;
        }),
        NUMBER( (stack, token, next) -> {
            Number n = token.getValue();
            return new Constant(n);
        }),
        VARIABLE( (stack, token, next) -> {
            String name = token.getValue();
            return new Variable(name);
        });

        TokenMapper<NumericExpression> mapper;

        CalcTokens(TokenMapper<NumericExpression> mapper) {
            this.mapper = mapper;
        }

        @Override
        public NumericExpression transform(
                ValueStack<Value<NumericExpression>> stack,
                TokenValue<?> token,
                Deque<Value<NumericExpression>> next)
        {
            return this.mapper.transform(stack, token, next);
        }

    }

}
