package ml.alternet.security;

import javax.security.auth.Destroyable;

/**
 * A safe password, stored obfuscate.
 *
 * In order to make difficult to retrieve a password in the memory, the user
 * should use this class for a long-term storage of its passwords.
 *
 * It is recommended to keep a strong reference to the clear password just at
 * the time where it is necessary, and then to throw that strong reference.
 *
 * A password can be created thanks to {@link PasswordManager}, that exist in
 * several flavors. To pick one, use the {@link PasswordManagerFactory} or
 * supply your own implementation (your own implementation can be set as the
 * default one with the discovery service).
 *
 * <h3>Password creation</h3>
 *
 * <pre>
 * // pick one of the available password manager
 * // (replace XXX with the one you prefer)
 * PasswordManager manager = PasswordManagerFactory.getXXXPasswordManager();
 * Password pwd = manager.newPassword(pwdChars);
 * // from this point,
 * // pwd is safe for staying in memory as long as necessary,
 * // pwdChars has been unset after the creation of the password.
 * </pre>
 *
 * <h3>Typical usage</h3>
 *
 * <pre>
 * try (Password.Clear clear = pwd.getClearCopy()) {
 *     char[] clearPwd = clear.get();
 *     // use clearPwd in the block
 * }
 * // at this point clearPwd has been unset
 * // before being eligible by the garbage collector
 * </pre>
 *
 * @see ml.alternet.security
 * @see Clear
 * @see PasswordManager
 * @see PasswordManagerFactory
 * @see ml.alternet.discover.DiscoveryService
 *
 * @author Philippe Poulard
 */
public interface Password extends Destroyable {

    /**
     * Obfuscate the given password in a new Password instance.
     *
     * This is a convenient shortcut method that lookup for the default
     * password manager set in the platform (that can be override).
     *
     * @param password
     *            The password to obfuscate ; may be null or empty ; that char
     *            array is cleared when returning
     * @return The password
     *
     * @see EmptyPassword#SINGLETON
     * @see PasswordManagerFactory#getDefaultPasswordManager()
     */
    static Password newPassword(char[] password) {
        return PasswordManagerFactory.getDefaultPasswordManager().newPassword(password);
    }

    @Override
    default boolean isDestroyed() {
        return state() == PasswordState.Invalid;
    }

    /**
     * Invalidate this password.
     */
    @Override
    void destroy();

    /**
     * Return the state of this password.
     *
     * @return <code>Empty</code>, <code>Valid</code>, or <code>Invalid</code>
     */
    PasswordState state();

    /**
     * Wrap this password in a clear copy.
     *
     * @return A working copy of the password.
     *
     * @throws IllegalStateException
     *             If this password has been invalidated.
     */
    Clear getClearCopy() throws IllegalStateException;

    /**
     * This class helps keeping low the period where a password appear in clear
     * in the memory, in order to make it difficult to find when a memory dump
     * is performed.
     *
     * Usage :
     *
     * <pre>
     * try (Password.Clear clear = password.getClearCopy()) {
     *     char[] clearPwd = clear.get();
     *     // use clearPwd in the block
     * }
     * // at this point clearPwd has been cleaned
     * // before being eligible by the garbage collector
     * </pre>
     *
     * The user has to ensure to keep the try-with-resource block as short as
     * possible, and to not make copies of the char array if possible.
     *
     * Once closed, a Password.Clear can't be reused.
     *
     * @author Philippe Poulard
     */
    interface Clear extends AutoCloseable {

        /**
         * Get a clear copy of the password ; the password remains obfuscated
         * until this method is invoked.
         *
         * @return The clear password.
         *
         * @throws IllegalStateException
         *             If this password has been invalidated.
         */
        char[] get();

        /**
         * Unset the clear password after usage.
         */
        @Override
        void close();

    }

}
