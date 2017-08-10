package ml.alternet.parser;

import ml.alternet.facet.Rewindable;

/**
 * Events receiver when a grammar is parsing an input
 * with rewind capabilities (mark, consume/cancel).
 *
 * @author Philippe Poulard
 */
public interface Handler extends EventsHandler, Rewindable {

    @Override
    default Handler asHandler() {
        return this;
    };

    /**
     * The null handler does nothing.
     */
    Handler NULL_HANDLER = new Handler() {
        @Override
        public void receive(TokenValue<?> value) { }
        @Override
        public void receive(RuleStart ruleStart) { }
        @Override
        public void receive(RuleEnd ruleEnd) { }
        @Override
        public void mark() { }
        @Override
        public void cancel() throws IllegalStateException { }
        @Override
        public void consume() throws IllegalStateException { }
        @Override
        public Handler asHandler() {
            return this;
        }
    };

}
