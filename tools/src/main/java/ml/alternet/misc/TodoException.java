package ml.alternet.misc;

/**
 * An exception that indicates that something remains to do in the development.
 *
 * @author Philippe Poulard
 */
public class TodoException extends WtfException {

    private static final long serialVersionUID = -1256986161964653713L;

    /**
     * Create the exception.
     */
    public TodoException() { }

    /**
     * Create the exception.
     *
     * @param message The message.
     */
    public TodoException(String message) {
        super(message);
    }

    /**
     * Create the exception.
     *
     * @param cause The cause.
     */
    public TodoException(Throwable cause) {
        super(cause.getMessage(), cause);
    }

    /**
     * Create the exception.
     *
     * @param message The message.
     * @param cause The cause.
     */
    public TodoException(String message, Throwable cause) {
        super(message, cause);
    }

}
