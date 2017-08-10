package ml.alternet.parser.step3;

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
    public abstract double eval(double value);

}
