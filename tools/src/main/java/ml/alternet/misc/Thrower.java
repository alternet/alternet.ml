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

    /**
     * Invoke a callable without throwing any checked exception : if the underlying
     * callable throws a checked exception, it is cast to an unchecked exception.
     *
     * @param <T> Any return type
     *
     * @param callable The function to call
     *
     * @return The value.
     */
    public static <T> T safeCall(java.util.concurrent.Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            return doThrow(e);
        }
    }

    /**
     * Invoke a callable without throwing any checked exception : if the underlying
     * callable throws a checked exception, it is cast to an unchecked exception.
     *
     * @param callable The function to call
     */
    public static void safeCall(Callable callable) {
        try {
            callable.call();
        } catch (Exception e) {
            doThrow(e);
        }
    }

    /**
     * Like a callable, but without a return value,
     * or like a java.lang.Runnable but with exception.
     *
     * @author Philippe Poulard
     */
    @FunctionalInterface
    public interface Callable {

        /**
         * Call something, or throws an exception if unable to do so.
         *
         * @throws Exception if unable to call.
         */
        void call() throws Exception;

    }

}
