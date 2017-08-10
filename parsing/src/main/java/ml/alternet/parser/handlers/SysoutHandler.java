package ml.alternet.parser.handlers;

import ml.alternet.parser.EventsHandler;
import ml.alternet.parser.Handler;

/**
 * Just print the received events.
 *
 * @author Philippe Poulard
 */
public class SysoutHandler implements EventsHandler {

    @Override
    public void receive(TokenValue<?> value) {
        System.out.println(value);
    }

    @Override
    public void receive(RuleStart ruleStart) {
        System.out.println(ruleStart.getRule() + " starting");
    }

    @Override
    public void receive(RuleEnd ruleEnd) {
        System.out.println(ruleEnd.getRule() + " terminated" + (ruleEnd.matched ? "" : " without matching"));
    }

    @Override
    public Handler asHandler() {
        return new HandlerBuffer(this);
    };

}
