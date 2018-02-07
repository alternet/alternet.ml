package ml.alternet.parser.tests;

import ml.alternet.parser.Grammar;
import ml.alternet.parser.handlers.HandlerAccumulator;

import static ml.alternet.parser.Grammar.*;
import static ml.alternet.parser.tests.GrammarTest.SimpleGrammar.AltExpr;
import static ml.alternet.parser.tests.GrammarTest.SimpleGrammar.Expr;
import static ml.alternet.parser.tests.GrammarTest.SimpleGrammar.ExprList;
import static ml.alternet.parser.tests.GrammarTest.SimpleGrammar.LT;
import static ml.alternet.parser.tests.GrammarTest.SimpleGrammar.LTE;
import static ml.alternet.parser.tests.GrammarTest.SimpleGrammar.LtExpr;
import static ml.alternet.parser.tests.GrammarTest.SimpleGrammar.LteExpr;
import static ml.alternet.parser.tests.GrammarTest.SimpleGrammar.NUMBER;
import static ml.alternet.parser.tests.GrammarTest.SimpleGrammar.NotExpr;
import static ml.alternet.parser.tests.GrammarTest.SimpleGrammar.NumberList;
import static ml.alternet.parser.tests.GrammarTest.SimpleGrammar.Sequence;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.testng.annotations.Test;

public class GrammarTest extends GrammarTestBase {

    public static interface SimpleGrammar extends Grammar {

        Token LPAREN = is('(');
        Token RPAREN = is(')');
        Token LT = is('<');
        Token GT = is('>');
        Token LTE = is("<=");
        Token GTE = is(">=");
        Token NOT = is("not");
        Token NUMBER = number();

        @MainRule
        Proxy Expr = $();
        Rule LtExpr = NUMBER.seq(LT).seq(NUMBER);
        Rule GtExpr = NUMBER.seq(GT).seq(NUMBER);
        Rule GteExpr = NUMBER.seq(GTE).seq(NUMBER);
        Rule LteExpr = NUMBER.seq(LTE).seq(NUMBER);
        Rule AltExpr = LteExpr.or(GteExpr).or(LtExpr).or(GtExpr);

        Rule NotExpr = NOT.seq(LPAREN).seq(Expr.optional()).seq(RPAREN);

        boolean rule1 =
              Expr.is( NUMBER.seq(LTE.or(LT).or(GTE).or(GT)).seq(NUMBER) );

        Token COMMA = is(',');

        Rule NumberList = NUMBER.seq(COMMA.seq(NUMBER).zeroOrMore());
        Rule ExprList = Expr.seq(COMMA.seq(Expr).zeroOrMore());

        Proxy Sequence = $();
        boolean rule2 =
                // recursive rule  Sequence := Expr ',' Sequence
                Sequence.is(Expr.seq(COMMA.seq(Sequence).zeroOrMore()));

        SimpleGrammar $ = $();

    }

    public interface AnotherGrammar extends Grammar {
        AnotherGrammar $ = $();
    }

//    @Test
//    public void grammarSingleton_ShouldBe_generatedFromInterface() {
//        SimpleGrammar g = Grammar.$(GrammarTest.SimpleGrammar.class);
//        assertThat(g).isInstanceOf(SimpleGrammar.class);
//        assertThat(g).isInstanceOf(Grammar.class);
//        SimpleGrammar g2 = Grammar.$(SimpleGrammar.class);
//        assertThat(g).isSameAs(g2);
//    }

//    @Test
//    public void grammarSingleton_ShouldBe_generated() {
//        SimpleGrammar g = SimpleGrammar.$;
//        assertThat(g).isInstanceOf(SimpleGrammar.class);
//        assertThat(g).isInstanceOf(Grammar.class);
//        SimpleGrammar g2 = Grammar.$(SimpleGrammar.class);
//        assertThat(g).isSameAs(g2);
//    }

    @Test
    public void simpleGrammar_Should_consumeChar() throws IOException {
        List<String> res = parseToAcc("<", SimpleGrammar.class, LT);
        assertThat(res).contains("String:<");
    }

    @Test
    public void simpleGrammar_ShouldNot_consumeChar() throws IOException {
        List<String> res = parseToAcc("=", SimpleGrammar.class, LT);
        assertThat(res).isEmpty();
    }

    @Test
    public void simpleGrammar_Should_consumeString() throws IOException {
        List<String> res = parseToAcc("<=", SimpleGrammar.class, LTE);
        assertThat(res).contains("String:<=");
    }

    @Test
    public void simpleGrammar_ShouldNot_consumeString() throws IOException {
        HandlerAccumulator acc = parse(">=", SimpleGrammar.class, LTE);
        assertThat(acc.events).isEmpty();
    }

    @Test
    public void simpleGrammar_Should_consumeNumber() throws IOException {
        List<String> res = parseToAcc("1234", SimpleGrammar.class, NUMBER);
        assertThat(res).contains("Number:1234");
    }

    @Test
    public void simpleGrammar_Should_consumeLtExpression() throws IOException {
        List<String> res = parseToAcc("1234<5678", SimpleGrammar.class, LtExpr);
        assertThat(res).contains("Number:1234", "String:<", "Number:5678");
    }

    @Test
    public void simpleGrammar_Should_consumeLteExpression() throws IOException {
        List<String> res = parseToAcc("1234<=5678", SimpleGrammar.class, LteExpr);
        assertThat(res).contains("Number:1234", "String:<=", "Number:5678");
    }

    @Test
    public void simpleGrammar_Should_consumeExpression() throws IOException {
        List<String> res = parseToAcc("1234>5678", SimpleGrammar.class, Expr);
        assertThat(res).contains("Number:1234", "String:>", "Number:5678");
    }

    @Test
    public void simpleGrammar_Should_consumeAltExpression() throws IOException {
        List<String> res = parseToAcc("1234>5678", SimpleGrammar.class, AltExpr);
        assertThat(res).contains("Number:1234", "String:>", "Number:5678");
    }

    @Test
    public void simpleGrammar_Should_consumeNotOptionalExpression() throws IOException {
        List<String> res = parseToAcc("not()", SimpleGrammar.class, NotExpr);
        assertThat(res).contains("String:not", "String:(", "String:)");
    }

    @Test
    public void simpleGrammar_Should_consumeNotExpression() throws IOException {
        List<String> res = parseToAcc("not(1234>5678)", SimpleGrammar.class, NotExpr);
        assertThat(res).contains("String:not", "String:(", "Number:1234", "String:>", "Number:5678", "String:)");
    }

    @Test
    public void simpleGrammar_Should_consumeSingleNumberList() throws IOException {
        List<String> res = parseToAcc("123", SimpleGrammar.class, NumberList);
        assertThat(res).contains("Number:123");
    }

    @Test
    public void simpleGrammar_Should_consumeSeveralNumberList() throws IOException {
        List<String> res = parseToAcc("12,34,56", SimpleGrammar.class, NumberList);
        assertThat(res).contains("Number:12", "Number:34", "Number:56");
    }

    @Test
    public void simpleGrammar_Should_consumeSingleExprList() throws IOException {
        List<String> res = parseToAcc("12<34", SimpleGrammar.class, ExprList);
        assertThat(res).contains("Number:12", "String:<", "Number:34");
    }

    @Test
    public void simpleGrammar_Should_consumeSeveralExprList() throws IOException {
        List<String> res = parseToAcc("12<34,567>=890,123>47", SimpleGrammar.class, ExprList);
        assertThat(res).contains("Number:12", "String:<", "Number:34", "String:,", "Number:567", "String:>=", "Number:890", "String:,", "Number:123", "String:>", "Number:47");
    }

    @Test
    public void simpleGrammar_Should_consumeSingleExprSequence() throws IOException {
        List<String> res = parseToAcc("12<34", SimpleGrammar.class, Sequence);
        assertThat(res).contains("Number:12", "String:<", "Number:34");
    }

    @Test
    public void simpleGrammar_Should_consumeSeveralExprSequence() throws IOException {
        List<String> res = parseToAcc("12<34,567>=890,123>47", SimpleGrammar.class, Sequence);
        assertThat(res).contains("Number:12", "String:<", "Number:34", "String:,", "Number:567", "String:>=", "Number:890", "String:,", "Number:123", "String:>", "Number:47");
    }

}
