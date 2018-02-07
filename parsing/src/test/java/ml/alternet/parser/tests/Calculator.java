package ml.alternet.parser.tests;

import static ml.alternet.parser.Grammar.*;
import static ml.alternet.util.EnumUtil.replace;

import ml.alternet.parser.Grammar;

public interface Calculator extends Grammar {

    // FUNCTION ::= 'sin' | 'cos' | 'exp' | 'ln' | 'sqrt'
    enum Function {
        sin, cos, exp, ln, sqrt;
    }
    Token FUNCTION = is(Function.class);

    Token LBRACKET = is('(');
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
    @Fragment Token DIGIT = range('0', '9').asNumber();

    // NUMBER ::= DIGIT+
    Token NUMBER = DIGIT.oneOrMore()
            .asNumber();

    // UPPERCASE ::= [A-Z]
    @Fragment Token UPPERCASE = range('A', 'Z');
    // LOWERCASE ::= [a-z]
    @Fragment Token LOWERCASE = range('a', 'z');

    @Fragment Token UNDERSCORE = is('_');

    // VARIABLE ::= ([a-z] | [A-Z]) ([a-z] | [A-Z] | DIGIT | '_')*
    Token VARIABLE = ((LOWERCASE).or(UPPERCASE)).seq(
            (LOWERCASE).or(UPPERCASE).or(DIGIT).or(UNDERSCORE).zeroOrMore() )
            .asToken();

    // Value ::= NUMBER | VARIABLE
    @Fragment Rule Value = NUMBER.or(VARIABLE);

    // Argument ::= Value | FUNCTION Argument | '(' Expression ')'
    @MainRule Proxy Expression = $();
//    Proxy Argument = proxy();
//    boolean b1 = Argument.is(
//            Value.or( FUNCTION.seq(Argument) ).or( LBRACKET.seq(Expression, RBRACKET) )
//    );
    // Argument ::= Value | FUNCTION Argument | '(' Expression ')'
    @Fragment Rule Argument = Value.or( FUNCTION.seq($self) ).or( LBRACKET.seq( $("Expression"), RBRACKET ) );

    Proxy SignedFactor = $();
    // Factor ::= Argument ('^' SignedFactor)?
    Rule Factor = Argument.seq( RAISED.seq(SignedFactor).optional() );

    // SignedFactor ::= ADDITIVE? Factor
    boolean b2 = SignedFactor.is(
        ADDITIVE.optional().seq(Factor)
    );

    Proxy TermOp = $();
    // Term ::= Factor TermOp
    Rule Term = Factor.seq(TermOp);
    // TermOp ::= (MULTIPLICATIVE SignedFactor TermOp)?
    boolean b3 = TermOp.is(
        MULTIPLICATIVE.seq(SignedFactor, TermOp).optional()
    );

    // SignedTerm ::= ADDITIVE? Term
    Rule SignedTerm = ADDITIVE.optional().seq(Term);

    Proxy SumOp = $();
    // Expression ::= SignedTerm SumOp
    boolean b4 = Expression.is(
        SignedTerm.seq(SumOp)
    );

    // SumOp ::= (ADDITIVE Term SumOp)?
    boolean b5 = SumOp.is(
        ADDITIVE.seq(Term, SumOp).optional()
    );

    Calculator C = $();

}
