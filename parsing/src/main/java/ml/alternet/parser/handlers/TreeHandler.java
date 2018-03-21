package ml.alternet.parser.handlers;

import java.util.Deque;
import java.util.LinkedList;
import java.util.logging.Logger;

import ml.alternet.parser.util.ValueStack;
import ml.alternet.parser.util.ValueStack.Stackable;
import ml.alternet.parser.handlers.ValueMapper.Value;

/**
 * A tree handler builds a target data structure while parsing.
 *
 * @author Philippe Poulard
 *
 * @param <T> The type of the nodes in the target data structure.
 * @param <R> The type of the top node in the target data structure.
 */
public abstract class TreeHandler<T, R extends T> implements DataHandler<R>, ValueMapper<T>, Stackable<Value<T>> {

    private static final Logger LOG = Logger.getLogger(TreeHandler.class.getName());

    // The stack of intermediate values hold by this handler.
    private ValueStack<Value<T>> stack = new ValueStack<>();

    @Override
    public ValueStack<Value<T>> getStack() {
        return this.stack;
    }

    @SuppressWarnings("unchecked")
    @Override
    public R get() {
        // get the final value
        Value<T> val = getStack().get();
        if (val.isSource()) {
            // but it need to be transformed
            TokenValue<T> value = (TokenValue<T>) val.getSource();
            val = tokenToValue(value, null);
            if (val.isSource()) {
                // no mapper : unwrap the token value
                return (R) value.getValue();
            }
        }
        // otherwise we already had the target or did transform the source
        return (R) val.getTarget();
    }

    @Override
    public void receive(TokenValue<?> tokenValue) {
        if (tokenValue.getRule().isFragment()) {
            LOG.finest(() -> "Receiving fragment token : " + tokenValue);
        } else {
            LOG.fine(() -> "Receiving token : " + tokenValue);
            getStack().push(new Value<T>().setSource(tokenValue));
        }
    }

    @Override
    public void receive(RuleStart ruleStart) {
        LOG.fine(() -> "Receiving rule start : " + ruleStart);
        getStack().push(new Value<T>().setSource(ruleStart));
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
        for (valStart = getStack().pop() ;
                ! valStart.matches(ruleEnd) ;
                valStart = getStack().pop())
        {
            // valStart is not ruleStart so far
            if (ruleEnd.matched && valStart.isSource()) {
                RuleEvent<?> e = valStart.getSource();
                // only tokens are remaining
                valStart.setValue(tokenToValue((TokenValue<?>) e, args));
            } // else it may be T
            // everything between rule start and rule end is argument
            args.addFirst(valStart);
        }
        if (ruleEnd.matched) {
            if (ruleEnd.getRule().isFragment()) {
                // simply push each argument individually
                // => merge args with args of the nested rule
                args.forEach(arg -> getStack().push(arg));
            } else {
                // transform rule start to T
                Value<T> t = ruleToValue(valStart.getSource().getRule(), args);
                if (t == null) {
                    // => merge args with args of the nested rule
                    args.forEach(arg -> getStack().push(arg));
                } else {
                    valStart.setValue(t);
                    // replace in the stack because it was popped
                    getStack().push(valStart);
                }
            }
        } // else ignore since the rule wasn't fulfilled
    }

    @Override
    public void mark() {
        getStack().mark();
    }

    @Override
    public void cancel() throws IllegalStateException {
        getStack().cancel();
    }

    @Override
    public void consume() throws IllegalStateException {
        getStack().consume();
    }

}
