package ml.alternet.parser.step6;

import static ml.alternet.parser.Grammar.*;

import ml.alternet.parser.Grammar;

public interface ValueTemplate extends Grammar {

    @Fragment Token LCB = is('{');
    @Fragment Token RCB = is('}');

    Token ESCAPE_LCB = is("{{"); // a double {{ is an escape for {
    Token ESCAPE_RCB = is("}}"); // a double }} is an escape for }

    //   EXPRESSION ::= Calc
    Token EXPRESSION = is(
            Calc.$,
            () -> new ExpressionBuilder());

    //   Text ::= ('{{' | '}}' | !('{' & '}'))*
    Rule Text = ESCAPE_LCB.or(ESCAPE_RCB, isNot(LCB, RCB)).zeroOrMore();

    //   ValueTemplate ::= ( Text EXPRESSION? )*
    @MainRule
    Rule ValueTemplate = Text.seq(LCB.seq(EXPRESSION.optional(), RCB).optional()).zeroOrMore();

    ValueTemplate $ = $();

}
