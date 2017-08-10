package ml.alternet.parser.step4;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.ast.NodeBuilder;
import ml.alternet.parser.ast.RuleMapper;
import ml.alternet.parser.ast.TokenMapper;
import ml.alternet.parser.step4.Calc.Additive;
import ml.alternet.parser.step4.Calc.Multiplicative;
import ml.alternet.parser.step4.NumericExpression.Constant;
import ml.alternet.parser.step4.NumericExpression.Exponent;
import ml.alternet.parser.step4.NumericExpression.Product;
import ml.alternet.parser.step4.NumericExpression.Sum;
import ml.alternet.parser.step4.NumericExpression.Term;
import ml.alternet.parser.step4.NumericExpression.Variable;
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
        super(Calc.$);
        setTokenMapper(Tokens.class);
        setRuleMapper(Rules.class);
    }

    /**
     * Rules to Expression mapper.
     *
     * @author Philippe Poulard
     */
    enum Rules implements RuleMapper<NumericExpression> {
        Sum {
            @SuppressWarnings("unchecked")
            @Override
            public NumericExpression transform(
                    ValueStack<ml.alternet.parser.handlers.TreeHandler.Value<NumericExpression>> stack,
                    Rule rule,
                    Deque<ml.alternet.parser.handlers.TreeHandler.Value<NumericExpression>> args)
            {
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
            }
        },
        Product {
            @SuppressWarnings("unchecked")
            @Override
            public NumericExpression transform(
                    ValueStack<ml.alternet.parser.handlers.TreeHandler.Value<NumericExpression>> stack,
                    Rule rule,
                    Deque<ml.alternet.parser.handlers.TreeHandler.Value<NumericExpression>> args)
            {
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
            }
        },
        Factor {
            @Override
            public NumericExpression transform(
                    ValueStack<ml.alternet.parser.handlers.TreeHandler.Value<NumericExpression>> stack,
                    Rule rule,
                    Deque<ml.alternet.parser.handlers.TreeHandler.Value<NumericExpression>> args)
            {
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
            }
        };

    }

    /**
     * Tokens to Expression mapper.
     *
     * @author Philippe Poulard
     */
    enum Tokens implements TokenMapper<NumericExpression> {
        FUNCTION {
            @Override
            public NumericExpression transform(
                    ValueStack<ml.alternet.parser.handlers.TreeHandler.Value<NumericExpression>> stack,
                    TokenValue<?> token,
                    Deque<ml.alternet.parser.handlers.TreeHandler.Value<NumericExpression>> next)
            {
                // e.g.   sin  x
                //   function  argument
                Calc.Function function = token.getValue();   // e.g.   CalcGrammar.Function.sin
                NumericExpression argument = next.pollFirst().getTarget(); // e.g.   Expression.Variable("x")
                return new NumericExpression.Function(function, argument);
            }
        },
        RAISED {
            @Override
            public NumericExpression transform(
                    ValueStack<ml.alternet.parser.handlers.TreeHandler.Value<NumericExpression>> stack,
                    TokenValue<?> token,
                    Deque<ml.alternet.parser.handlers.TreeHandler.Value<NumericExpression>> next)
            {
                // e.g. a ^ b
                return null; // we don't know how to process it here => keep the source value
            }
        },
        ADDITIVE {
            @Override
            public NumericExpression transform(
                    ValueStack<ml.alternet.parser.handlers.TreeHandler.Value<NumericExpression>> stack,
                    TokenValue<?> token,
                    Deque<ml.alternet.parser.handlers.TreeHandler.Value<NumericExpression>> next)
            {
                // e.g. a + b
                Additive op = token.getValue(); // + | -
                // + is always followed by an argument
                NumericExpression arg = next.pollFirst().getTarget(); // b argument
                Term<Additive> term = new Term<>(op, arg);
                return term;
            }
        },
        MULTIPLICATIVE {
            @Override
            public NumericExpression transform(
                    ValueStack<ml.alternet.parser.handlers.TreeHandler.Value<NumericExpression>> stack,
                    TokenValue<?> token,
                    Deque<ml.alternet.parser.handlers.TreeHandler.Value<NumericExpression>> next)
            {
                // e.g. a * b
                Multiplicative op = token.getValue(); // * | /
                // * is always followed by an argument
                NumericExpression arg = next.pollFirst().getTarget(); // b argument
                Term<Multiplicative> term = new Term<>(op, arg);
                return term;
            }
        },
        NUMBER {
            @Override
            public NumericExpression transform(
                    ValueStack<ml.alternet.parser.handlers.TreeHandler.Value<NumericExpression>> stack,
                    TokenValue<?> token,
                    Deque<ml.alternet.parser.handlers.TreeHandler.Value<NumericExpression>> next)
            {
                Number n = token.getValue();
                return new Constant(n);
            }
        },
        VARIABLE {
            @Override
            public NumericExpression transform(
                    ValueStack<ml.alternet.parser.handlers.TreeHandler.Value<NumericExpression>> stack,
                    TokenValue<?> token,
                    Deque<ml.alternet.parser.handlers.TreeHandler.Value<NumericExpression>> next)
            {
                String name = token.getValue();
                return new Variable(name);
            }
        };

    }

}
