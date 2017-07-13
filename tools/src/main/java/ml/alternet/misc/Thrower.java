package ml.alternet.misc;

/**
 * A thrower can "cast" any exception as a RuntimeException
 * without wrapping it.
 *
 * @author Philippe Poulard
 */
public class Thrower {

    /**
     * Throw an exception as a RuntimeException ;
     * the thrown exception is NOT wrapped in a RuntimeException.
     *
     * This allow to throw a checked exception as an unchecked one.
     *
     * @param <T> Any return type
     *
     * @param e Any exception.
     *
     * @return <code>null</code> when the calling code is in a block
     *      that require a value, it can return this call.
     */
    public static <T> T doThrow(Exception e) {
        Thrower.<RuntimeException> throwAs(e);
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Exception> void throwAs(Exception e) throws E {
        throw (E) e;
    }

}
