package ml.alternet.parser.tests;

import static ml.alternet.parser.tests.Calculator.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.testng.annotations.Test;

import ml.alternet.parser.Grammar;
import ml.alternet.parser.Grammar.Choice;
import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.Grammar.ZeroOrMore;
import ml.alternet.parser.handlers.HandlerAccumulator;
import ml.alternet.scan.Scanner;
import ml.alternet.scan.StringScanner;

public class CalculatorTest {

    @Test
    public void calcGrammarSingleton_ShouldBe_generated() {
        System.out.println("calcGrammarSingleton_ShouldBe_generated");
        Calculator g = Calculator.C;
        assertThat(g).isInstanceOf(Calculator.class);
        assertThat(g).isInstanceOf(Grammar.class);
        Calculator g2 = Grammar.$(Calculator.class);
        assertThat(g).isSameAs(g2);
        System.out.println("calcGrammarSingleton_ShouldBe_generated");
    }

    public static HandlerAccumulator parse(String str, Class<? extends Grammar> g, Rule r) throws IOException {
        Scanner scanner = new StringScanner(str);
        HandlerAccumulator acc = new HandlerAccumulator();
        Grammar sg = Grammar.$(g);
        sg.parse(scanner, acc, r, true);
        return acc;
    }

    public static List<String> parseToAcc(String str, Class<? extends Grammar> g, Rule r) throws IOException {
        HandlerAccumulator acc = parse(str, g, r);
        StringListAccumulator sla = new StringListAccumulator();
        acc.emitAll(sla);
        return sla.result;
    }

    @Test
    public void calcGrammarTokenizer_Should_containAllTokensExceptFragments() throws IOException {
        System.out.println("calcGrammarTokenizer_Should_containAllTokensExceptFragments");
        Rule rule = C.tokenizer();
        Choice choice = (Choice) ((ZeroOrMore) rule).getComponent();
        List<Rule> tokens = choice.getComponent();
        assertThat(tokens).containsExactly(FUNCTION, LBRACKET, RBRACKET, RAISED,
                ADDITIVE, MULTIPLICATIVE, NUMBER, VARIABLE);
        System.out.println("calcGrammarTokenizer_Should_containAllTokensExceptFragments");
    }

    @Test
    public void calcGrammar_Should_consumeTokens() throws IOException {
        System.out.println("calcGrammar_Should_consumeTokens");
        List<String> res = parseToAcc("sin(x)*(1+var_12)", Calculator.class, C.tokenizer());
        assertThat(res).containsExactly("Function:sin", "String:(", "String:x", "String:)", "Multiplicative:*",
                                "String:(", "Number:1", "Additive:+", "String:var_12", "String:)");
        System.out.println("calcGrammar_Should_consumeTokens");
    }

    @Test
    public void calcGrammar_Should_consumeFunctionCall() throws IOException {
        System.out.println("calcGrammar_Should_consumeFunctionCall");
        List<String> res = parseToAcc("sin(x)", Calculator.class, C.tokenizer());
        assertThat(res).containsExactly("Function:sin", "String:(", "String:x", "String:)");
        System.out.println("calcGrammar_Should_consumeFunctionCall");
    }

    @Test
    public void calcGrammar_Should_consumeMult() throws IOException {
        System.out.println("calcGrammar_Should_consumeMult");
        List<String> res = parseToAcc("*", Calculator.class, C.tokenizer());
        assertThat(res).containsExactly("Multiplicative:*");
        System.out.println("calcGrammar_Should_consumeMult");
    }

    @Test
    public void mathGrammarSingleton_ShouldBe_generated() {
        System.out.println("mathGrammarSingleton_ShouldBe_generated");
        Math g = Math.M;
        assertThat(g).isInstanceOf(Math.class);
        assertThat(g).isInstanceOf(Grammar.class);
        Math g2 = Grammar.$(Math.class);
        assertThat(g).isSameAs(g2);
        System.out.println("mathGrammarSingleton_ShouldBe_generated");
    }

}
