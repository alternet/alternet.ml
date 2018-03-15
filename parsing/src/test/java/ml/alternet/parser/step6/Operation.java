package ml.alternet.parser.step6;

import java.util.function.BiFunction;

/**
 * Operations.
 *
 * @author Philippe Poulard
 */
public interface Operation {

    interface Function extends Operation, java.util.function.Function<Number, Number> { }

    interface Operator { }

    interface Addition extends Operation, Operator, java.util.function.Function<Number, Number> { };

    interface Multiplication extends Operation, Operator, BiFunction<Number, Number, Number> { };

}
