package ml.alternet.scan;

import java.io.IOException;
import java.util.Optional;

/**
 * Allow to read a value of a specific type.
 *
 * @author Philippe Poulard
 *
 * @param <T> The type of the return value.
 */
public interface Readable<T> {

    /**
     * Read the next value compliant with those defined by this.
     *
     * @param scanner The input to read.
     *
     * @return <code>empty</code> if not found in the input,
     *      the actual value otherwise.
     *
     * @throws IOException When an I/O error occurs.
     */
    Optional<T> nextValue(Scanner scanner) throws IOException;

}
