package ml.alternet.parser.step2;

import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.handlers.TreeHandler;
import ml.alternet.parser.step2.Expression;
import ml.alternet.parser.step2.Calc.Additive;
import ml.alternet.parser.step2.Calc.Multiplicative;
import ml.alternet.parser.step2.Expression.Constant;
import ml.alternet.parser.step2.Expression.Exponent;
import ml.alternet.parser.step2.Expression.Product;
import ml.alternet.parser.step2.Expression.Sum;
import ml.alternet.parser.step2.Expression.Term;
import ml.alternet.parser.step2.Expression.Variable;
import ml.alternet.scan.Scanner;

/**
 * Build evaluable expressions based on the {@link Calc}.
 *
 * @see #build(String)
 *
 * @author Philippe Poulard
 */
public class ExpressionBuilder extends TreeHandler<Expression, Expression> {

    @Override
    public Value<Expression> tokenToValue(TokenValue<?> token, Deque<Value<Expression>> next) {
        String tokenName = token.getRule().getName(); // e.g. "FUNCTION"
        // find it with the same name and ask it to transform the token to an expression
        Expression expr = Tokens.valueOf(tokenName).asExpression(token, next);
        if (expr == null) {
            // no transformations was made
            return new Value<Expression>().setSource(token);
        } else {
            // we have it
            return new Value<Expression>().setTarget(expr);
        }
    }

    @Override
    public Value<Expression> ruleToValue(Rule rule, Deque<Value<Expression>> args) {
        String ruleName = rule.getName(); // e.g. "Product"
        // find it with the same name and ask it to transform the rule to an expression
        Expression expr = Rules.valueOf(ruleName).asExpression(rule, args);
        if (expr == null) {
            return null; // discard
        } else {
            return new Value<Expression>().setTarget(expr);
        }
    }

    /**
     * Entry point for building an expression.
     *
     * @param input An expression that follows the grammar, e.g. "sin(x)*(1+var_12)"
     *
     * @return An evaluable expression.
     *
     * @throws IOException
     *
     * @see Calc
     */
    public static Optional<Expression> build(String input) throws IOException {
        ExpressionBuilder eb = new ExpressionBuilder();
        if (Calc.$.parse(Scanner.of(input), eb, true)) {
            return Optional.of(eb.get());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Rules to Expression mapper.
     *
     * @author Philippe Poulard
     */
    enum Rules {
        Sum {
            @SuppressWarnings("unchecked")
            @Override
            public Expression asExpression(Rule rule, Deque<Value<Expression>> args) {
                // Sum ::= SignedTerm (ADDITIVE Product)*
                if (args.size() == 1) {
                    // a single term is not a sum
                    return args.pollFirst().getTarget();
                } else {
                    Expression signedTerm = args.removeFirst().getTarget();
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
            public Expression asExpression(Rule rule, Deque<Value<Expression>> args) {
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
            public Expression asExpression(Rule rule, Deque<Value<Expression>> args) {
                // Factor ::= Argument ('^' SignedFactor)?
                Expression base = args.pollFirst().getTarget();
                Value<Expression> raised = args.peekFirst();
                if (raised != null && raised.isSource() && raised.getSource().getRule() == Calc.RAISED) {
                    args.pollFirst(); // ^
                    Expression exponent = args.pollFirst().getTarget();
                    return new Exponent(base, exponent);
                } else {
                    // a single term is not a factor
                    return base;
                }
            }
        };

        public abstract Expression asExpression(Rule rule, Deque<Value<Expression>> args);

    }

    /**
     * Tokens to Expression mapper.
     *
     * @author Philippe Poulard
     */
    enum Tokens {
        FUNCTION {
            @Override
            public Expression asExpression(TokenValue<?> token, Deque<Value<Expression>> next) {
                // e.g.   sin  x
                //   function  argument
                Calc.Function function = token.getValue();   // e.g.   CalcGrammar.Function.sin
                Expression argument = next.pollFirst().getTarget(); // e.g.   Expression.Variable("x")
                return new Expression.Function(function, argument);
            }
        },
        RAISED {
            @Override
            public Expression asExpression(TokenValue<?> token, Deque<Value<Expression>> next) {
                // e.g. a ^ b
                return null; // we don't know how to process it here => keep the source value
            }
        },
        ADDITIVE {
            @Override
            public Expression asExpression(TokenValue<?> token, Deque<Value<Expression>> next) {
                // e.g. a + b
                Additive op = token.getValue(); // + | -
                // + is always followed by an argument
                Expression arg = next.pollFirst().getTarget(); // b argument
                Term<Additive> term = new Term<>(op, arg);
                return term;
            }
        },
        MULTIPLICATIVE {
            @Override
            public Expression asExpression(TokenValue<?> token, Deque<Value<Expression>> next) {
                // e.g. a * b
                Multiplicative op = token.getValue(); // * | /
                // * is always followed by an argument
                Expression arg = next.pollFirst().getTarget(); // b argument
                Term<Multiplicative> term = new Term<>(op, arg);
                return term;
            }
        },
        NUMBER {
            @Override
            public Expression asExpression(TokenValue<?> token, Deque<Value<Expression>> next) {
                Number n = token.getValue();
                return new Constant(n);
            }
        },
        VARIABLE {
            @Override
            public Expression asExpression(TokenValue<?> token, Deque<Value<Expression>> next) {
                String name = token.getValue();
                return new Variable(name);
            }
        };

        public abstract Expression asExpression(TokenValue<?> token, Deque<Value<Expression>> next);

    }

}
