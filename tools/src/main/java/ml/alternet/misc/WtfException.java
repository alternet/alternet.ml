package ml.alternet.misc;

/**
 * <a href="http://en.wiktionary.org/wiki/WTF">As the name suggest</a>, an
 * unexpected exception.
 * 
 * Use this exception in places where no exception can occur whereas handling an
 * exception is mandatory.
 * 
 * For example :
 * 
 * <pre>
 * try {
 *     MessageDigest md = MessageDigest.getInstance(&quot;MD5&quot;);
 *     return md;
 * } catch (NoSuchAlgorithmException e) {
 *     // can't occur since MD5 have to be supported
 *     WtfException.throwException(e);
 * }
 * </pre>
 * 
 * @author Philippe Poulard
 */
public class WtfException extends RuntimeException {

    private static final long serialVersionUID = -6645630169970707970L;

    WtfException() {
    }

    WtfException(String message) {
        super(message);
    }

    WtfException(String message, Throwable cause) {
        super(message, cause);
    }

    public static WtfException throwException(Throwable cause) {
        if (cause instanceof WtfException) {
            return (WtfException) cause;
        } else {
            return new WtfException(cause.getMessage(), cause);
        }
    }

    public static WtfException throwException(String message, Throwable cause) {
        if (cause instanceof WtfException) {
            return (WtfException) cause;
        } else {
            return new WtfException(message, cause);
        }
    }

}
