package ml.alternet.security.auth;

import java.security.InvalidAlgorithmParameterException;
import java.util.Properties;

import ml.alternet.security.Password;
import ml.alternet.security.auth.impl.PBKDF2Hasher;

/**
 * Computes/checks a password to/with a crypt.
 *
 * <p>Some hasher require a user name, but the more often they won't.</p>
 *
 * <p>A hasher is usually available to the discovery service with
 * a variant make from the crypt format family name and the
 * hasher scheme.</p>
 *
 * <p>{@link HashUtil} supply several means to find a hasher or
 * to check a password.</p>
 *
 * @see ml.alternet.discover.DiscoveryService
 * @see CryptFormat#family()
 * @see HashUtil
 *
 * @author Philippe Poulard
 */
public interface Hasher {

    /**
     * Return the default hasher.
     *
     * @return The PBKDF2 hasher.
     *
     * @see PBKDF2Hasher
     */
    static Hasher getDefault() {
        return new PBKDF2Hasher();
    }

    /**
     * Get the scheme of this hasher.
     *
     * @return The scheme.
     */
    String getScheme();

    /**
     * Some hasher may use properties for computing a crypt ;
     * a hasher usually doesn't need properties to check a password.
     *
     * @param properties The properties used to configure this hasher.
     *
     * @throws InvalidAlgorithmParameterException When a property is
     *      not supported by this hasher.
     */
    void configure(Properties properties) throws InvalidAlgorithmParameterException;

    /**
     * Return the current settings of this hasher.
     *
     * @return The current properties, including the default values.
     */
    Properties getConfiguration();

    /**
     * Compute a crypt from a password.
     *
     * <p>By default, the user is ignored.</p>
     *
     * @param user The user name (sometimes required by some hasher), or <tt>null</tt>
     * @param password The password.
     *
     * @return A crypt.
     *
     * @throws InvalidAlgorithmParameterException When the hash algorithm fails.
     */
    default String encrypt(String user, char[] password) throws InvalidAlgorithmParameterException {
        return encrypt(password);
    }

    /**
     * Compute a crypt from a password.
     *
     * @param password The password.
     *
     * @return A crypt.
     *
     * @throws InvalidAlgorithmParameterException When the hash algorithm fails.
     */
    String encrypt(char[] password) throws InvalidAlgorithmParameterException;

    /**
     * Compute a crypt from a password.
     *
     * <p>By default, the user is ignored.</p>
     *
     * @param user The user name (sometimes required by some hasher), or <tt>null</tt>
     *
     * @param password The password.
     *
     * @return A crypt.
     *
     * @throws InvalidAlgorithmParameterException When the hash algorithm fails.
     */
    default String encrypt(String user, Password password) throws InvalidAlgorithmParameterException {
        return encrypt(password);
    }

    /**
     * Compute a crypt from a password.
     *
     * @param password The password.
     *
     * @return A crypt.
     *
     * @throws InvalidAlgorithmParameterException When the hash algorithm fails.
     */
    String encrypt(Password password) throws InvalidAlgorithmParameterException;

    /**
     * Check whether a password matches a crypt.
     *
     * @param user The user name (sometimes required by some hasher), or <tt>null</tt>
     * @param password The password to check.
     * @param crypt The crypt that has been previously computed by
     *          the same hasher.
     * @return <code>true</code> if the password matches the crypt,
     *          <code>false</code> otherwise.
     *
     * @throws InvalidAlgorithmParameterException When the hash algorithm fails.
     */
    boolean check(String user, char[] password, String crypt) throws InvalidAlgorithmParameterException;

    /**
     * Check whether a password matches a crypt.
     *
     * @param user The user name (sometimes required by some hasher), or <tt>null</tt>
     * @param password The password to check.
     * @param crypt The crypt that has been previously computed by
     *          the same hasher.
     * @return <code>true</code> if the password matches the crypt,
     *          <code>false</code> otherwise.
     *
     * @throws InvalidAlgorithmParameterException When the hash algorithm fails.
     */
    boolean check(String user, Password password, String crypt) throws InvalidAlgorithmParameterException;

}
