package ml.alternet.parser.step2;

import static ml.alternet.parser.Grammar.*;

import static ml.alternet.util.EnumUtil.replace;

import ml.alternet.parser.Grammar;

public interface Calc extends Grammar {

    // FUNCTION ::= 'sin' | 'cos' | 'exp' | 'ln' | 'sqrt'
    enum Function implements EvaluableFunction {
        sin {
            @Override
            public double eval(double value) {
                return Math.sin(value);
            }
        }, cos {
            @Override
            public double eval(double value) {
                return Math.cos(value);
            }
        }, exp {
            @Override
            public double eval(double value) {
                return Math.exp(value);
            }
        }, ln {
            @Override
            public double eval(double value) {
                return Math.log(value);
            }
        }, sqrt {
            @Override
            public double eval(double value) {
                return Math.sqrt(value);
            }
        };
    }
    Token FUNCTION = is(Function.class);

    @Fragment
    Token LBRACKET = is('(');
    @Fragment
    Token RBRACKET = is(')');
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
    @Fragment
    CharToken DIGIT = range('0', '9');

    // NUMBER ::= DIGIT+
    Token NUMBER = DIGIT.oneOrMore()
            .asNumber();

    // UPPERCASE ::= [A-Z]
    @Fragment
    Token UPPERCASE = range('A', 'Z');
    // LOWERCASE ::= [a-z]
    @Fragment
    Token LOWERCASE = range('a', 'z');

    @Fragment Token UNDERSCORE = is('_');

    // VARIABLE ::= ([a-z] | [A-Z]) ([a-z] | [A-Z] | DIGIT | '_')*
    Token VARIABLE = (LOWERCASE).or(UPPERCASE).seq(
            (LOWERCASE).or(UPPERCASE).or(DIGIT).or(UNDERSCORE).zeroOrMore() )
            .asToken();
    // VARIABLE ::= ([a-z] | [A-Z]) ([a-z] | [A-Z] | DIGIT | '_')*
    Token VARIABLE___ = range('a', 'z').union('A', 'Z').seq(
            range('a', 'z').union('A', 'Z').union(DIGIT).union('_').zeroOrMore() )
            .asToken();

    // Value ::= NUMBER | VARIABLE
    @Fragment
    Rule Value = NUMBER.or(VARIABLE);

    // Expression ::= Sum
    Proxy Sum = $();
    @MainRule
    @Fragment
    Rule Expression = is(Sum);

    // Argument ::= FUNCTION Argument | Value | '(' Expression ')'
    @Fragment
    Rule Argument = ( FUNCTION.seq($self) ).or(Value).or( LBRACKET.seq( Expression, RBRACKET ) );

    @Fragment
    Proxy SignedFactor = $();
    // Factor ::= Argument ('^' SignedFactor)?
    Rule Factor = Argument.seq( RAISED.seq(SignedFactor).optional() );

    // SignedFactor ::= ADDITIVE? Factor
    boolean b1 = SignedFactor.is(
        ADDITIVE.optional().seq(Factor)
    );

    // Product ::= Factor (MULTIPLICATIVE SignedFactor)*
    Rule Product = Factor.seq(MULTIPLICATIVE.seq(SignedFactor).zeroOrMore());

    // SignedTerm ::= ADDITIVE? Product
    @Fragment
    Rule SignedTerm = ADDITIVE.optional().seq(Product);

    // Sum ::= SignedTerm (ADDITIVE Product)*
    boolean b2 = Sum.is(
        SignedTerm.seq(ADDITIVE.seq(Product).zeroOrMore())
    );

    Calc $ = $(Calc.class);

}
