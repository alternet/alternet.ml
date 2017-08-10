package ml.alternet.parser.tests;

import java.util.ArrayList;
import java.util.List;

import ml.alternet.parser.Handler;
import ml.alternet.parser.EventsHandler;
import ml.alternet.parser.EventsHandler.RuleEnd;
import ml.alternet.parser.EventsHandler.RuleStart;
import ml.alternet.parser.EventsHandler.TokenValue;
import ml.alternet.parser.handlers.HandlerBuffer;

class StringListAccumulator implements EventsHandler {

    List<String> result = new ArrayList<>();

    @Override
    public void receive(TokenValue<?> value) {
        if (value.getRule().isFragment()) {

        }
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

    @Override
    public Handler asHandler() {
        return new HandlerBuffer(this);
    }

}