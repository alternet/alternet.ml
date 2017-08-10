package ml.alternet.parser.tests;

import static ml.alternet.parser.Grammar.*;

import ml.alternet.util.EnumUtil;

public interface Math extends CalcGrammar {

    Math $ = $();

    // MULTIPLICATIVE ::= '×' | '÷'
    enum MathMultiplicative {
        MULT("×"), DIV("÷");
        MathMultiplicative(String str) {
            EnumUtil.replace(MathMultiplicative.class, this, s -> str);
        }
    }
    // same name than in CalcGrammar -> automatic replacement
    Token MULTIPLICATIVE = is(MathMultiplicative.class);

    // FUNCTION ::= 'sin' | 'cos' | 'exp' | 'ln' | 'sqrt' | 'asin' | 'acos'
    enum MathFunction {
        sin, cos, exp, ln, sqrt, asin, acos;
    }

    @Replace(field="FUNCTION", grammar=CalcGrammar.class)
    Token ADVANCED_FUNCTION = is(MathFunction.class);

    // VARIABLE ::= [A-Z] ([A-Z] | DIGIT | '_')*
    @Replace(field="VARIABLE")
    Token UPPERCASE_VARIABLE = UPPERCASE.seq(
            UPPERCASE.or(DIGIT, UNDERSCORE).zeroOrMore() )
            .asToken();

//    @MainRule
//    Rule Expression = CalcGrammar.Expression;

    Proxy Argument = $();
    boolean b1 = Argument.is(
            ( FUNCTION.seq(LBRACKET, Argument, RBRACKET) ).or( Value ).or( LBRACKET.seq(Expression, RBRACKET) )
    );

}
