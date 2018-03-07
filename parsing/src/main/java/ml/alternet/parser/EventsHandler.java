package ml.alternet.parser;

import java.util.Optional;

import ml.alternet.facet.Rewindable;
import ml.alternet.facet.Trackable;
import ml.alternet.misc.Position;
import ml.alternet.parser.Grammar.Fragment;
import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.Grammar.Token;
import ml.alternet.parser.handlers.HandlerBuffer;
import ml.alternet.scan.TrackableScanner;

/**
 * Events receiver when a grammar is parsing an input.
 *
 * @author Philippe Poulard
 */
public interface EventsHandler {

    /**
     * Receive a token value, even for a token that was
     * annotated with {@link Fragment} : the handler has
     * to check the state of the token value to process
     * it properly.
     *
     * @param value The token value
     */
    void receive(TokenValue<?> value);

    /**
     * Receive a rule start event, even for a rule that was
     * annotated with {@link Fragment} : the handler has
     * to check the state of the token value to process
     * it properly.
     *
     * @param ruleStart The rule start event
     */
    void receive(RuleStart ruleStart);

    /**
     * Receive a rule end event, even for a rule that was
     * annotated with {@link Fragment} : the handler has
     * to check the state of the token value to process
     * it properly.
     *
     * @param ruleEnd The rule end event
     */
    void receive(RuleEnd ruleEnd);

    /**
     * Return this event receiver as a rewindable handler.
     *
     * @return A handler with rewindable capabilities.
     *
     * @see Rewindable
     */
    default Handler asHandler() {
        return new HandlerBuffer(this);
    }

    /**
     * Event fired when a rule starts.
     *
     * @author Philippe Poulard
     */
    class RuleStart extends RuleEvent<Rule> {

        /**
         * Create a rule start event.
         *
         * @param rule The rule that is starting.
         * @param trackable The scanner from which the value was read.
         *      If the scanner is a trackable scanner, informations
         *      about the position of the value will be available.
         *
         * @see #getPosition()
         */
        public RuleStart(Rule rule, Trackable trackable) {
            super(rule, trackable);
        }

        @Override
        public void emit(EventsHandler handler) {
            handler.receive(this);
        }

    }

    /**
     * Event fired when a rule ends ; indicates whether
     * the rule was fulfilled or not.
     *
     * @author Philippe Poulard
     */
    class RuleEnd extends RuleEvent<Rule> {

        public boolean matched;

        /**
         * Create a rule end event.
         *
         * @param rule The rule that is ending.
         * @param trackable The scanner from which the value was read.
         *      If the scanner is a trackable scanner, informations
         *      about the position of the value will be available.
         * @param matched <code>true</code> for a rule
         *      that matched the input, <code>false</code>
         *      otherwise.
         *
         * @see #getPosition()
         */
        public RuleEnd(Rule rule, Trackable trackable, boolean matched) {
            super(rule, trackable);
            this.matched = matched;
        }

        @Override
        public void emit(EventsHandler handler) {
            handler.receive(this);
        }
    }

    /**
     * When a rule or token is matched while parsing.
     *
     * A token is somewhat atomic, but a rule is matched
     * in two phases : when entering the rule and when
     * exiting the rule ; on exit, a boolean indicates
     * whether the rule was fulfilled or not.
     *
     * @author Philippe Poulard
     *
     * @param <R> <code>Rule</code> or <code>Token</code>
     */
    abstract class RuleEvent<R extends Rule> implements Trackable {

        Optional<Position> pos;
        R rule;

        /**
         * Create a rule/token event.
         *
         * @param rule The actual rule or token.
         * @param trackable The scanner from which the value was read.
         *      If the scanner is a trackable scanner, informations
         *      about the position of the value will be available.
         *
         * @see #getPosition()
         */
        public RuleEvent(R rule, Trackable trackable) {
            this.rule = rule;
            this.pos = trackable.getPosition();
        }

        /**
         * Return the token/rule that built this value.
         *
         * @return The token/rule.
         */
        public R getRule() {
            return this.rule;
        }

        /**
         * A position is available if the scanner tracks it.
         *
         * @return The position, when available.
         *
         * @see TrackableScanner
         */
        @Override
        public Optional<Position> getPosition() {
            return this.pos;
        }

        /**
         * Emit this event to the target handler.
         *
         * @param handler The target handler.
         */
        public abstract void emit(EventsHandler handler);

        @Override
        public String toString() {
            return this.rule.getName();
        }

    }

    /**
     * A wrapper of a parsed value, the token that creates it,
     * and the position where the value was found when
     * available.
     *
     * @author Philippe Poulard
     *
     * @param <T> The type of the value.
     */
    class TokenValue<T> extends RuleEvent<Token> {

        T value;

        /**
         * Wrap a value that was read.
         *
         * @param token The token that create this value.
         * @param value The actual value.
         * @param trackable The scanner from which the value was read.
         *      If the scanner is a trackable scanner, informations
         *      about the position of the value will be available.
         *
         * @see #getPosition()
         */
        public TokenValue(Token token, T value, Trackable trackable) {
            super(token, trackable);
            this.value = value;
        }

        /**
         * Return the value that was read.
         *
         * @param <V> The type of the value.
         *
         * @return The value.
         */
        @SuppressWarnings("unchecked")
        public <V> V getValue() {
            return (V) this.value;
        }

        @SuppressWarnings("unchecked")
        public void setValue(Object value) {
            this.value = (T) value;
        }

        /**
         * Return the type of the value.
         *
         * @return The type.
         */
        public Class<?> getType() {
            return this.value.getClass();
        }

        @Override
        public String toString() {
            return this.value.toString();
        }

        @Override
        public void emit(EventsHandler handler) {
            handler.receive(this);
        }
    }

    /**
     * A string as the token value.
     *
     * @author Philippe Poulard
     */
    class StringValue extends TokenValue<String> {

        /**
         * Create a string token value.
         *
         * @param token The token in the grammar that produced that value.
         * @param value The actual value.
         * @param trackable The scanner from which the value was read.
         *      If the scanner is a trackable scanner, informations
         *      about the position of the value will be available.
         */
        public StringValue(Token token, String value, Trackable trackable) {
            super(token, value, trackable);
        }

        /**
         * Create a string token value.
         *
         * @param token The token in the grammar that produced that value.
         * @param value The actual value as a Unicode codepoint.
         * @param trackable The scanner from which the value was read.
         *      If the scanner is a trackable scanner, informations
         *      about the position of the value will be available.
         */
        public StringValue(Token token, int value, Trackable trackable) {
            super(token, new String(Character.toChars(value)), trackable);
        }

        /**
         * Create a string token value.
         *
         * @param value The actual value.
         * @param otherValue Get the token and the trackable from the
         *      other value to set in this string value.
         */
        public StringValue(String value, TokenValue<?> otherValue) {
            super(otherValue.getRule(), value, otherValue);
        }

        /**
         * Create a string token value.
         *
         * @param value The actual value as a Unicode codepoint.
         * @param otherValue Get the token and the trackable from the
         *      other value to set in this string value.
         */
        public StringValue(int value, TokenValue<?> otherValue) {
            super(otherValue.getRule(), new String(Character.toChars(value)), otherValue);
        }

    }

    /**
     * A number as the token value.
     *
     * @author Philippe Poulard
     */
    class NumberValue extends TokenValue<Number> {

        /**
         * Create a number token value.
         *
         * @param token The token in the grammar that produced that value.
         * @param value The actual value.
         * @param trackable The scanner from which the value was read.
         *      If the scanner is a trackable scanner, informations
         *      about the position of the value will be available.
         */
        public NumberValue(Token token, Number value, Trackable trackable) {
            super(token, value, trackable);
        }

        @Override
        public Class<Number> getType() {
            return Number.class;
        }

    }

}
