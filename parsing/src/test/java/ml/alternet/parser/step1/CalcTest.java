package ml.alternet.parser.step1;

import static ml.alternet.parser.tests.Calculator.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import ml.alternet.parser.Grammar;
import ml.alternet.parser.Grammar.Choice;
import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.Grammar.ZeroOrMore;
import ml.alternet.parser.handlers.HandlerAccumulator;
import ml.alternet.parser.EventsHandler;
import ml.alternet.scan.Scanner;

public class CalcTest {

    @Test
    public void calcGrammarSingleton_ShouldBe_generated() {
        Calc g = Calc.$;
        assertThat(g).isInstanceOf(Calc.class);
        assertThat(g).isInstanceOf(Grammar.class);
        Calc g2 = Grammar.$(Calc.class);
        assertThat(g).isSameAs(g2);
    }

    public HandlerAccumulator parse(String str, Class<? extends Grammar> g, Rule r) throws IOException {
        Scanner scanner = Scanner.of(str);
        HandlerAccumulator acc = new HandlerAccumulator();
        Grammar sg = Grammar.$(g);
        sg.parse(scanner, acc, r, true);
        return acc;
    }

    public List<String> parseToAcc(String str, Class<? extends Grammar> g, Rule r) throws IOException {
        HandlerAccumulator acc = parse(str, g, r);
        StringListAccumulator sla = new StringListAccumulator();
        acc.emitAll(sla);
        return sla.result;
    }

    static class StringListAccumulator implements EventsHandler {

        List<String> result = new ArrayList<>();

        @Override
        public void receive(TokenValue<?> value) {
            result.add(value.getType().getSimpleName() + ":" + value.getValue());
        }

        @Override
        public void receive(RuleStart ruleStart) {
            System.out.println("START " + ruleStart.getRule());
        }

        @Override
        public void receive(RuleEnd ruleEnd) {
            System.out.println("END " + ruleEnd.getRule() + " " + ruleEnd.matched);
        }

    }

    @Test
    public void calcGrammarTokenizer_Should_containAllTokensExceptFragments() throws IOException {
        Rule rule = C.tokenizer();
        Choice choice = (Choice) ((ZeroOrMore) rule).getComponent();
        List<Rule> tokens = choice.getComponent();
        assertThat(tokens).containsExactly(FUNCTION, LBRACKET, RBRACKET, RAISED,
                ADDITIVE, MULTIPLICATIVE, NUMBER, VARIABLE);
    }

    @Test
    public void calcGrammar_Should_consumeTokens() throws IOException {
        List<String> res = parseToAcc("sin(x)*(1+var_12)", Calc.class, Calc.$.tokenizer());
        assertThat(res).containsExactly("Function:sin", "String:(", "String:x", "String:)", "Multiplicative:*",
                                "String:(", "Number:1", "Additive:+", "String:var_12", "String:)");
    }

    @Test
    public void calcGrammar_Should_consumeFunctionCall() throws IOException {
        List<String> res = parseToAcc("sin(x)", Calc.class, Calc.$.tokenizer());
        assertThat(res).containsExactly("Function:sin", "String:(", "String:x", "String:)");
    }

    @Test
    public void calcGrammar_Should_consumeMult() throws IOException {
        List<String> res = parseToAcc("*", Calc.class, Calc.$.tokenizer());
        assertThat(res).containsExactly("Multiplicative:*");
    }

}
