package ml.alternet.parser.ast;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.handlers.ValueMapper.Value;
import ml.alternet.parser.util.ValueStack;

/**
 * Maps a rule or token value to a custom user value.
 *
 * @author Philippe Poulard
 *
 * @param <Match> Rule or Token value that matched.
 * @param <V> The type of the target value.
 */
@FunctionalInterface
public interface Mapper<Match, V> {

    /**
     * Transform a rule to a node in the context
     * of what was previously parsed in the stack,
     * and what follows in the current rule.
     *
     * @param stack The stack contains source tokens
     *      encountered so far but not yet processed.
     *      The stack is a work area that may be useful
     *      for the transformation of the token.
     * @param matcher The actual rule/token value to transform is
     *      supplied by the parser.
     * @param data The elements available in the current
     *      context of this transformation.
     * @return A custom user value, or <code>null</code>
     *      to discard the rule, which means that all
     *      its arguments will be merged with the arguments
     *      of the nested rule.
     */
    V transform(
            ValueStack<Value<V>> stack,
            Match matcher,
            Deque<Value<V>> data);

    /**
     * Convenient method for building a set
     * of mappers.
     *
     * @param <K> The type of the key : Rule, or Token.
     * @param <M> A subclass of Mapper.
     * @param <V> The type of the target value.
     *
     * @return A mapper builder.
     */
    static <K extends Rule, M extends Mapper<?, V>, V> Builder<K, M, V> $() {
        return new Builder<K, M, V>();
    }

    /**
     * A mapper builder.
     *
     * @author Philippe Poulard
     *
     * @param <K> The type of the key : String, Rule, or Token.
     * @param <M> A subclass of Mapper.
     * @param <V> The type of the target value.
     */
    class Builder<K extends Rule, M extends Mapper<?, V>, V> implements Supplier<Map<String, M>> {

        Map<String, M> map = new HashMap<>();

        /**
         * Add a mapping
         *
         * @param ruleOrToken The key is the actual rule or token
         * @param mapper The mapper
         * @return {@code this}, for chaining.
         */
        public Builder<K, M, V> add(K ruleOrToken, M mapper) {
            map.put(ruleOrToken.getName(), mapper);
            return this;
        }

        @Override
        public Map<String, M> get() {
            return map;
        }

    }

}
