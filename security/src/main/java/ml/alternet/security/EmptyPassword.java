package ml.alternet.security;

/**
 * The empty password (use it as a placeholder for
 * a password when no password is supplied.
 * 
 * @see EmptyPassword#SINGLETON
 * 
 * @author Philippe Poulard
 */
public class EmptyPassword implements Password {

	/** The singleton empty password */
	public final static EmptyPassword SINGLETON = new EmptyPassword();

	private final static Clear EMPTY_CLEAR = new Clear() {
		@Override
		public void close() {}
		@Override
		public char[] get() {
			return EMPTY_CHAR;
		}
	};

	private final static char[] EMPTY_CHAR = new char[0];

	private EmptyPassword() {}

	/**
	 * The empty password can't be invalidated :
	 * this method does nothing.
	 */
	@Override
	public void invalidate() {}

	/**
	 * Return <code>Empty</code>
	 */
	@Override
	public PasswordState state() {
		return PasswordState.Empty;
	}

	@Override
	public Clear getClearCopy() {
		return EMPTY_CLEAR;
	}

}
