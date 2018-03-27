package ml.alternet.parser.step4;

import static ml.alternet.parser.Grammar.*;
import static ml.alternet.util.EnumUtil.replace;

import java.util.function.Supplier;

import ml.alternet.parser.Grammar;
import ml.alternet.parser.Grammar.CharToken;
import ml.alternet.parser.Grammar.Fragment;
import ml.alternet.parser.Grammar.Skip;

@Skip(token="WS")
public interface Calc extends Grammar {

    @Fragment
    CharToken WS = isOneOf(" \t\n\r");

    // FUNCTION ::= 'sin' | 'cos' | 'exp' | 'ln' | 'sqrt'
    enum Function implements EvaluableFunction {
        sin {
            @Override
            public java.lang.Number eval(java.lang.Number value) {
                return Math.sin(value.doubleValue());
            }
        }, cos {
            @Override
            public java.lang.Number  eval(java.lang.Number  value) {
                return Math.cos(value.doubleValue());
            }
        }, exp {
            @Override
            public java.lang.Number  eval(java.lang.Number  value) {
                return Math.exp(value.doubleValue());
            }
        }, ln {
            @Override
            public java.lang.Number  eval(java.lang.Number  value) {
                return Math.log(value.doubleValue());
            }
        }, sqrt {
            @Override
            public java.lang.Number  eval(java.lang.Number  value) {
                return Math.sqrt(value.doubleValue());
            }
        };
    }
    Token FUNCTION = is(Function.class);

    // LBRACKET ::= '('
    @Fragment Token LBRACKET = is('(');
    // RBRACKET ::= ')'
    @Fragment Token RBRACKET = is(')');
    // RAISED ::= '^'
    Token RAISED = is('^');

    // ADDITIVE ::= '+' | '-'
    enum Additive {
        PLUS("+"), MINUS("-");
        Additive(String str) {
            replace(this, s -> str);
        }
    }
    Token ADDITIVE = is(Additive.class);

    // MULTIPLICATIVE ::= '*' | '/'
    enum Multiplicative {
        MULT("*"), DIV("/");
        Multiplicative(String str) {
            replace(this, s -> str);
        }
    }
    Token MULTIPLICATIVE = is(Multiplicative.class);

    // DIGIT ::= [0-9]
    @Skip(token="$empty")
    Token DIGIT = range('0', '9').asNumber();

    // NUMBER ::= DIGIT+
    Token NUMBER = DIGIT.oneOrMore()
            .asNumber();

    // UPPERCASE ::= [A-Z]
    @Skip(token="$empty")
    Token UPPERCASE = range('A', 'Z');
    // LOWERCASE ::= [a-z]
    @Skip(token="$empty")
    Token LOWERCASE = range('a', 'z');

    @Skip(token="$empty")
    Token UNDERSCORE = is('_');

    // VARIABLE ::= ([a-z] | [A-Z]) ([a-z] | [A-Z] | DIGIT | '_')*
    Token VARIABLE = (LOWERCASE).or(UPPERCASE).seq(
            (LOWERCASE).or(UPPERCASE).or(DIGIT).or(UNDERSCORE).zeroOrMore() )
            .asToken();

    // Value ::= NUMBER | VARIABLE
    @Fragment Rule Value = NUMBER.or(VARIABLE);

    // Sum ::= SignedTerm (ADDITIVE Product)*
    Rule Sum = $(()-> Calc.SignedTerm.seq(ADDITIVE.seq(Calc.Product).zeroOrMore()));

    // Expression ::= Sum
    @MainRule @Fragment
    Rule Expression = is(Sum);

    // Argument ::= FUNCTION Argument | Value | '(' Expression ')'
    @Fragment Rule Argument = ( FUNCTION.seq($self) ).or(Value).or( LBRACKET.seq( Expression, RBRACKET ) );

    // SignedFactor ::= ADDITIVE? Factor
    @Fragment Rule SignedFactor = $(() -> ADDITIVE.optional().seq(Calc.Factor));

    // Factor ::= Argument ('^' SignedFactor)?
    Rule Factor = Argument.seq( RAISED.seq(SignedFactor).optional() );

    // Product ::= Factor (MULTIPLICATIVE SignedFactor)*
    Rule Product = Factor.seq(MULTIPLICATIVE.seq(SignedFactor).zeroOrMore());

    // SignedTerm ::= ADDITIVE? Product
    @Fragment Rule SignedTerm = ADDITIVE.optional().seq(Product);

    Calc $ = $();

}
