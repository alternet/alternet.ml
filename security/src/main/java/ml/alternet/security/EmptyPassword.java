package ml.alternet.security;

/**
 * The empty password (use it as a placeholder for
 * a password when no password is supplied.
 *
 * NOTE : since a password can be wrapped in a delegate
 * class, use the <tt>state()</tt> method to check whether
 * a password is empty or not.
 *
 * @see EmptyPassword#SINGLETON
 *
 * @author Philippe Poulard
 */
public class EmptyPassword implements Password {

    /** The singleton empty password */
    public static final EmptyPassword SINGLETON = new EmptyPassword();

    private static final Clear EMPTY_CLEAR = new Clear() {
        @Override
        public void close() { }
        @Override
        public char[] get() {
            return EMPTY_CHAR;
        }
    };

    private static final char[] EMPTY_CHAR = new char[0];

    private EmptyPassword() { }

    /**
     * The empty password can't be invalidated :
     * this method does nothing.
     */
    @Override
    public void destroy() { }

    /**
     * Return false.
     *
     * @return <code>false</code>
     */
    @Override
    public boolean isDestroyed() {
        return false;
    }

    /**
     * Return <code>Empty</code>
     *
     * @return The empty state
     */
    @Override
    public PasswordState state() {
        return PasswordState.Empty;
    }

    @Override
    public Clear getClearCopy() {
        return EMPTY_CLEAR;
    }

    @Override
    public String toString() {
        return "";
    }

}
