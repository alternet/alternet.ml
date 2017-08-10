package ml.alternet.parser.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ml.alternet.parser.Grammar;
import ml.alternet.parser.Handler;
import ml.alternet.parser.EventsHandler;
import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.handlers.HandlerAccumulator;
import ml.alternet.parser.handlers.HandlerBuffer;
import ml.alternet.scan.Scanner;
import ml.alternet.scan.StringScanner;

public class GrammarTestBase {

    public HandlerAccumulator parse(String str, Class<? extends Grammar> g, Rule r) throws IOException {
        Scanner scanner = new StringScanner(str);
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

    class StringListAccumulator implements EventsHandler {

        List<String> result = new ArrayList<>();

        @Override
        public void receive(TokenValue<?> value) {
            if (value.getType() == Number.class) {
                Number n = value.getValue();
                result.add("Number:" + n);
            } else {
                result.add("String:" + value);
            }
        }

        @Override
        public void receive(RuleStart ruleStart) {
            System.out.println("START " + ruleStart.getRule());
        }

        @Override
        public void receive(RuleEnd ruleEnd) {
            System.out.println("END " + ruleEnd.getRule() + " " + ruleEnd.matched);
        }

        @Override
        public Handler asHandler() {
            return new HandlerBuffer(this);
        }

    }

}
