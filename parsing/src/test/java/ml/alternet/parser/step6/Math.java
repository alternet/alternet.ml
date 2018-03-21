package ml.alternet.parser.step6;

import static ml.alternet.parser.Grammar.$;
import static ml.alternet.parser.Grammar.is;

import ml.alternet.parser.Grammar.Fragment;
import ml.alternet.parser.Grammar.MainRule;
import ml.alternet.parser.Grammar.Proxy;
import ml.alternet.parser.Grammar.Replace;
import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.Grammar.Token;
import ml.alternet.parser.Grammar.WhitespacePolicy;
import ml.alternet.parser.step6.Operation.Function;
import ml.alternet.parser.step6.Operation.Multiplication;
import ml.alternet.util.EnumUtil;

@WhitespacePolicy
public interface Math extends Calc {

    // MULTIPLICATIVE ::= '×' | '÷'
    enum MathMultiplicative implements Multiplication {
        MULT("×") {
            @Override
            public java.lang.Number apply(java.lang.Number n1, java.lang.Number n2) {
                return n1.doubleValue() * n2.doubleValue();
            }
        }, DIV("÷") {
            @Override
            public java.lang.Number apply(java.lang.Number n1, java.lang.Number n2) {
                return n1.doubleValue() / n2.doubleValue();
            }
        };
        MathMultiplicative(String str) {
            EnumUtil.replace(this, s -> str);
        }
    }
    // same name than in CalcGrammar -> automatic replacement
    Token MULTIPLICATIVE = is(MathMultiplicative.class);

    // FUNCTION ::= 'sin' | 'cos' | 'exp' | 'ln' | 'sqrt' | 'asin' | 'acos'
    enum MathFunction implements Operation.Function {

        asin(n -> java.lang.Math.asin(n.doubleValue())),
        acos(n -> java.lang.Math.acos(n.doubleValue()));

        static {
            EnumUtil.extend(Calc.Function.class);
        }

        java.util.function.Function<java.lang.Number, java.lang.Number> fun;

        @Override
        public java.lang.Number apply(java.lang.Number value) {
            return fun.apply(value);
        }

        MathFunction(java.util.function.Function<java.lang.Number, java.lang.Number> fun) {
            this.fun = fun;
        }

        MathFunction(Calc.Function fun) {
            this.fun = fun::apply;
        }
    }

    Token FUNCTION = is(MathFunction.class);

    // VARIABLE ::= [A-Z] ([A-Z] | DIGIT | '_')*
    Token VARIABLE = UPPERCASE.seq(
            UPPERCASE.or(DIGIT, UNDERSCORE).zeroOrMore() )
            .asToken();

//    @MainRule @Fragment
//    Rule Expression = is(Sum);

    @Fragment
    Rule Argument = ( FUNCTION.seq(LBRACKET, $self, RBRACKET) ).or( Value ).or( LBRACKET.seq(Expression, RBRACKET) );

    Math $ = $();

}
