package ml.alternet.security.auth.impl;

import java.util.Optional;

import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.HashUtil;
import ml.alternet.security.auth.Hasher;

/**
 * With the Colon Crypt format, the parts are separated by ":".
 *
 * <h4>Examples :</h4>
 * <ul>
 * <li><pre>PBKDF2:1000:uGWNzmy5WSU7dlwF6WQp0oFysI6bbnXD:u+BetVYiks7q3Gu
 * 9SR6B4i+8ccTMTq2/</pre></li>
 * <li><pre>PBKDF2:999:0314E17362D0D966C8F999A66045210DBE7EA897F024E07F:
 * 3AE344F7AB5AA17308A49FDAD997105340DD6E348FDF5623</pre></li>
 * </ul>
 *
 * Alternet Security supply an implementation in a separate module
 * (see <a href="http://alternet.ml/alternet-libs/security/security.html">the documentation</a>).
 *
 * @see ml.alternet.security.auth.impl.PBKDF2Hasher
 *
 * @author Philippe Poulard
 */
public class ColonCryptFormat implements CryptFormat {

    /** The singleton of the colon crypt format. */
    public static final ColonCryptFormat SINGLETON = new ColonCryptFormat();

    @Override
    public Optional<Hasher> resolve(String crypt) {
        String scheme = null;
        try {
            String[] parts = crypt.split(":");
            if (parts.length > 0) {
                scheme = parts[0];
                return HashUtil.lookup(family(), scheme, null);
            }
        } catch (Exception e) {
            LOGGER.fine("No crypt format found for " + scheme + " for " + family());
        }
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

}
