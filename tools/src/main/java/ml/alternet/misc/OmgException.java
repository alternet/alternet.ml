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

    public OmgException() {
    }

    public OmgException(String message) {
        super(message);
    }

    public OmgException(Throwable cause) {
        super(cause);
    }

    public OmgException(String message, Throwable cause) {
        super(message, cause);
    }

    public static OmgException throwException(Throwable cause) {
        if (cause instanceof OmgException) {
            return (OmgException) cause;
        } else {
            return new OmgException(cause.getMessage(), cause);
        }
    }

    public static OmgException throwException(String message, Throwable cause) {
        if (cause instanceof OmgException) {
            return (OmgException) cause;
        } else {
            return new OmgException(message, cause);
        }
    }

}
