package ml.alternet.parser.handlers;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import ml.alternet.facet.Rewindable;
import ml.alternet.parser.EventsHandler;
import ml.alternet.parser.Handler;

/**
 * A rewindable accumulator that can be flushed to a target handler.
 *
 * @author Philippe Poulard
 */
public class HandlerBuffer extends HandlerAccumulator implements Handler, Rewindable {

    Deque<Integer> marks = new LinkedList<>();

    EventsHandler handler;

    /**
     * Make a buffer of a handler.
     *
     * @param handler The actual handler.
     */
    public HandlerBuffer(EventsHandler handler) {
        this.handler = handler;
    }

    @Override
    public void receive(TokenValue<?> value) {
        if (this.marks.isEmpty()) {
            handler.receive(value);
        } else {
            super.receive(value);
        }
    }

    @Override
    public void receive(RuleStart ruleStart) {
        if (this.marks.isEmpty()) {
            handler.receive(ruleStart);
        } else {
            super.receive(ruleStart);
        }
    }

    @Override
    public void receive(RuleEnd ruleEnd) {
        if (this.marks.isEmpty()) {
            handler.receive(ruleEnd);
        } else {
            super.receive(ruleEnd);
        }
    }

    @Override
    public void mark() {
        this.marks.push(this.events.size());
    }

    @Override
    public HandlerBuffer asHandler() {
        return this;
    };

    @Override
    public void cancel() throws IllegalStateException {
        if (this.marks.isEmpty()) {
            this.events.clear();
        } else {
            List<RuleEvent<?>> candidates = this.events.subList(this.marks.pop(), this.events.size());
            candidates.clear();
        }
    }

    @Override
    public void consume() throws IllegalStateException {
        if (! this.marks.isEmpty()) {
            this.marks.pop();
        }
        if (this.marks.isEmpty()) {
            super.emitAll(this.handler);
        }
    }

}
