package ml.alternet.misc;

/**
 * An exception that indicates that something remains to do in the development.
 *
 * @author Philippe Poulard
 */
public class TodoException extends WtfException {

    private static final long serialVersionUID = -1256986161964653713L;

    public TodoException() {
    }

    public TodoException(String message) {
        super(message);
    }

    public TodoException(Throwable cause) {
        super(cause.getMessage(), cause);
    }

    public TodoException(String message, Throwable cause) {
        super(message, cause);
    }

}
