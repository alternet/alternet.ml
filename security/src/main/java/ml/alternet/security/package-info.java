/**
 * This package aims to enhance security on passwords
 * handled in the JVM.
 *
 * The idea is to keep low the period where a password
 * appeared in clear in the memory, in order to make it
 * difficult to find when a memory dump is performed.
 *
 * A password can be created thanks to {@link PasswordManager}, that
 * exist in several flavors. To pick one, use the {@link PasswordManagerFactory}
 * or supply your own implementation (your own implementation can
 * override the default one with the discovery service).
 *
 * <h3>Password creation</h3>
 * <pre> // pick one of the available password manager
 * // (replace XXX with the one you prefer)
 * PasswordManager manager = PasswordManagerFactory.getXXXPasswordManager();
 * Password pwd = manager.newPassword(pwdChars);
 * // from this point,
 * // pwd is safe for staying in memory as long as necessary,
 * // pwdChars has been unset after the creation of the password.
 * </pre>
 *
 * <h3>Typical usage</h3>
 * <pre>try (Password.Clear clear = pwd.getClearCopy()) {
 *     char[] clearPwd = clear.get();
 *     // use clearPwd in the block
 * }
 * // at this point clearPwd has been unset
 * // before being eligible by the garbage collector
 * </pre>
 *
 * The user has to ensure to keep the try-with-resource
 * block as short as possible, and to not make copies of
 * the char array if possible.
 *
 * <h3>About passwords</h3>
 * The <a href="http://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html#PBEEx">Crypto
 * Specification</a>
 * states that since as String is immutable (and subject to be stored in a
 * pool by the JVM), char arrays are preferred for storing password.
 *
 * Usually, a password may be used for accessing
 * a resource ; before and after such access, the intermediate
 * data used as the password should not remain in memory :
 * the idea of the Password class is to allow a long-term
 * store of the password between two resource accesses.
 * Since the Password class obfuscate the data, if the memory is dumped
 * it will be difficult to guess which part of the memory was
 * a password. The idea is to limit usage of the clear password
 * outside of the Password class.
 *
 * In many cases, the Password will be built from data
 * that are coming from a string (such as an URI), it is
 * recommended to not keep a strong reference to such data
 */
@ml.alternet.misc.InfoClass
package ml.alternet.security;
