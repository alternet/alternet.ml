package ml.alternet.parser.handlers;

import java.util.Deque;
import java.util.LinkedList;
import java.util.logging.Logger;

import ml.alternet.parser.EventsHandler.TokenValue;
import ml.alternet.parser.EventsHandler.RuleStart;
import ml.alternet.parser.EventsHandler.RuleEnd;
import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.Grammar.Fragment;
import ml.alternet.parser.util.Dual;
import ml.alternet.parser.util.ValueStack;

/**
 * A tree handler builds a target data structure while parsing.
 *
 * @author Philippe Poulard
 *
 * @param <T> The type of the nodes in the target data structure.
 */
public abstract class TreeHandler<T> implements DataHandler<T> {

    private static final Logger LOG = Logger.getLogger(TreeHandler.class.getName());

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
         *      couterpart rule start, <code>false</code> otherwise.
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

    /**
     * The stack of intermediate values hold by this handler.
     */
    protected ValueStack<Value<T>> stack = new ValueStack<>();

    /**
     * Transform a token value to a custom node of
     * the target data structure.
     *
     * @param token The actual token.
     * @param next The next values within the boundaries of the current rule.
     *      For example, if your grammar can parse "+ 123" and that "+" is the
     *      token, then "123" is the first item within the next values.
     * @return A node
     */
    public abstract Value<T> tokenToNode(TokenValue<?> token, Deque<Value<T>> next);

    /**
     * Transform a rule and its arguments to a a custom node
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
     * @return A node, or <code>null</code> to discard the rule
     *      and merge the args with the args of the nested rule.
     */
    public abstract Value<T> ruleToNode(Rule rule, Deque<Value<T>> args);

    @Override
    public T get() {
        Value<T> val = this.stack.get();
        if (val.isSource()) {
            @SuppressWarnings("unchecked")
            TokenValue<T> value = (TokenValue<T>) val.getSource();
            return tokenToNode(value, null).getTarget();
        } else {
            return val.getTarget();
        }
    }

    @Override
    public void receive(TokenValue<?> tokenValue) {
        if (tokenValue.getRule().isFragment()) {
            LOG.finest(() -> "Receiving fragment token : " + tokenValue);
        } else {
            LOG.fine(() -> "Receiving token : " + tokenValue);
            this.stack.push(new Value<T>().setSource(tokenValue));
        }
    }

    @Override
    public void receive(RuleStart ruleStart) {
        LOG.fine(() -> "Receiving rule start : " + ruleStart);
        this.stack.push(new Value<T>().setSource(ruleStart));
    }

    @Override
    public void receive(RuleEnd ruleEnd) {
        LOG.fine(() -> "Receiving " + (ruleEnd.matched ? "" : " unfulfilled ")
                        + "rule end : " + ruleEnd);
        // will contain items from RuleStart to RuleEnd
        Deque<Value<T>> args = new LinkedList<>();
        // will contain ruleStart
        Value<T> valStart;
        // look for rule start
        for (valStart = this.stack.pop() ;
                ! valStart.matches(ruleEnd) ;
                valStart = this.stack.pop())
        {
            // valStart is not ruleStart so far
            if (ruleEnd.matched && valStart.isSource()) {
                RuleEvent<?> e = valStart.getSource();
                // only tokens are remaining
                valStart.setValue(tokenToNode((TokenValue<?>) e, args));
            } // else it may be T
            // everything between rule start and rule end is argument
            args.addFirst(valStart);
        }
        if (ruleEnd.matched) {
            if (ruleEnd.getRule().isFragment()) {
                // simply push each argument individually
                // => merge args with args of the nested rule
                args.forEach(arg -> this.stack.push(arg));
            } else {
                // transform rule start to T
                Value<T> t = ruleToNode(valStart.getSource().getRule(), args);
                if (t == null) {
                    // => merge args with args of the nested rule
                    args.forEach(arg -> this.stack.push(arg));
                } else {
                    valStart.setValue(t);
                    // replace in the stack because it was popped
                    this.stack.push(valStart);
                }
            }
        } // else ignore since the rule wasn't fulfilled
    }

    @Override
    public void mark() {
        this.stack.mark();
    }

    @Override
    public void cancel() throws IllegalStateException {
        this.stack.cancel();
    }

    @Override
    public void consume() throws IllegalStateException {
        this.stack.consume();
    }

}
