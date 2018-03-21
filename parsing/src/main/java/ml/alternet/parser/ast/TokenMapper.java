package ml.alternet.parser.ast;

import java.util.Deque;

import ml.alternet.parser.EventsHandler.TokenValue;
import ml.alternet.parser.Grammar.Fragment;
import ml.alternet.parser.Grammar.Token;
import ml.alternet.parser.handlers.ValueMapper.Value;
import ml.alternet.parser.util.ValueStack;

/**
 * Maps a token value to a custom user node.
 *
 * A token marked as {@link Fragment} doesn't
 * need a mapper.
 *
 * @author Philippe Poulard
 *
 * @param <Node> The type of the target node.
 */
public interface TokenMapper<Node> extends Mapper<TokenValue<?>, Node> {

    /**
     * Transform a token to a node in the context
     * of what was previously parsed in the stack,
     * and what follows in the current rule.
     *
     * For example, if a rule defines a comma-separated
     * list of digits, that the input is "1,2,3,4", and
     * that the current token is "2", then the next elements
     * are ",3,4" and the stack is "1,".
     *
     * Some elements may be consumed during the production
     * of the target node.
     *
     * @param stack The stack contains source tokens
     *      encountered so far but not yet processed.
     *      The stack is a work area that may be useful
     *      for the transformation of the token.
     * @param token The actual token value to transform is
     *      supplied by the parser.
     * @param next The next elements in the boundaries
     *      of the current rule.
     * @return A custom user node, or <code>null</code>
     *      to keep the token unchanged (in that case it
     *      is supposed to be consumed elsewhere.
     */
    @Override
    Node transform(
            ValueStack<Value<Node>> stack,
            TokenValue<?> token,
            Deque<Value<Node>> next);

    /**
     * Convenient method for building a set of mappers.
     *
     * @param <Node> The type of the target node.
     *
     * @return A token mapper builder.
     */
    static <Node> Builder<Token, TokenMapper<Node>, Node> $() {
        return Mapper.<Token, TokenMapper<Node>, Node> $();
    }

}
