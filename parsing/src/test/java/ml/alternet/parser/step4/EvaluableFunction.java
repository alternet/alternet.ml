package ml.alternet.parser.step4;

/**
 * A single arg function.
 *
 * @author Philippe Poulard
 */
public interface EvaluableFunction {

    /**
     * Evaluate the function
     *
     * @param value The argument of the function
     *
     * @return The computed value.
     */
    Number eval(Number value);

}
