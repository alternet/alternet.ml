package ml.alternet.misc;

/**
 * <a href="http://en.wiktionary.org/wiki/OMG">As the name suggest</a>, an
 * unforeseen exception that shouldn't be thrown, and therefore that some
 * processing is missing; not as critical as an unexpected exception.
 *
 * @see WtfException
 *
 * @author Philippe Poulard
 */
public class OmgException extends RuntimeException {

    private static final long serialVersionUID = 3138850059487850348L;

    /**
     * Create the exception.
     */
    public OmgException() { }

    /**
     * Create the exception.
     *
     * @param message The message.
     */
    public OmgException(String message) {
        super(message);
    }

    /**
     * Create the exception.
     *
     * @param cause The cause.
     */
    public OmgException(Throwable cause) {
        super(cause);
    }

    /**
     * Create the exception.
     *
     * @param message The message.
     * @param cause The cause.
     */
    public OmgException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Propagate or create the exception.
     *
     * @param cause The cause.
     *
     * @return The exception.
     */
    public static OmgException throwException(Throwable cause) {
        if (cause instanceof OmgException) {
            return (OmgException) cause;
        } else {
            return new OmgException(cause.getMessage(), cause);
        }
    }

    /**
     * Propagate or create the exception.
     *
     * @param message The message.
     * @param cause The cause.
     *
     * @return The exception.
     */
    public static OmgException throwException(String message, Throwable cause) {
        if (cause instanceof OmgException) {
            return (OmgException) cause;
        } else {
            return new OmgException(message, cause);
        }
    }

}
