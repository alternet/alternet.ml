package ml.alternet.security.auth.formats;

import java.util.Optional;

import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.HashUtil;
import ml.alternet.security.auth.Hasher;

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
 *
 * Alternet Security doesn't supply an implementation of such crypt format.
 *
 * @author Philippe Poulard
 */
public class CurlyBracesCryptFormat implements CryptFormat {

    /** The singleton of the curly braces crypt format. */
    public static final CurlyBracesCryptFormat SINGLETON = new CurlyBracesCryptFormat();

    @Override
    public Optional<Hasher> resolve(String crypt) {
        String scheme = null;
        try {
            if (crypt.startsWith("{")) {
                int rcb = crypt.indexOf('}');
                if (rcb > 1) {
                    scheme = crypt.substring(1, rcb);
                    return HashUtil.lookup(family(), scheme, null);
                }
            }
        } catch (Exception e) {
            if (scheme == null) {
                LOGGER.fine("Unable to parse " + family());
            } else {
                LOGGER.fine("No crypt format found for " + scheme + " for " + family());
            }
        }
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

}
