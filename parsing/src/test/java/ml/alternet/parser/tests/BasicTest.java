package ml.alternet.parser.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import ml.alternet.parser.Grammar;
import ml.alternet.parser.Handler;
import ml.alternet.parser.EventsHandler.TokenValue;
import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.ast.NodeBuilder;
import ml.alternet.parser.handlers.DataHandler;
import ml.alternet.parser.handlers.TokensCollector;
import ml.alternet.parser.handlers.TreeHandler;
import ml.alternet.parser.handlers.TreeHandler.Value;

import static ml.alternet.parser.Grammar.*;
import ml.alternet.parser.step4.ValueTemplate;
import ml.alternet.scan.NumberConstraint;
import ml.alternet.scan.Scanner;
import ml.alternet.scan.StringScanner;
import static ml.alternet.parser.tests.BasicTest.SimpleGrammar.*;

public class BasicTest {

    public interface BasicGrammar extends Grammar {

        @Fragment Token L = is('[');
        @Fragment Token R = is(']');

        @Fragment Token OTHER = isNot(L, R);
        Token OTHERS = isNot(L, R).zeroOrMore().asToken();

        Rule Surrounded = L.seq(OTHERS, R).asToken(
            tokens -> tokens.stream()
                .filter(t -> t.getRule() != L && t.getRule() != R)
                .map(t -> t.getValue().toString())
                .findFirst().orElseGet(()->"")
        ).optional();

        Rule Text = OTHERS.seq(Surrounded).zeroOrMore();

        Rule ChoiceWithOptional = L.optional().or(R);

        @Fragment Token BACKSLASH = is('\\');
        @Fragment Token NOT_BACKSLASH = isNot(BACKSLASH);
        Token CHAR = NOT_BACKSLASH.or(BACKSLASH.seq($any)).asToken(
            tokens -> tokens.stream()
                .filter(t -> t.getRule() != BACKSLASH)
                .map(t -> t.getValue())
                .findFirst().get()
        );

        BasicGrammar $ = $();

    }

     public static List<String> parseToList(String str, Grammar g, Rule r) throws IOException {
        StringListAccumulator sla = new StringListAccumulator();
        g.parse(Scanner.of(str), sla, r, true);
        return sla.result;
    }

    @Test
    public void notChar_Should_matchInput() throws IOException {
        List<String> list = parseToList("?", BasicGrammar.$, BasicGrammar.OTHER);
        assertThat(list).containsExactly("String:?");
    }

    @Test
    public void notChar_ShouldNot_matchInput() throws IOException {
        List<String> list = parseToList("[", BasicGrammar.$, BasicGrammar.OTHER);
        assertThat(list).isEmpty();
    }

    @Test
    public void notChar_ShouldNot_matchLastInput() throws IOException {
        List<String> list = parseToList("]", BasicGrammar.$, BasicGrammar.OTHER);
        assertThat(list).isEmpty();
    }

    @Test
    public void notChar_Should_matchInputString() throws IOException {
        List<String> list = parseToList("abcd[efgh]ijkl", BasicGrammar.$, BasicGrammar.Text);
        assertThat(list).containsExactly("String:abcd", "String:efgh", "String:ijkl");
    }

    @Test
    public void othersChar_Should_matchInputString() throws IOException {
        List<String> list = parseToList("abcd", BasicGrammar.$, BasicGrammar.Text);
        assertThat(list).containsExactly("String:abcd");
    }

    @Test
    public void optionalChoice_Should_matchInputString() throws IOException {
        List<String> list = parseToList("[abc", BasicGrammar.$, BasicGrammar.ChoiceWithOptional);
        assertThat(list).containsExactly("String:[");
    }

    @Test
    public void choiceWithOptional_Should_matchInputString() throws IOException {
        List<String> list = parseToList("]abc", BasicGrammar.$, BasicGrammar.ChoiceWithOptional);
        assertThat(list).containsExactly("String:]");
    }

    @Test
    public void choiceWithOptional_ShouldNot_matchInputString() throws IOException {
        List<String> list = parseToList("_abc", BasicGrammar.$, BasicGrammar.ChoiceWithOptional);
        assertThat(list).isEmpty();
    }

    @Test
    public void char_Should_matchInputString() throws IOException {
        List<String> list = parseToList("abc", BasicGrammar.$, BasicGrammar.CHAR);
        assertThat(list).containsExactly("String:a");
    }

    @Test
    public void escapedChar_Should_matchCharWithTreeHandler() throws IOException {
        TreeHandler th = new TreeHandler<String>() {
            @Override
            public TreeHandler.Value<String> tokenToNode(TokenValue<?> token,
                    Deque<TreeHandler.Value<String>> next) {
                String val = token.getType().getSimpleName() + ":" + token.getValue();
                return new Value<String>().setTarget(val);
            }
            @Override
            public TreeHandler.Value<String> ruleToNode(Rule rule,
                    Deque<TreeHandler.Value<String>> args) {
                return null;
            }
        };
        BasicGrammar.$.parse(Scanner.of("\\abc"), th, BasicGrammar.CHAR, true);
        String tokens = (String) th.get();
        assertThat(tokens).isEqualTo("String:a");
    }

    @Test
    public void escapedChar_Should_matchCharWithNodeBuilder() throws IOException {
        NodeBuilder<String> nb = new NodeBuilder<>(BasicGrammar.$);
        BasicGrammar.$.parse(Scanner.of("\\abc"), nb, BasicGrammar.CHAR, true);
        String list = nb.get();
        assertThat(list).isEqualTo("a");
    }

    @Test
    public void escapedChar_Should_matchCharWithTokenValueHandler() throws IOException {
        TokensCollector<LinkedList<TokenValue<?>>> tc = TokensCollector.newTokenValueHandler();
        BasicGrammar.$.parse(Scanner.of("\\abc"), tc, BasicGrammar.CHAR, true);
        LinkedList<TokenValue<?>> tokens = tc.get();
        List<String> list = tokens.stream()
            .map(value -> value.getType().getSimpleName() + ":" + value.getValue())
            .collect(toList());
        assertThat(list).containsExactly("String:a");
    }

    @Test
    public void escapedChar_Should_matchChar() throws IOException {
        List<String> list = parseToList("\\abc", BasicGrammar.$, BasicGrammar.CHAR);
        assertThat(list).containsExactly("String:a");
    }

    @Test
    public void escapedBackslash_Should_matchBackslash() throws IOException {
        List<String> list = parseToList("\\\\abc", BasicGrammar.$, BasicGrammar.CHAR);
        assertThat(list).containsExactly("String:\\");
    }

    @Test(dataProvider="rules")
    public void rules_ShouldBe_prettyPrinted(Rule rule, String ruleText) throws IOException {
        assertThat(rule.toPrettyString().toString()).isEqualTo(ruleText);
    }

    @DataProvider(name="rules")
    public Object[][] get() {
        return new Object[][] {
            {CHAR, "'_'"},
            {STR, "'Text'"},
            {RANGE, "['a'-'m']"},
            {ENUM, "( 'next' | 'val1' | 'val4' | 'other' | 'Value2' | 'value3' | 'previous' )"},
            {STRENUM, "( 'bar' | 'baz' | 'foo' )"},
            {ENUMVAL, "'next'"},
            {BASIC, "#ml.alternet.parser.tests.BasicTest$BasicGrammar"},
            {NUMBER, "#NUMBER"},
            {BYTE, "#java.lang.Byte"},
            {FLOAT, "#java.lang.Float"},
            {ISNOT, "!( '(' | ')' | '_' )"},
            {ZOM, "STRENUM*"},
            {OPTIONAL, "STRENUM?"},
            {OOM, "STRENUM+"},
            {CHOICE, "( CHAR | STR | RANGE )"},
            {SEQ, "( CHAR STR RANGE )"},
            {SimpleGrammar.$.tokenizer(), "( CHAR | UNICODE_CHAR_CODE | UNICODE_CHAR | STR | RANGE | ENUM | STRENUM | ENUMVAL | BASIC | NUMBER | BYTE | FLOAT | ISNOT )*"},

            {BasicGrammar.OTHER, "!( '[' | ']' )"},
            {BasicGrammar.ChoiceWithOptional, "( L? | R )"},
            {BasicGrammar.Text, "( OTHERS Surrounded )*"},
            {((ZeroOrMore) BasicGrammar.Text).getComponent(), "( OTHERS Surrounded )"}
        };
    }

    enum Enum {
        val1, Value2, value3, val4, next, previous, other;
    }

    public interface SimpleGrammar extends Grammar {

        @Fragment Token L = is('(');
        @Fragment Token R = is(')');

        Token CHAR = is('_');
        Token UNICODE_CHAR_CODE = is(0x1F60E);
        Token UNICODE_CHAR = is("😎".codePointAt(0));
        Token STR = is("Text");
        Token RANGE = range('a', 'm');
        Token ENUM = is(Enum.class);
        Token STRENUM = is("foo", "bar", "baz");
        Token ENUMVAL = is(Enum.next);
        Token BASIC = is(BasicGrammar.$, () -> null);
        Token NUMBER = number();
        Token BYTE = number(NumberConstraint.BYTE_CONSTRAINT);
        Token FLOAT = number(NumberConstraint.FLOAT_CONSTRAINT);
        Token ISNOT = isNot(CHAR, L, R);

        Rule ZOM = STRENUM.zeroOrMore();
        Rule OPTIONAL = STRENUM.optional();
        Rule OOM = STRENUM.oneOrMore();

        Rule CHOICE = CHAR.or(STR, RANGE);
        Rule SEQ = CHAR.seq(STR, RANGE);

        SimpleGrammar $ = $();

    }

    @Test(expectedExceptions={ExceptionInInitializerError.class})
    public void badGrammar_Should_FailOnInitialization() {
        java.util.Optional<Rule> main = BadGrammar.$.mainRule(); // throws Error
        assertThat(main).isEmpty();
    }

    @Test
    public void unicodeChar_ShouldBe_parsed() throws IOException {
        String text = "😎";
        int smiley = text.codePointAt(0);
        assertThat(Character.isBmpCodePoint(smiley)).isFalse();

        assertThat(
            SimpleGrammar.$.parse(Scanner.of(text), Handler.NULL_HANDLER, SimpleGrammar.UNICODE_CHAR, true)
        ).isTrue();
        assertThat(
            SimpleGrammar.$.parse(Scanner.of(text), Handler.NULL_HANDLER, SimpleGrammar.UNICODE_CHAR_CODE, true)
        ).isTrue();
    }

}
