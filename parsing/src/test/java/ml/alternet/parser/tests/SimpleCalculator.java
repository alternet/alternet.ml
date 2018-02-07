package ml.alternet.parser.tests;

import static ml.alternet.parser.Grammar.*;

import ml.alternet.parser.Grammar;

/**
 * <pre>
 * Expression ← Term ((‘+’ / ‘-’) Term)*
 * Term ← Factor ((‘*’ / ‘/’) Factor)*
 * Factor ← Number / ‘(’ Expression ‘)’
 * Number ← [0-9]+
 * </pre>
 *
 * @author Philippe Poulard
 */
public interface SimpleCalculator extends Grammar {

    Token PLUS = is('+');
    Token MINUS = is('-');
    Token MULT = is('*');
    Token DIV = is('/');
    Token LPAREN = is('(');
    Token RPAREN = is(')');
    Proxy Term = $();
    Proxy Factor = $();
    Rule Expression = Term.seq( PLUS.or(MINUS).seq(Term).zeroOrMore() );
    Rule Number = range('0', '9').oneOrMore();
    boolean init = Term.is( Factor.seq(MULT.or(DIV).seq(Factor).zeroOrMore()) )
            ^    Factor.is( Number.or(LPAREN.seq(Expression).seq(RPAREN)) );

    SimpleCalculator $ = $();

}
