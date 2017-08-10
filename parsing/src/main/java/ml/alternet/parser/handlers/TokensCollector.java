package ml.alternet.parser.handlers;

import java.util.LinkedList;
import java.util.function.Predicate;

import ml.alternet.parser.EventsHandler;

/**
 * A handler that collect tokens.
 *
 * @param <T> The type of the collector.
 *
 * @see #newStringBuilderHandler()
 * @see #newTokenValueHandler()
 *
 * @author Philippe Poulard
 */
public class TokensCollector<T> extends HandlerBuffer {

    T tokens;
    Predicate<T> isEmpty;
    boolean collected = false;

    private TokensCollector(EventsHandler handler, T init, Predicate<T> isEmpty) {
        super(handler);
        this.tokens = init;
        this.isEmpty = isEmpty;
    }

    private interface NoRulesHandler extends EventsHandler {

        @Override
        default void receive(RuleStart ruleStart) { }

        @Override
        default void receive(RuleEnd ruleEnd) { }

    }

    /**
     * Create a handler that collect tokens in a list.
     *
     * @return A collector of tokens.
     */
    public static TokensCollector<LinkedList<TokenValue<?>>> newTokenValueHandler() {
        return new NoRulesHandler() {

            LinkedList<TokenValue<?>> list = new LinkedList<>();

            @Override
            public TokensCollector<LinkedList<TokenValue<?>>> asHandler() {
                return new TokensCollector<>(this, list, l -> l.isEmpty());
            };

            @Override
            public void receive(TokenValue<?> value) {
                list.add(value);
            }

        }.asHandler();
    }

    /**
     * Create a handler that collect tokens in a string builder.
     *
     * @return A collector of tokens.
     */
    public static TokensCollector<StringBuilder> newStringBuilderHandler() {
        return new NoRulesHandler() {

            StringBuilder buf = new StringBuilder();

            @Override
            public TokensCollector<StringBuilder> asHandler() {
                return new TokensCollector<>(this, buf, b -> b.length() == 0);
            };

            @Override
            public void receive(TokenValue<?> value) {
                buf.append(value.toString());
            }

        }.asHandler();
    }

    /**
     * Return the tokens collected.
     *
     * @return The tokens.
     *
     * @param <U> The type of the collector.
     */
    @SuppressWarnings("unchecked")
    public <U extends T> U get() {
        collect();
        return (U) this.tokens;
    }

    /**
     * Indicates after collecting whether the tokens are empty or not.
     *
     * @return <code>true</code> when the collection is empty,
     *      <code>false</code> otherwise.
     */
    public boolean isEmpty() {
        collect();
        return this.isEmpty.test(this.tokens);
    }

    private void collect() {
        if (! this.collected) {
            this.collected = true;
            this.consume();
        }
    }

}
