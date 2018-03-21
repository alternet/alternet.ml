package ml.alternet.parser.step6;

import static ml.alternet.parser.Grammar.*;

import ml.alternet.parser.Grammar;

public interface MathValueTemplate extends ValueTemplate {

    //   EXPRESSION ::= Math
    Token EXPRESSION = is(
            Math.$,
            () -> new MathExpressionBuilder());

    MathValueTemplate $ = $();

}
