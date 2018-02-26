package ml.alternet.parser.ast;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import ml.alternet.parser.handlers.TreeHandler.Value;
import ml.alternet.parser.util.ValueStack;

/**
 * Maps a rule or token value to a custom user node.
 *
 * @author Philippe Poulard
 *
 * @param <M> Rule or Token value.
 * @param <Node> The type of the target node.
 */
public interface Mapper<M, Node> {

    /**
     * Transform a rule to a node in the context
     * of what was previously parsed in the stack,
     * and what follows in the current rule.
     *
     * @param stack The stack contains source tokens
     *      encountered so far but not yet processed.
     *      The stack is a work area that may be useful
     *      for the transformation of the token.
     * @param value The actual rule/token value to transform is
     *      supplied by the parser.
     * @param data The elements available in the current
     *      context of this transformation.
     * @return A custom user node, or <code>null</code>
     *      to discard the rule, which means that all
     *      its arguments will be merged with the arguments
     *      of the nested rule.
     */
    Node transform(
            ValueStack<Value<Node>> stack,
            M value,
            Deque<Value<Node>> data);

    /**
     * Convenient method for building a set
     * of mappers.
     *
     * @return A mapper builder.
     */
    static <M, T, Node> Builder<M, T, Node> $() {
        return new Builder<M, T, Node>();
    }

    /**
     * A mapper builder.
     *
     * @author Philippe Poulard
     *
     * @param <M> A subclass of Mapper.
     * @param <T> The type of the key : String, Rule, or Token.
     * @param <Node> The type of the target node.
     */
    class Builder<M, T, Node> implements Supplier<Map<T, M>> {

        Map<T, M> map = new HashMap<>();

        /**
         * Add a mapping
         *
         * @param ruleOrToken The actual rule or token
         * @param mapper The mapper
         * @return {@code this}, for chaining.
         */
        public Builder<M, T, Node> add(T ruleOrToken, M mapper) {
            map.put(ruleOrToken, mapper);
            return this;
        }

        @Override
        public Map<T, M> get() {
            return map;
        }

    }

}
