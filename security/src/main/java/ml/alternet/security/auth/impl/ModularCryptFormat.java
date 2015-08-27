package ml.alternet.security.auth.impl;

import java.util.Optional;

import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.HashUtil;
import ml.alternet.security.auth.Hasher;

/**
 * The <a href="https://pythonhosted.org/passlib/modular_crypt_format.html">Modular Crypt Format</a>
 * is an ad-hoc standard used in popular linux-based systems.
 *
 * <h4>Examples :</h4>
 * <ul>
 * <li>Apache MD5 crypt format :
 * <pre>$apr1$jgwedrkq$jzeetEHMGal5H0SUFDMEl1</pre></li>
 * <li>Crypt MD5 :
 * <pre>$1$3iuE5z/b$JHyXMzQOIq3cl6WlEMoZC.</pre></li>
 * </ul>
 *
 * Alternet Security doesn't supply an implementation of such crypt format.
 *
 * @author Philippe Poulard
 */
public class ModularCryptFormat implements CryptFormat {

    /** The singleton of the modular crypt format. */
    public static final ModularCryptFormat SINGLETON = new ModularCryptFormat();

    @Override
    public Optional<Hasher> resolve(String crypt) {
        String scheme = null;
        try {
            String[] parts = crypt.split("$");
            if (parts.length > 1) {
                scheme = parts[1];
                return HashUtil.lookup(family(), scheme, null);
            }
        } catch (Exception e) {
            LOGGER.fine("No crypt format found for " + scheme + " for " + family());
        }
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

}
