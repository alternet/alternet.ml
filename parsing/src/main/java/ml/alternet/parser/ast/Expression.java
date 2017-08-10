package ml.alternet.parser.ast;

import ml.alternet.parser.handlers.DataHandler;

/**
 * A helper class for evaluating expressions
 * given by a parser.
 *
 * @see DataHandler#get()
 *
 * @author Philippe Poulard
 *
 * @param <T> The type of the computed value.
 * @param <C> The context used on evaluation.
 */
public interface Expression<T, C> {

    /**
     * An expression is evaluable to a value.
     *
     * @param context The context used when evaluating the expression ;
     *      may contains a set of variables available, a set of functions, etc.
     *
     * @return The computed value.
     */
    T eval(C context);

}
