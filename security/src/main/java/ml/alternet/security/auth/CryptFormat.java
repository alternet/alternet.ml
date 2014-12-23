package ml.alternet.security.auth;

import java.util.Optional;

import ml.alternet.security.auth.impl.PBKDF2Hasher;

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
 * take care of several crypt formats. If a single crypt format
 * is handled by the application, one can just get a hasher with
 * {@link HashUtil#lookup(String, String, java.util.Properties)}
 * (see below how to register your own hasher with a simple
 * {@link ml.alternet.discover.LookupKey}).</p>
 *
 * <p>This interface contains common built-in family crypt formats that
 * allow to extract the hash scheme used for resolving the concrete hasher.
 * It is up to the user to register it to the discovery service.</p>
 *
 * <p>For example, imagine that you have a family of crypt formats that
 * are encoded in the form : "<tt>[SHA]QvQHx34cyGz2cjXj6cauQoAwtIg=</tt>"
 * where the scheme appears in square brackets. Two things have to be done :</p>
 * <ol>
 *  <li>create a CryptFormat instance (say SQUARE_BRACKET_CRYPT_FORMAT)
 *          that checks whether a crypt matches "<tt>[scheme]</tt>"</li>
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
 *    SQUARE_BRACKET_CRYPT_FORMAT,
 *    MODULAR_CRYPT_FORMAT,
 *    COLON_CRYPT_FORMAT);
 *if (h.check(password, crypt)) {
 *    // ...
 *}
 * </pre>
 *
 * @author ppoulard
 */
public interface CryptFormat {

    /*
    Digest (md5)        3 colon delimited field, 32 character hash                  admin:The Realm:11fbe079ed3476f7712030d24042ca35
    SHA-1               {SHA} magic in hash, 33 characters total                    admin:{SHA}QvQHx34cyGz2cjXj6cauQoAwtIg=
    Crypt               (no magic) - 11 character hash                              admin:$cnhJ7swqUWTc
    Apache MD5          $apr1$ magic in hash<br />(Not supported by Foswiki)        admin:$apr1$jgwedrkq$jzeetEHMGal5H0SUFDMEl1
    crypt-MD5           $1$ magic in hash, 34 characters total                      admin:$1$3iuE5z/b$JHyXMzQOIq3cl6WlEMoZC.
     */

    Optional<Hasher> resolve(String crypt);

    /**
     * The name of the family of crypt format.
     *
     * @return The family name.
     */
    String family();

    /**
     * Return a 'template', that indicates how a
     * crypt looks like. A template should at least
     * have a 'scheme' and a 'scheme specific part'.
     *
     * @return
     */
    String infoTemplate();

    /**
     * The scheme of this format appears in curly braces.
     *
     * <h4>Examples :</h4>
     * <ul>
     * <li>Contains the password encoded to base64 (just like {SSHA}) :
     * <pre>{SSHA.b64}986H5cS9JcDYQeJd6wKaITMho4M9CrXM</pre></li>
     * <li>Contains the password encoded to hexa :
     * <pre>{SSHA.HEX}3f5ca6203f8cdaa44d9160575c1ee1d77abcf59ca5f852d1</pre></li>
     * </ul>
     */
    CryptFormat CURLY_BRACES_CRYPT_FORMAT = new CryptFormat() {
        @Override
        public Optional<Hasher> resolve(String crypt) {
            try {
                if (crypt.startsWith("{")) {
                    int rcb = crypt.indexOf('}');
                    if (rcb > 1) {
                        String scheme = crypt.substring(1, rcb);
                        return HashUtil.lookup(family(), scheme, null);
                    }
                }
            } catch (Exception e) { }
            return Optional.empty();
        }

        /**
         * @return "CurlyBracesCryptFormat"
         */
        @Override
        public String family() {
            return "CurlyBracesCryptFormat";
        }

        /**
         * @return "{[scheme]}:[shemeSpecificPart]"
         */
        @Override
        public String infoTemplate() {
            return "{[scheme]}:[shemeSpecificPart]";
        }
    };

    /**
     * The <a href="https://pythonhosted.org/passlib/modular_crypt_format.html">Modular Crytp Format</a>
     * is an ad-hoc standard used in popular linux-based systems.
     *
     * <h4>Examples :</h4>
     * <ul>
     * <li>Apache MD5 crypt format :
     * <pre>$apr1$jgwedrkq$jzeetEHMGal5H0SUFDMEl1</pre></li>
     * <li>Crypt MD5 :
     * <pre>$1$3iuE5z/b$JHyXMzQOIq3cl6WlEMoZC.</pre></li>
     * </ul>
     */
    CryptFormat MODULAR_CRYPT_FORMAT = new CryptFormat() {
        @Override
        public Optional<Hasher> resolve(String crypt) {
            try {
                String[] parts = crypt.split("$");
                if (parts.length > 1) {
                    String scheme = parts[1];
                    return HashUtil.lookup(family(), scheme, null);
                }
            } catch (Exception e) { }
            return Optional.empty();
        }

        /**
         * @return "ModularCryptFormat"
         */
        @Override
        public String family() {
            return "ModularCryptFormat";
        }

        /**
         * @return "$[scheme]$[schemeSpecificPart]"
         */
        @Override
        public String infoTemplate() {
            return "$[scheme]$[schemeSpecificPart]";
        }
    };

    /**
     * With the Colon Crypt format, the parts are separated by ":".
     *
     * <h4>Examples :</h4>
     * <ul>
     * <li><pre>PBKDF2:1000:uGWNzmy5WSU7dlwF6WQp0oFysI6bbnXD:u+BetVYiks7q3Gu9SR6B4i+8ccTMTq2/</pre></li>
     * <li><pre>PBKDF2:999:0314E17362D0D966C8F999A66045210DBE7EA897F024E07F:3AE344F7AB5AA17308A49FDAD997105340DD6E348FDF5623</pre></li>
     * </ul>
     *
     * @see PBKDF2Hasher
     */
    CryptFormat COLON_CRYPT_FORMAT = new CryptFormat() {
        @Override
        public Optional<Hasher> resolve(String crypt) {
            try {
                String[] parts = crypt.split(":");
                if (parts.length > 0) {
                    String scheme = parts[0];
                    return HashUtil.lookup(family(), scheme, null);
                }
            } catch (Exception e) { }
            return Optional.empty();
        }

        /**
         * @return "ColonCryptFormat"
         */
        @Override
        public String family() {
            return "ColonCryptFormat";
        }

        /**
         * @return "[scheme]:[shemeSpecificPart]"
         */
        @Override
        public String infoTemplate() {
            return "[scheme]:[shemeSpecificPart]";
        }

    };

}
