package ml.alternet.parser.handlers;

import java.util.LinkedList;
import java.util.List;

import ml.alternet.parser.EventsHandler;

/**
 * An accumulator stores all the events it receives.
 *
 * @author Philippe Poulard
 */
public class HandlerAccumulator implements EventsHandler {

    /**
     * The accumulated events.
     */
    public List<RuleEvent<?>> events = new LinkedList<>();

    /**
     * Emit all accumulated events to a target handler
     * and remove them from this accumulator.
     *
     * @param handler The target handler.
     */
    public void emitAll(EventsHandler handler) {
        this.events.forEach(e -> e.emit(handler));
        this.events.clear();
    }

    @Override
    public void receive(TokenValue<?> value) {
        this.events.add(value);
    }

    @Override
    public void receive(RuleStart ruleStart) {
        this.events.add(ruleStart);
    }

    @Override
    public void receive(RuleEnd ruleEnd) {
        this.events.add(ruleEnd);
    }

}