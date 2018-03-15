package ml.alternet.parser.step5;

import static ml.alternet.parser.Grammar.$;
import static ml.alternet.parser.Grammar.is;

import ml.alternet.parser.Grammar.Fragment;
import ml.alternet.parser.Grammar.MainRule;
import ml.alternet.parser.Grammar.Proxy;
import ml.alternet.parser.Grammar.Replace;
import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.Grammar.Token;
import ml.alternet.parser.Grammar.WhitespacePolicy;
import ml.alternet.parser.step4.Calc;
import ml.alternet.parser.step4.EvaluableFunction;
import ml.alternet.util.EnumUtil;

@WhitespacePolicy
public interface Math extends Calc {

    // MULTIPLICATIVE ::= '×' | '÷'
    enum MathMultiplicative {
        MULT("×"), DIV("÷");
        MathMultiplicative(String str) {
            EnumUtil.replace(this, s -> str);
        }
    }
    // same name than in CalcGrammar -> automatic replacement
    Token MULTIPLICATIVE = is(MathMultiplicative.class);

    // FUNCTION ::= 'sin' | 'cos' | 'exp' | 'ln' | 'sqrt' | 'asin' | 'acos'
    enum MathFunction implements EvaluableFunction {
        sin(Calc.Function.sin::eval),
        cos(Calc.Function.cos::eval),
        exp(Calc.Function.exp::eval),
        ln(Calc.Function.ln::eval),
        sqrt(Calc.Function.sqrt::eval),
        asin(n -> java.lang.Math.asin(n.doubleValue())),
        acos(n -> java.lang.Math.acos(n.doubleValue()));

        java.util.function.Function<java.lang.Number, java.lang.Number> fun;

        @Override
        public java.lang.Number eval(java.lang.Number value) {
            return fun.apply(value);
        }

        MathFunction(java.util.function.Function<java.lang.Number, java.lang.Number> fun) {
            this.fun = fun;
        }
    }

    Token FUNCTION = is(MathFunction.class);

    // VARIABLE ::= [A-Z] ([A-Z] | DIGIT | '_')*
    Token VARIABLE = UPPERCASE.seq(
            UPPERCASE.or(DIGIT, UNDERSCORE).zeroOrMore() )
            .asToken();

    @Fragment
    Rule Argument = ( FUNCTION.seq(LBRACKET, $self, RBRACKET) ).or( Value ).or( LBRACKET.seq(Expression, RBRACKET) );

    Math $ = $();

}
