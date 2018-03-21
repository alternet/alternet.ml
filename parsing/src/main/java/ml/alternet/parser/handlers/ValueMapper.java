package ml.alternet.parser.handlers;

import java.util.Deque;

import ml.alternet.parser.EventsHandler.RuleEnd;
import ml.alternet.parser.EventsHandler.RuleEvent;
import ml.alternet.parser.EventsHandler.RuleStart;
import ml.alternet.parser.EventsHandler.TokenValue;
import ml.alternet.parser.Grammar.Fragment;
import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.ast.Mappers;
import ml.alternet.parser.ast.RuleMapper;
import ml.alternet.parser.ast.TokenMapper;
import ml.alternet.parser.util.Dual;

/**
 * Transform tokens and rules to values.
 *
 * The context of the transformation is narrowed to the current
 * token and its next values, or to the current rule and its
 * arguments.
 *
 * @see RuleMapper
 * @see TokenMapper
 * @see Mappers
 *
 * @author Philippe Poulard
 *
 * @param <T> The type of the values.
 */
public interface ValueMapper<T> {

    /**
     * Transform a token value to a custom value of
     * the target data structure.
     *
     * @param token The actual token.
     * @param next The next values within the boundaries of the current rule.
     *      For example, if your grammar can parse "+ 123" and that "+" is the
     *      token, then "123" is the first item within the next values.
     * @return A value
     */
    Value<T> tokenToValue(TokenValue<?> token, Deque<Value<T>> next);

    /**
     * Transform a rule and its arguments to a a custom value
     * of the target data structure.
     * The boundaries of the rule are defined by the
     * rule start and rule end events, the events within
     * are considered as argument (unless they are marked
     * as fragments).
     *
     * @see RuleStart
     * @see RuleEnd
     * @see Fragment

     * @param rule The actual rule
     * @param args The elements between rule start and rule end
     *
     * @return A value, or <code>null</code> to discard the rule
     *      and merge the args with the args of the nested rule.
     */
    Value<T> ruleToValue(Rule rule, Deque<Value<T>> args);

    /**
     * Holds &lt;V&gt; or {@link TokenValue} or {@link RuleStart}
     *
     * @author Philippe Poulard
     *
     * @param <V> The type of value.
     */
    public static class Value<V> extends Dual<RuleEvent<?>, V> {

        /**
         * Indicates whether this value is the counterpart
         * rule start of the given ruleEnd or not.
         *
         * @param ruleEnd The rule end to test.
         *
         * @return <code>true</code> if this value wraps the
         *      counterpart rule start, <code>false</code> otherwise.
         */
        public boolean matches(RuleEnd ruleEnd) {
            if (isSource()) {
                RuleEvent<?> e = getSource();
                if (e instanceof RuleStart && e.getRule() == ruleEnd.getRule()) {
                    return true;
                }
            }
            return false;
        }
    }

}
