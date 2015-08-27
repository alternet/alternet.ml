package ml.alternet.security.auth;

import java.security.InvalidAlgorithmParameterException;
import java.util.Properties;

import ml.alternet.discover.DiscoveryService;
import ml.alternet.misc.Thrower;
import ml.alternet.security.Password;

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
 * @see Credentials
 *
 * @author Philippe Poulard
 */
public interface Hasher {

    /**
     * The default variant of this class to lookup is "/ColonCryptFormat/PBKDF2".
     *
     * @see ml.alternet.discover.DiscoveryService#lookupSingleton(String)
     * @see #getDefault()
     */
    public static String DEFAULT_VARIANT = "/ColonCryptFormat/PBKDF2";

    /**
     * Return the default hasher. The lookup key is :
     * "<code>ml.alternet.security.auth.Hasher/ColonCryptFormat/PBKDF2</code>".
     *
     * <p>Note that Alternet Security supply an implementation of this hasher
     * available in the separate module : <code>ml.alternet:alternet-security-auth-impl</code></p>
     *
     * @return The PBKDF2 hasher implementation.
     *
     * @see #DEFAULT_VARIANT
     */
    static Hasher getDefault() {
        try {
            return DiscoveryService.lookupSingleton(Hasher.class.getName() + DEFAULT_VARIANT);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            return Thrower.doThrow(e);
        }
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
     * Compute a crypt from credentials.
     *
     * <p>By default, the user is ignored.</p>
     *
     * @param credentials Credentials, that must contain at least the password.
     *
     * @return A crypt.
     *
     * @throws InvalidAlgorithmParameterException When the hash algorithm fails.
     */
    String encrypt(Credentials credentials) throws InvalidAlgorithmParameterException;

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
        return encrypt(Credentials.fromUserPassword(user, password));
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
    default String encrypt(char[] password) throws InvalidAlgorithmParameterException {
        return encrypt(Credentials.fromPassword(password));
    }

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
        return encrypt(Credentials.fromUserPassword(user, password));
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
    default String encrypt(Password password) throws InvalidAlgorithmParameterException {
        return encrypt(Credentials.fromPassword(password));
    }

    /**
     * Check whether a password matches a crypt.
     *
     * @param credentials Credentials, that must contain at least the password.
     *
     * @param crypt The crypt that has been previously computed by
     *          the same hasher.
     *
     * @return <code>true</code> if the password matches the crypt,
     *          <code>false</code> otherwise.
     *
     * @throws InvalidAlgorithmParameterException When the hash algorithm fails.
     */
    boolean check(Credentials credentials, String crypt) throws InvalidAlgorithmParameterException;

    /**
     * Compare bytes array in length constant time in order to prevent "timing attack".
     *
     * @param a A non-null byte array.
     * @param b A non-null byte array.
     *
     * @return <code>true</true> if all the bytes are equals, <code>false</true> otherwise.
     */
    static boolean compare(byte[] a, byte[] b) {
        // do not use Arrays.equals(hash, pwdHash);
        int res = a.length ^ b.length;
        for (int i = 0; i < a.length && i < b.length; i++) {
            // do not use == , may be compiled / interpreted as a branch
            res |= a[i] ^ b[i];
        }
        return res == 0;
    }

}
