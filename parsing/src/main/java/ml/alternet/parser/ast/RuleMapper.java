package ml.alternet.parser.ast;

import java.util.Deque;

import ml.alternet.parser.Grammar.Fragment;
import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.handlers.ValueMapper.Value;
import ml.alternet.parser.util.ValueStack;

/**
 * Maps a rule to a custom user node.
 *
 * A rule marked as {@link Fragment} doesn't
 * need a mapper.
 *
 * @author Philippe Poulard
 *
 * @param <Node> The type of the target node.
 */
public interface RuleMapper<Node> extends Mapper<Rule, Node> {

    /**
     * Transform a rule to a node in the context
     * of what was previously parsed in the stack,
     * and what follows in the current rule.
     *
     * @param stack The stack contains source tokens
     *      encountered so far but not yet processed.
     *      The stack is a work area that may be useful
     *      for the transformation of the token.
     * @param rule The actual rule to transform is
     *      supplied by the parser.
     * @param args All the elements in the boundaries
     *      of the current rule, that have been previously
     *      transformed.
     * @return A custom user node, or <code>null</code>
     *      to discard the rule, which means that all
     *      its arguments will be merged with the arguments
     *      of the nested rule.
     */
    @Override
    Node transform(
            ValueStack<Value<Node>> stack,
            Rule rule,
            Deque<Value<Node>> args);

    /**
     * Convenient method for building a set of mappers.
     *
     * @param <Node> The type of the target node.
     *
     * @return A rule mapper builder.
     */
    static <Node> Builder<Rule, RuleMapper<Node>, Node> $() {
        return Mapper.<Rule, RuleMapper<Node>, Node> $();
    }

}
