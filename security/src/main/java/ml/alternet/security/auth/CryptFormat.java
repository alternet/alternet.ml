package ml.alternet.security.auth;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Giving a crypt, allow to detect the right hasher suitable for checking
 * a password validity.
 *
 * <p>An instance doesn't work with every hash scheme and format available
 * (some looks identical, and even may vary according to the applications),
 * but only on those available by a specific application which have to define
 * which ones are eligible.</p>
 *
 * <p>The CryptFormat is useful only for applications that have to
 * take care of several crypt formats, typically in a legacy application
 * where crypt passwords already exist.
 * If a single crypt format is handled by the application, one can just
 * get a hasher with {@link HashUtil#lookup(String, String, java.util.Properties)}
 * (see below how to register your own hasher with a simple
 * {@link ml.alternet.discover.LookupKey}).</p>
 *
 * <p>For example, imagine that you have a family of crypt formats that
 * are encoded in the form : "<tt>[SHA]QvQHx34cyGz2cjXj6cauQoAwtIg=</tt>"
 * where the scheme appears in square brackets. Two things have to be done :</p>
 * <ol>
 *  <li>create a CryptFormat instance that checks whether a crypt matches "<tt>[scheme]</tt>"</li>
 *  <li>create a Hasher that breakdown the crypt in different part and autoconfigure
 *  it if necessary.</li>
 * </ol>
 * <br/>
 * <pre> // "SquareBrackets" stands for the crypt format family name
 *{@literal @}ml.alternet.discover.LookupKey(forClass = Hasher.class, variant = "SquareBrackets/SHA")
 *public class SHAHasherWithSquareBrackets implements Hasher {
 *     // ...
 * }
 * </pre>
 *
 * <p>Now, to check a crypt that may exist in several format, use :</p>
 *
 * <pre>// find the right hasher from the format of a crypt
 *Hasher h = HashUtil.lookup(crypt, prop,
 *    new SHAHasherWithSquareBrackets(),
 *    ModularCryptFormat.SINGLETON,
 *    ColonCryptFormat.SINGLETON);
 *if (h.check(password, crypt)) {
 *    // ...
 *}
 * </pre>
 *
 * @author Philippe Poulard
 */
public interface CryptFormat {

    /*
    Digest (md5) 3 colon delimited field, 32 character hash           admin:The Realm:11fbe079ed3476f7712030d24042ca35
    SHA-1        {SHA} magic in hash, 33 characters total             admin:{SHA}QvQHx34cyGz2cjXj6cauQoAwtIg=
    Crypt        (no magic) - 11 character hash                       admin:$cnhJ7swqUWTc
    Apache MD5   $apr1$ magic in hash<br />(Not supported by Foswiki) admin:$apr1$jgwedrkq$jzeetEHMGal5H0SUFDMEl1
    crypt-MD5    $1$ magic in hash, 34 characters total               admin:$1$3iuE5z/b$JHyXMzQOIq3cl6WlEMoZC.
     */

    /**
     * A logger for this class.
     */
    Logger LOGGER = Logger.getLogger(CryptFormat.class.getName());

    /**
     * Get the hasher bound to a crypt.
     *
     * @param crypt The candidate crypt.
     * @return The hasher if the given crypt
     *      matches this crypt format.
     */
    Optional<Hasher> resolve(String crypt);

    /**
     * The name of the family of crypt format.
     *
     * @return The family name, use as the first part of the variant
     * of the hasher to lookup.
     *
     * @see Hasher#DEFAULT_VARIANT
     */
    String family();

    /**
     * Return a 'template', that indicates how a
     * crypt looks like. A template should at least
     * have a 'scheme' and a 'scheme specific part'.
     *
     * @return The shape of the crypt format.
     */
    String infoTemplate();

}
