package ml.alternet.parser.handlers;

import java.util.function.Supplier;

import ml.alternet.parser.Handler;

/**
 * A handler that can supply a data after parsing.
 *
 * @author Philippe Poulard
 *
 * @param <T> The type of data.
 */
public interface DataHandler<T> extends Handler, Supplier<T> {

}
