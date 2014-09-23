package ml.alternet.scan;

import java.io.IOException;

/**
 * A constraint used by scanners to read specific objects
 * under conditions.
 *
 * @author Philippe Poulard
 *
 * @see UserData
 */
public interface DataConstraint<T> extends Constraint {

    /**
     * Evaluate the stop condition from the given parameters.
     *
     * <p>This condition is evaluated by the scanner to check
     * if it has to append characters to the current buffer.</p>
     *
     * @param sourceIndex The number of characters read so far ;
     * 		might be useful in certain stop conditions.
     * @param userData Any useful data for building the target
     * 		object. It is initialized to be empty by the
     * 		scanner, and it can be updated by this method as
     * 		one goes along. When the stop condition is realized,
     * 		this method have to set the definitive target object
     * 		that will be returned by the scanner
     * 		(that can be <code>null</code>). Before the stop
     * 		condition is realized, any intermediate value can be
     * 		stored within.
     * @param scanner The scanner that reads the input.
     * 		According to the parsing strategy, if the sequence
     * 		of characters involved in the stop condition (if any)
     * 		don't have to be consumed, the relevant methods of
     * 		the scanner should be involved.
     *
     * @return <code>true</code> to indicate that the current scan
     * 		must stop, <code>false</code> if more characters have
     * 		to be read.
     *
     * @throws IOException When the scanner cause an error.
     */
    boolean stopCondition(int sourceIndex, UserData<T> userData, Scanner scanner) throws IOException;

}
